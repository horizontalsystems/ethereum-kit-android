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
import org.web3j.tuples.generated.Tuple2
import org.web3j.tuples.generated.Tuple4
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

@RealmModule(library = true, allClasses = true)
class EthereumKitModule

class EthereumKit(words: List<String>, networkType: NetworkType) {

    interface Listener {
        fun onTransactionsUpdate(inserted: List<Transaction>, updated: List<Transaction>, deleted: List<Int>)
        fun onBalanceUpdate(address: String, balance: Double)
        fun onLastBlockHeightUpdate(height: Int)
        fun onKitStateUpdate(contractAddress: String?, state: KitState)
    }

    var listener: Listener? = null

    val balanceERC20 = hashMapOf<String, Double>()
    var balance: Double = 0.0
        private set

    var lastBlockHeight: Int? = null
        private set

    private val erc20List = mutableListOf<ERC20>()

    private var realmFactory: RealmFactory = RealmFactory("ethereumkit-${networkType.name}")
    private val transactionRealmResults: RealmResults<Transaction>
    private val balanceRealmResults: RealmResults<Balance>
    private val lastBlockHeightRealmResults: RealmResults<LastBlockHeight>

    private val hdWallet: HDWallet = HDWallet(Mnemonic().toSeed(words), 1)

    private val address = hdWallet.address()

    private val web3j = Web3jInfura(networkType, infuraApiKey)
    private val etherscanService = EtherscanService(networkType, etherscanApiKey)
    private val addressValidator = AddressValidator()
    private var disposables = CompositeDisposable()

    private val timer: Timer
    private val timerERC20: Timer

    init {
        val realm = realmFactory.realm

        transactionRealmResults = realm.where(Transaction::class.java).findAll()
        transactionRealmResults.addChangeListener { t, changeSet ->
            handleTransactions(t, changeSet)
        }

        balanceRealmResults = realm.where(Balance::class.java).findAll()
        balanceRealmResults.addChangeListener { collection, _ ->
            collection.forEach {
                if (it.address == address) {
                    balance = it.balance
                } else {
                    balanceERC20[it.address] = it.balance
                }

                listener?.onBalanceUpdate(it.address, it.balance)
            }
        }

        lastBlockHeightRealmResults = realm.where(LastBlockHeight::class.java).findAll()
        lastBlockHeightRealmResults.addChangeListener { lastBlockHeightCollection, _ ->
            lastBlockHeight = lastBlockHeightCollection.firstOrNull()?.height
            lastBlockHeight?.let { listener?.onLastBlockHeightUpdate(it) }
        }

        timer = Timer(30, object : Timer.Listener {
            override fun onTimeIsUp() {
                refresh()
            }
        })

        timerERC20 = Timer(30, object : Timer.Listener {
            override fun onTimeIsUp() {
                erc20List.forEach { refresh(it) }
            }
        })
    }

    fun start() {
        timer.start()
        timerERC20.start()

        refresh()
        erc20List.forEach { refresh(it) }
    }

    fun include(contractAddress: String, decimal: Int) {
        erc20List.add(ERC20(contractAddress, decimal))
    }

    fun exclude(contractAddress: String) {
        erc20List.removeAll { it.contractAddress == contractAddress }
    }

    @Synchronized
    fun refresh() {
        listener?.onKitStateUpdate(null, KitState.Syncing(0.0))
        erc20List.forEach { refresh(it) }

        Flowable.zip(updateBalance(), updateLastBlockHeight(), updateTransactions(), updateGasPrice(),
                Function4 { b: Double, h: Int, t: Int, g: Double ->
                    Tuple4(b, h, t, g)
                })
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .subscribe({
                    listener?.onKitStateUpdate(null, KitState.Synced)
                }, {
                    it?.printStackTrace()
                    listener?.onKitStateUpdate(null, KitState.NotSynced)
                }).let {
                    disposables.add(it)
                }
    }

    fun transactions(fromHash: String? = null, limit: Int? = null): Single<List<Transaction>> {
        return Single.create { subscriber ->
            realmFactory.realm.use { realm ->
                var results = realm.where(Transaction::class.java)
                        .equalTo("contractAddress", "")
                        .equalTo("input", "0x")
                        .sort("timeStamp", Sort.DESCENDING)

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

    fun clear() {
        disposables.clear()
        timer.stop()
        timerERC20.stop()
        web3j.shutdown()
        realmFactory.realm.use { realm ->
            realm.executeTransaction {
                it.deleteAll()
            }
        }
    }

    fun receiveAddress() = address

    fun validateAddress(address: String) {
        addressValidator.validate(address)
    }

    fun send(toAddress: String, amount: Double, completion: ((Throwable?) -> (Unit))? = null) {
        val value = Convert.toWei(BigDecimal.valueOf(amount), Convert.Unit.ETHER).toBigInteger()

        broadcastTransaction(toAddress, value)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    completion?.invoke(null)
                    refresh()
                }, {
                    completion?.invoke(it)
                }).let {
                    disposables.add(it)
                }
    }

    fun fee(): Double {
        realmFactory.realm.use { realm ->
            val gasPriceInGwei = realm.where(GasPrice::class.java).findFirst()?.gasPriceInGwei
                    ?: DEFAULT_GAS_PRICE
            val gasPriceInWei = Convert.toWei(BigDecimal.valueOf(gasPriceInGwei), Convert.Unit.GWEI)
            return Convert.fromWei(gasPriceInWei.multiply(BigDecimal(GAS_LIMIT)), Convert.Unit.ETHER).toDouble()
        }
    }

    //
    // ERC20
    //

    @Synchronized
    fun refresh(erc20: ERC20) {
        listener?.onKitStateUpdate(erc20.contractAddress, KitState.Syncing(0.0))

        Flowable.zip(updateBalance(erc20), updateTransactions(true),
                BiFunction { b: Double, t: Int ->
                    Tuple2(b, t)
                })
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .subscribe({
                    listener?.onKitStateUpdate(erc20.contractAddress, KitState.Synced)
                }, {
                    it?.printStackTrace()
                    listener?.onKitStateUpdate(erc20.contractAddress, KitState.NotSynced)
                }).let {
                    disposables.add(it)
                }
    }

    fun sendERC20(toAddress: String, contractAddress: String, decimal: Int, amount: Double, completion: ((Throwable?) -> (Unit))? = null) {
        val erc20 = erc20List.find { it.contractAddress == contractAddress }
                ?: throw EthereumKitException.TokenNotFound(contractAddress)

        val value = BigDecimal.valueOf(amount).multiply(BigDecimal.TEN.pow(decimal)).toBigInteger()

        broadcastTransaction(toAddress, value, erc20.contractAddress)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    completion?.invoke(null)
                    refresh(erc20)
                }, {
                    completion?.invoke(it)
                }).let {
                    disposables.add(it)
                }
    }

    fun transactionsERC20(contractAddress: String, fromHash: String? = null, limit: Int? = null): Single<List<Transaction>> {
        return Single.create { subscriber ->
            realmFactory.realm.use { realm ->
                var results = realm.where(Transaction::class.java)
                        .equalTo("contractAddress", contractAddress)
                        .sort("timeStamp", Sort.DESCENDING)

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

    //
    // ETHEREUM
    //

    private fun broadcastTransaction(toAddress: String, amount: BigInteger, contractAddress: String? = null): Flowable<Unit> {
        return Flowable.fromCallable {
            //  get the next available nonce
            val ethGetTransactionCount = web3j.getTransactionCount(address)
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
                from = address
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
        val gasPriceInGwei = it.where(GasPrice::class.java).findFirst()?.gasPriceInGwei
                ?: DEFAULT_GAS_PRICE
        Convert.toWei(BigDecimal.valueOf(gasPriceInGwei), Convert.Unit.GWEI).toBigInteger()
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

    private fun updateBalance(erc20: ERC20? = null): Flowable<Double> {
        val flowable = if (erc20 == null) {
            web3j.getBalance(address)
        } else {
            web3j.getTokenBalance(address, erc20)
        }

        return flowable.map { balance ->
            val record = Balance(erc20?.contractAddress ?: address, balance, erc20?.decimal ?: 18)
            realmFactory.realm.use { realm ->
                realm.executeTransaction {
                    it.insertOrUpdate(record)
                }
            }

            balance
        }
    }

    private fun updateLastBlockHeight(): Flowable<Int> {
        return web3j.getBlockNumber()
                .map { lastBlockHeight ->
                    realmFactory.realm.use { realm ->
                        realm.executeTransaction {
                            it.insertOrUpdate(LastBlockHeight(lastBlockHeight))
                        }
                    }
                    lastBlockHeight
                }
    }

    private fun updateTransactions(isToken: Boolean = false): Flowable<Int> {

        var lastBlockHeight = this.lastBlockHeight

        realmFactory.realm.use { realm ->
            lastBlockHeight = realm.where(Transaction::class.java)
                    .sort("blockNumber", Sort.DESCENDING)
                    .findFirst()?.blockNumber?.toInt()
        }

        return etherscanService
                .getTransactionList(address, (lastBlockHeight ?: 0) + 1, token = isToken)
                .map { response ->
                    realmFactory.realm.use { realm ->
                        realm.executeTransaction {
                            response.result.map { tx -> Transaction(tx) }.forEach { tx ->
                                realm.insertOrUpdate(tx)
                            }
                        }
                    }

                    response.result.size
                }
    }

    private fun handleTransactions(collection: RealmResults<Transaction>, changeSet: OrderedCollectionChangeSet) {
        if (changeSet.state == OrderedCollectionChangeSet.State.UPDATE) {
            val skipInvalid: (Transaction) -> Boolean = { it.contractAddress.isNotEmpty() && it.input != "0x" }

            listener?.let { listener ->
                val inserted = changeSet.insertions.asList().mapNotNull { collection[it] }.filter(skipInvalid)
                val updated = changeSet.changes.asList().mapNotNull { collection[it] }.filter(skipInvalid)
                val deleted = changeSet.deletions.asList()

                listener.onTransactionsUpdate(inserted, updated, deleted)
            }
        }
    }

    companion object {
        private const val DEFAULT_GAS_PRICE = 10.0 //in GWei
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

