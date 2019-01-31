package io.horizontalsystems.ethereumkit

import android.content.Context
import android.content.pm.PackageManager
import io.horizontalsystems.ethereumkit.core.*
import io.horizontalsystems.ethereumkit.models.Balance
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.LastBlockHeight
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.ethereumkit.network.EtherscanService
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function4
import io.reactivex.schedulers.Schedulers
import io.realm.OrderedCollectionChangeSet
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import io.realm.annotations.RealmModule
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

@RealmModule(library = true, allClasses = true)
class EthereumKitModule

class EthereumKit(words: List<String>, networkType: NetworkType, walletId: String) {

    interface Listener {
        fun onTransactionsUpdate(inserted: List<Transaction>, updated: List<Transaction>, deleted: List<Int>)
        fun onBalanceUpdate(balance: Double)
        fun onLastBlockHeightUpdate(height: Int)
        fun onKitStateUpdate(state: KitState)
    }

    interface ListenerERC20 : Listener {
        val contractAddress: String
        val decimal: Int
    }

    val receiveAddress: String

    var balance: Double = 0.0
        private set

    var lastBlockHeight: Int? = null
        private set

    var listener: Listener? = null
    var kitState: KitState = KitState.NotSynced
        private set(value) {
            listener?.onKitStateUpdate(value)
            field = value
        }

    private val erc20List = hashMapOf<String, ERC20>()

    private val realmFactory = RealmFactory("ethereumkit-${networkType.name}-$walletId")
    private val transactionRealmResults: RealmResults<Transaction>
    private val balanceRealmResults: RealmResults<Balance>

    private val hdWallet = HDWallet(Mnemonic().toSeed(words), if (networkType == NetworkType.MainNet) 60 else 1)
    private val web3j = Web3jInfura(networkType, infuraApiKey)
    private val etherscanService = EtherscanService(networkType, etherscanApiKey)
    private val addressValidator = AddressValidator()
    private val disposables = CompositeDisposable()

    private val timer: Timer

    init {
        val realm = realmFactory.realm
        receiveAddress = hdWallet.address()

        transactionRealmResults = realm.where(Transaction::class.java).findAll()
        transactionRealmResults.addChangeListener { t, changeSet ->
            handleTransactionsUpdate(t, changeSet)
        }

        getBalance(receiveAddress)?.let {
            balance = it.balance
        }

        balanceRealmResults = realm.where(Balance::class.java).findAll()
        balanceRealmResults.addChangeListener { b, changeSet ->
            handleUpdateBalance(b, changeSet)
        }

        timer = Timer(30, object : Timer.Listener {
            override fun onTimeIsUp() {
                refresh()
                erc20List.forEach { refresh(it.key) }
            }
        })
    }

    //
    // API methods
    //

    fun start() {
        timer.start()

        refresh()
    }

    fun clear() {
        disposables.clear()
        timer.stop()
        web3j.shutdown()
        erc20List.forEach { unregister(it.key) }
        realmFactory.realm.use { realm ->
            realm.executeTransaction {
                it.deleteAll()
            }
        }
    }

    @Synchronized
    fun refresh() {
        kitState = KitState.Syncing(0.0)

        Flowable.zip(updateBalance(), updateLastBlockHeight(), updateTransactions(), updateGasPrice(), Function4<Double, Int, Int, Double, Unit> { _, _, _, _ -> Unit })
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .subscribe({
                    kitState = KitState.Synced
                }, {
                    it?.printStackTrace()
                    kitState = KitState.NotSynced
                }).let {
                    disposables.add(it)
                }
    }

    fun validateAddress(address: String) {
        addressValidator.validate(address)
    }

    fun transactions(fromHash: String? = null, limit: Int? = null): Single<List<Transaction>> {
        return getTransactions(fromHash, limit)
    }

    fun fee(): Double {
        realmFactory.realm.use { realm ->
            val gwei = realm.where(GasPrice::class.java).findFirst()?.gasPriceInGwei
            val wei = Convert.toWei(BigDecimal.valueOf(gwei
                    ?: DEFAULT_GAS_PRICE), Convert.Unit.GWEI)

            return Convert.fromWei(wei.multiply(BigDecimal(GAS_LIMIT)), Convert.Unit.ETHER).toDouble()
        }
    }

    fun send(toAddress: String, amount: Double, completion: ((Throwable?) -> Unit)? = null) {
        val value = Convert.toWei(BigDecimal.valueOf(amount), Convert.Unit.ETHER).toBigInteger()

        broadcastTransaction(toAddress, value)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    completion?.invoke(null)
                }, {
                    completion?.invoke(it)
                }).let {
                    disposables.add(it)
                }
    }

    //
    // ERC20
    //

    fun register(token: ListenerERC20) {
        if (erc20List[token.contractAddress] != null) {
            return
        }


        val holder = ERC20(token)
        erc20List[token.contractAddress] = holder
        getBalance(token.contractAddress)?.let {
            holder.balance = it.balance
        }

        refresh(token.contractAddress)
    }

    fun unregister(contractAddress: String) {
        erc20List.remove(contractAddress)
    }

    @Synchronized
    fun refresh(contractAddress: String) {
        val erc20 = erc20List[contractAddress] ?: return

        erc20.kitState = KitState.Syncing(0.0)

        Flowable.zip(updateBalance(contractAddress, erc20.listener.decimal), updateTransactions(contractAddress), BiFunction<Double, Int, Unit> { _, _ -> Unit })
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .subscribe({
                    erc20.kitState = KitState.Synced
                }, {
                    it?.printStackTrace()
                    erc20.kitState = KitState.NotSynced
                }).let {
                    disposables.add(it)
                }
    }

    fun feeERC20(): Double {
        realmFactory.realm.use { realm ->
            val gwei = realm.where(GasPrice::class.java).findFirst()?.gasPriceInGwei
            val number = BigDecimal.valueOf(gwei ?: DEFAULT_GAS_PRICE)
            val wei = Convert.toWei(number, Convert.Unit.GWEI)

            return Convert.fromWei(wei.multiply(BigDecimal(GAS_LIMIT_ERC20)), Convert.Unit.ETHER).toDouble()
        }
    }

    fun balanceERC20(contractAddress: String): Double {
        return erc20List[contractAddress]?.balance ?: 0.0
    }

    fun transactionsERC20(contractAddress: String, fromHash: String? = null, limit: Int? = null): Single<List<Transaction>> {
        return getTransactions(fromHash, limit, contractAddress)
    }

    fun sendERC20(toAddress: String, contractAddress: String, amount: Double, completion: ((Throwable?) -> Unit)? = null) {
        val token = erc20List[contractAddress]?.listener
                ?: throw EthereumKitException.TokenNotFound(contractAddress)

        val value = BigDecimal.valueOf(amount).multiply(BigDecimal.TEN.pow(token.decimal)).toBigInteger()

        broadcastTransaction(toAddress, value, contractAddress)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    completion?.invoke(null)
                }, {
                    completion?.invoke(it)
                }).let {
                    disposables.add(it)
                }
    }

    //
    // Private
    //

    private fun handleTransactionsUpdate(collection: RealmResults<Transaction>, changeSet: OrderedCollectionChangeSet) {
        if (changeSet.state == OrderedCollectionChangeSet.State.UPDATE) {
            val validTx: (Transaction) -> Boolean = { it.contractAddress.isNotEmpty() || it.input == "0x" }

            val inserts = changeSet.insertions.asList().mapNotNull { collection[it] }.filter(validTx)
            val updates = changeSet.changes.asList().mapNotNull { collection[it] }.filter(validTx)
            val deletes = changeSet.deletions.asList()

            val ethInserts = inserts.filter { it.contractAddress.isEmpty() }
            val ethUpdates = updates.filter { it.contractAddress.isEmpty() }
            if (ethInserts.isNotEmpty() || ethUpdates.isNotEmpty()) {
                listener?.onTransactionsUpdate(ethInserts, ethUpdates, deletes)
            }

            erc20List.forEach {
                val tokenInserts = inserts.filter { item -> item.contractAddress == it.key }
                val tokenUpdates = updates.filter { item -> item.contractAddress == it.key }
                if (tokenInserts.isNotEmpty() || tokenUpdates.isNotEmpty()) {
                    it.value.listener.onTransactionsUpdate(tokenInserts, tokenUpdates, deletes)
                }
            }
        }
    }

    private fun handleUpdateBalance(collection: RealmResults<Balance>, changeSet: OrderedCollectionChangeSet) {
        if (changeSet.state == OrderedCollectionChangeSet.State.UPDATE) {
            val inserts = changeSet.insertions.asList().mapNotNull { collection[it] }
            val updates = changeSet.changes.asList().mapNotNull { collection[it] }

            (inserts + updates).forEach {
                if (it.address == receiveAddress) {
                    balance = it.balance
                    listener?.onBalanceUpdate(it.balance)
                } else {
                    erc20List[it.address]?.balance = it.balance
                    erc20List[it.address]?.listener?.onBalanceUpdate(it.balance)
                }
            }
        }
    }

    private fun getTransactions(fromHash: String? = null, limit: Int? = null, contractAddress: String = ""): Single<List<Transaction>> {
        return Single.create { subscriber ->
            realmFactory.realm.use { realm ->
                var results = realm.where(Transaction::class.java).equalTo("contractAddress", contractAddress).sort("timeStamp", Sort.DESCENDING)
                if (contractAddress.isEmpty()) {
                    results.equalTo("input", "0x")
                }

                fromHash?.let { fromHash ->
                    results.equalTo("hash", fromHash)
                            .findFirst()?.let { fromTransaction ->
                                results = results.lessThan("timeStamp", fromTransaction.timeStamp)
                            }
                }

                limit?.let {
                    results = results.limit(it.toLong())
                }

                subscriber.onSuccess(results.findAll().mapNotNull { realm.copyFromRealm(it) })
            }
        }
    }

    private fun broadcastTransaction(toAddress: String, amount: BigInteger, contractAddress: String? = null): Flowable<Unit> {
        return Flowable.fromCallable {
            //  get the next available nonce
            val ethGetTransactionCount = web3j.getTransactionCount(receiveAddress)
            val nonce = ethGetTransactionCount.transactionCount

            val gasPrice = getGasPrice()

            val rawTransaction = if (contractAddress != null) {
                val transferFN = Function("transfer",
                        Arrays.asList<Type<*>>(Address(toAddress), Uint256(amount)),
                        Arrays.asList<TypeReference<*>>(object : TypeReference<Uint256>() {}))

                val encodeData = FunctionEncoder.encode(transferFN)
                RawTransaction.createTransaction(nonce, gasPrice, GAS_LIMIT_ERC20.toBigInteger(), contractAddress, encodeData)
            } else {
                RawTransaction.createEtherTransaction(nonce, gasPrice, GAS_LIMIT.toBigInteger(), toAddress, amount)
            }

            //  sign & send our transaction
            val signedMessage = TransactionEncoder.signMessage(rawTransaction, hdWallet.credentials())
            val hexValue = Numeric.toHexString(signedMessage)

            web3j.sendRawTransaction(hexValue)
        }.map { ethSendTransaction ->
            val pendingTx = Transaction().apply {
                hash = ethSendTransaction.transactionHash
                timeStamp = System.currentTimeMillis() / 1000
                from = receiveAddress
                to = toAddress
                value = amount.toString()

                contractAddress?.let {
                    this.contractAddress = it
                }
            }

            realmFactory.realm.use { realm ->
                realm.executeTransaction {
                    it.insertOrUpdate(pendingTx)
                }
            }
        }
    }

    private fun getGasPrice() = realmFactory.realm.use {
        val gPrice = it.where(GasPrice::class.java).findFirst()?.gasPriceInGwei
        val number = BigDecimal.valueOf(gPrice ?: DEFAULT_GAS_PRICE)

        Convert.toWei(number, Convert.Unit.GWEI).toBigInteger()
    }

    private fun getBalance(address: String): Balance? {
        realmFactory.realm.use {
            return it.where(Balance::class.java).equalTo("address", address).findFirst()
        }
    }

    private fun updateGasPrice(): Flowable<Double> {
        return web3j.getGasPrice()
                .map { gasPrice ->
                    realmFactory.realm.use { realm ->
                        realm.executeTransaction {
                            it.insertOrUpdate(GasPrice(gasPrice))
                        }
                    }
                    gasPrice
                }
                .onErrorReturn {
                    DEFAULT_GAS_PRICE
                }
    }

    private fun updateBalance(contractAddress: String? = null, decimal: Int = ETH_DECIMAL): Flowable<Double> {
        val flowable = if (contractAddress == null) {
            web3j.getBalance(receiveAddress)
        } else {
            web3j.getTokenBalance(receiveAddress, contractAddress, decimal)
        }

        return flowable.map { balance ->
            realmFactory.realm.use { realm ->
                realm.executeTransaction {
                    it.insertOrUpdate(Balance(contractAddress ?: receiveAddress, balance, decimal))
                }
            }

            balance
        }
    }

    private fun updateLastBlockHeight(): Flowable<Int> {
        return web3j.getBlockNumber()
                .map { height ->
                    lastBlockHeight = height
                    realmFactory.realm.use { realm ->
                        realm.executeTransaction {
                            it.insertOrUpdate(LastBlockHeight(height))
                        }
                    }

                    listener?.onLastBlockHeightUpdate(height)

                    erc20List.forEach {
                        it.value.listener.onLastBlockHeightUpdate(height)
                    }

                    height
                }
    }

    private fun updateTransactions(contractAddress: String? = null): Flowable<Int> {

        val lastBlockHeight = realmFactory.realm.use {
            it.where(Transaction::class.java).equalTo("contractAddress", contractAddress ?: "")
                    .sort("blockNumber", Sort.DESCENDING)
                    .findFirst()?.blockNumber?.toInt() ?: 0
        }

        val flowable = if (contractAddress == null) {
            etherscanService.getTransactionList(receiveAddress, lastBlockHeight + 1)
        } else {
            etherscanService.getTokenTransactions(contractAddress, receiveAddress, lastBlockHeight + 1)
        }

        return flowable.map { response ->
            realmFactory.realm.use { realm ->
                realm.executeTransaction {
                    response.result
                            .map { tx -> Transaction(tx) }
                            .forEach { tx ->
                                realm.insertOrUpdate(tx)
                            }
                }
            }

            response.result.size
        }
    }

    companion object {
        private const val DEFAULT_GAS_PRICE = 10.0 //in GWei
        private const val ETH_DECIMAL = 18
        private const val GAS_LIMIT = 21_000
        private const val GAS_LIMIT_ERC20 = 100_000

        private var infuraApiKey = ""
        private var etherscanApiKey = ""

        fun init(context: Context) {
            Realm.init(context)

            try {
                val applicationInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                val bundle = applicationInfo.metaData

                val infuraKey = bundle.getString("io.horizontalsystems.ethereumkit.infura_api_key")
                val etherscanKey = bundle.getString("io.horizontalsystems.ethereumkit.etherscan_api_key")

                infuraApiKey = if (infuraKey == null || infuraKey.isBlank()) {
                    throw EthereumKitException.InfuraApiKeyNotSet()
                } else {
                    infuraKey
                }

                etherscanApiKey = if (etherscanKey == null || etherscanKey.isBlank()) {
                    throw EthereumKitException.EtherscanApiKeyNotSet()
                } else {
                    etherscanKey
                }

            } catch (e: PackageManager.NameNotFoundException) {
                throw EthereumKitException.FailedToLoadMetaData(e.message)
            } catch (e: NullPointerException) {
                throw EthereumKitException.FailedToLoadMetaData(e.message)
            }
        }
    }

    enum class NetworkType { MainNet, Ropsten, Kovan, Rinkeby }

    open class EthereumKitException(msg: String) : Exception(msg) {
        class InfuraApiKeyNotSet : EthereumKitException("Infura API Key is not set!")
        class EtherscanApiKeyNotSet : EthereumKitException("Etherscan API Key is not set!")
        class FailedToLoadMetaData(errMsg: String?) : EthereumKitException("Failed to load meta-data, NameNotFound: $errMsg")
        class TokenNotFound(contract: String) : EthereumKitException("ERC20 token not found with contract: $contract")
    }

    sealed class KitState {
        object Synced : KitState()
        object NotSynced : KitState()
        class Syncing(val progress: Double) : KitState()
    }

}

