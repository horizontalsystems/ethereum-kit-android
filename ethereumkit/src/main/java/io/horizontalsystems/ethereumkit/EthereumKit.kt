package io.horizontalsystems.ethereumkit

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.ethereumkit.core.RealmFactory
import io.horizontalsystems.ethereumkit.core.address
import io.horizontalsystems.ethereumkit.core.credentials
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
import io.reactivex.functions.Function4
import io.reactivex.schedulers.Schedulers
import io.realm.OrderedCollectionChangeSet
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import io.realm.annotations.RealmModule
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.tuples.generated.Tuple4
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal


@RealmModule(library = true, allClasses = true)
class EthereumKitModule

class EthereumKit(words: List<String>, networkType: NetworkType) {

    interface Listener {
        fun transactionsUpdated(inserted: List<Transaction>, updated: List<Transaction>, deleted: List<Int>)
        fun balanceUpdated(balance: Double)
        fun lastBlockHeightUpdated(height: Int)
        fun onKitStateUpdate(state: KitState)
    }

    enum class NetworkType { MainNet, Ropsten, Kovan, Rinkeby }

    var listener: Listener? = null

    var balance: Double = 0.0
        private set

    var lastBlockHeight: Int? = null
        private set

    private var realmFactory: RealmFactory = RealmFactory("ethereumkit-${networkType.name}")
    private val transactionRealmResults: RealmResults<Transaction>
    private val balanceRealmResults: RealmResults<Balance>
    private val lastBlockHeightRealmResults: RealmResults<LastBlockHeight>

    private val web3j: Web3j = Web3j.build(HttpService(getInfuraUrl(networkType)))
    private val hdWallet: HDWallet = HDWallet(Mnemonic().toSeed(words), 60)

    private val address = hdWallet.address()

    private val etherscanService = EtherscanService(networkType, etherscanApiKey)
    private val addressValidator = AddressValidator()
    private var disposables = CompositeDisposable()

    private var timer: Timer

    init {
        val realm = realmFactory.realm

        transactionRealmResults = realm.where(Transaction::class.java)
                .findAll()
        transactionRealmResults.addChangeListener { t, changeSet ->
            handleTransactions(t, changeSet)
        }

        balanceRealmResults = realm.where(Balance::class.java)
                .findAll()
        balanceRealmResults.addChangeListener { balanceCollection, _ ->
            balance = balanceCollection.firstOrNull()?.balance ?: 0.0
            listener?.balanceUpdated(balance)
        }

        lastBlockHeightRealmResults = realm.where(LastBlockHeight::class.java)
                .findAll()
        lastBlockHeightRealmResults.addChangeListener { lastBlockHeightCollection, _ ->
            lastBlockHeight = lastBlockHeightCollection.firstOrNull()?.height
            lastBlockHeight?.let { listener?.lastBlockHeightUpdated(it) }
        }

        timer = Timer(30, object : Timer.Listener {
            override fun onTimeIsUp() {
                refresh()
            }
        })
    }

    fun start() {
        timer.start()
        refresh()
    }

    @Synchronized
    fun refresh() {
        listener?.onKitStateUpdate(KitState.Syncing(0.0))

        Flowable.zip(updateBalance(),
                updateLastBlockHeight(),
                updateTransactions(),
                updateGasPrice(),
                Function4 { balance: Double, lbh: Int, txCount: Int, gasPrice: Double ->
                    Tuple4(balance, lbh, txCount, gasPrice)
                })
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .subscribe({
                    listener?.onKitStateUpdate(KitState.Synced)
                }, {
                    Log.e("EthereumKit", it?.message)
                    it?.printStackTrace()
                    listener?.onKitStateUpdate(KitState.NotSynced)

                }).let {
                    disposables.add(it)
                }
    }

    fun transactions(fromHash: String? = null, limit: Int? = null): Single<List<Transaction>> {
        return Single.create { subscriber ->
            realmFactory.realm.use { realm ->
                var results = realm.where(Transaction::class.java)
                        .sort("timeStamp", Sort.DESCENDING)

                fromHash?.let { fromHash ->
                    realm.where(Transaction::class.java)
                            .equalTo("hash", fromHash)
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
        realmFactory.realm.use { realm ->
            realm.executeTransaction {
                it.deleteAll()
            }
        }
        disposables.clear()
        timer.stop()
        web3j.shutdown()
    }

    fun receiveAddress() = address

    @Throws
    fun validateAddress(address: String) {
        addressValidator.validate(address)
    }

    private fun getGasPrice() = realmFactory.realm.use {
        val gasPriceInGwei = it.where(GasPrice::class.java).findFirst()?.gasPriceInGwei
                ?: DEFAULT_GAS_PRICE
        Convert.toWei(BigDecimal.valueOf(gasPriceInGwei), Convert.Unit.GWEI).toBigInteger()
    }

    private fun broadCastTransaction(toAddress: String, value: Double) =
            Flowable.fromCallable {
                // get the next available nonce
                val ethGetTransactionCount = web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).send()
                val nonce = ethGetTransactionCount.transactionCount

                val gasPrice = getGasPrice()
                val gasLimit = GAS_LIMIT.toBigInteger()
                val valueInWei = Convert.toWei(BigDecimal.valueOf(value), Convert.Unit.ETHER).toBigInteger()

                // create our transaction
                val rawTransaction = RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, toAddress, valueInWei)

                // sign & send our transaction
                val signedMessage = TransactionEncoder.signMessage(rawTransaction, hdWallet.credentials())
                val hexValue = Numeric.toHexString(signedMessage)
                web3j.ethSendRawTransaction(hexValue).send()
            }

    fun send(toAddress: String, amount: Double, completion: ((Throwable?) -> (Unit))? = null) {

        broadCastTransaction(toAddress, amount)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { ethSendTransaction ->
                    val pendingTx = Transaction().apply {
                        hash = ethSendTransaction.transactionHash
                        timeStamp = System.currentTimeMillis() / 1000
                        from = address
                        to = toAddress
                        this.value = Convert.toWei(BigDecimal.valueOf(amount), Convert.Unit.ETHER).toBigInteger().toString()
                    }
                    realmFactory.realm.use { realm ->
                        realm.executeTransaction {
                            it.insertOrUpdate(pendingTx)
                        }
                    }
                }
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

    private fun updateGasPrice(): Flowable<Double> =
            web3j.ethGasPrice()
                    .flowable()
                    .map {
                        Convert.fromWei(it.gasPrice.toBigDecimal(), Convert.Unit.GWEI).toDouble()
                    }
                    .onErrorReturn { DEFAULT_GAS_PRICE }
                    .map { gasPrice ->
                        realmFactory.realm.use { realm ->
                            realm.executeTransaction {
                                it.insertOrUpdate(GasPrice(gasPrice))
                            }
                        }
                        gasPrice
                    }

    private fun updateBalance(): Flowable<Double> =
            web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST)
                    .flowable()
                    .map { Convert.fromWei(it.balance.toBigDecimal(), Convert.Unit.ETHER).toDouble() }
                    .map { balance ->
                        realmFactory.realm.use { realm ->
                            realm.executeTransaction {
                                it.insertOrUpdate(Balance(address, balance))
                            }
                        }
                        balance
                    }

    private fun updateLastBlockHeight(): Flowable<Int> =
            web3j.ethBlockNumber()
                    .flowable()
                    .map { it.blockNumber.toInt() }
                    .map { lastBlockHeight ->
                        realmFactory.realm.use { realm ->
                            realm.executeTransaction {
                                it.insertOrUpdate(LastBlockHeight(lastBlockHeight))
                            }
                        }
                        lastBlockHeight
                    }


    private fun updateTransactions(): Flowable<Int> {

        var lastBlockHeight = this.lastBlockHeight

        realmFactory.realm.use { realm ->
            lastBlockHeight = realm.where(Transaction::class.java)
                    .sort("blockNumber", Sort.DESCENDING)
                    .findFirst()?.blockNumber?.toInt()
        }

        return etherscanService.getTransactionList(address, (lastBlockHeight ?: 0) + 1)
                .map { etherscanResponse ->
                    realmFactory.realm.use { realm ->
                        realm.executeTransaction {
                            etherscanResponse.result.map { etherscanTx -> Transaction(etherscanTx) }.forEach { transaction ->
                                it.insertOrUpdate(transaction)
                            }
                        }
                    }
                    etherscanResponse.result.size
                }

    }

    private fun handleTransactions(collection: RealmResults<Transaction>, changeSet: OrderedCollectionChangeSet) {
        if (changeSet.state == OrderedCollectionChangeSet.State.UPDATE) {
            listener?.let { listener ->
                val inserted = changeSet.insertions.asList().mapNotNull { collection[it] }
                val updated = changeSet.changes.asList().mapNotNull { collection[it] }
                val deleted = changeSet.deletions.asList()

                listener.transactionsUpdated(inserted, updated, deleted)
            }
        }
    }

    private fun getInfuraUrl(network: NetworkType): String {
        val subDomain = when (network) {
            NetworkType.MainNet -> "mainnet."
            NetworkType.Kovan -> "kovan."
            NetworkType.Rinkeby -> "rinkeby."
            NetworkType.Ropsten -> "ropsten."
        }
        return "https://${subDomain}infura.io/$infuraApiKey"
    }

    companion object {
        private const val DEFAULT_GAS_PRICE = 10.0 //in GWei
        private const val GAS_LIMIT = 21_000

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

    open class EthereumKitException(msg: String) : Exception(msg) {
        class InfuraApiKeyNotSet : EthereumKitException("Infura API Key is not set!")
        class EtherscanApiKeyNotSet : EthereumKitException("Etherscan API Key is not set!")
        class FailedToLoadMetaData(errMsg: String?) : EthereumKitException("Failed to load meta-data, NameNotFound: $errMsg")
    }

    sealed class KitState {
        object Synced : KitState()
        object NotSynced : KitState()
        class Syncing(val progress: Double) : KitState()
    }

}

