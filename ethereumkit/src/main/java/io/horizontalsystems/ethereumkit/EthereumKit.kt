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
import io.realm.OrderedCollectionChangeSet
import io.realm.Realm
import io.realm.RealmResults
import io.realm.annotations.RealmModule
import org.web3j.protocol.Web3j
import org.web3j.protocol.Web3jFactory
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.protocol.http.HttpService
import org.web3j.tx.Transfer
import org.web3j.utils.Convert
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import java.math.BigDecimal


@RealmModule(library = true, allClasses = true)
class EthereumKitModule

class EthereumKit(words: List<String>, networkType: NetworkType) {

    interface Listener {
        fun transactionsUpdated(ethereumKit: EthereumKit, inserted: List<Transaction>, updated: List<Transaction>, deleted: List<Int>)
        fun balanceUpdated(ethereumKit: EthereumKit, balance: Double)
        fun lastBlockHeightUpdated(height: Int)
    }

    enum class NetworkType { MainNet, Ropsten, Kovan, Rinkeby }

    var listener: Listener? = null

    val transactions: List<Transaction>
        get() = transactionRealmResults.map { it }.sortedByDescending { it.blockNumber }

    val balance: Double
        get() = balanceRealmResults.firstOrNull()?.balance ?: 0.0

    val lastBlockHeight: Int?
        get() = lastBlockHeightRealmResults.firstOrNull()?.height

    private var realmFactory: RealmFactory = RealmFactory("ethereumkit-${networkType.name}")
    private val transactionRealmResults: RealmResults<Transaction>
    private val balanceRealmResults: RealmResults<Balance>
    private val lastBlockHeightRealmResults: RealmResults<LastBlockHeight>

    private val web3j: Web3j = Web3jFactory.build(HttpService(getInfuraUrl(networkType)))
    private val hdWallet: HDWallet = HDWallet(Mnemonic().toSeed(words), 60)

    private val address = hdWallet.address()

    private val etherscanService = EtherscanService(networkType, etherscanApiKey)
    private val addressValidator = AddressValidator()
    private var subscriptions: CompositeSubscription = CompositeSubscription()

    init {
        val realm = realmFactory.realm

        transactionRealmResults = realm.where(Transaction::class.java)
                .findAll()
        transactionRealmResults.addChangeListener { t, changeSet ->
            handleTransactions(t, changeSet)
        }

        balanceRealmResults = realm.where(Balance::class.java)
                .findAll()
        balanceRealmResults.addChangeListener { _, changeSet ->
            handleBalance(changeSet)
        }

        lastBlockHeightRealmResults = realm.where(LastBlockHeight::class.java)
                .findAll()
        lastBlockHeightRealmResults.addChangeListener { _, changeSet ->
            handleLastBlockHeight(changeSet)
        }
    }

    fun start() {
        refresh()
    }

    fun refresh() {
        val completion: ((Throwable?) -> (Unit)) = {
            Log.e("EthereumKit", it?.message)
            it?.printStackTrace()
        }
        updateBalance(completion)
        updateLastBlockHeight(completion)
        updateTransactions(completion)
        updateGasPrice()
    }

    fun clear() {
        realmFactory.realm.use { realm ->
            realm.executeTransaction {
                it.deleteAll()
            }
        }
        subscriptions.clear()
    }

    fun receiveAddress() = address

    @Throws
    fun validateAddress(address: String) {
        addressValidator.validate(address)
    }

    fun send(address: String, value: Double, completion: ((Throwable?) -> (Unit))? = null) {
        Transfer.sendFunds(web3j, hdWallet.credentials(), address, BigDecimal.valueOf(value), Convert.Unit.ETHER)
                .observable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    insertSentTransaction(it, Convert.toWei(BigDecimal.valueOf(value), Convert.Unit.ETHER))
                    refresh()
                    completion?.invoke(null)
                }, {
                    completion?.invoke(it)
                }).let {
                    subscriptions.add(it)
                }
    }

    private fun insertSentTransaction(txReceipt: TransactionReceipt, amountInWei: BigDecimal) {
        realmFactory.realm.use {
            val gasPriceInGwei = it.where(GasPrice::class.java).findFirst()?.gasPriceInGwei
                    ?: DEFAULT_GAS_PRICE
            val gasPriceInWei = Convert.toWei(BigDecimal.valueOf(gasPriceInGwei), Convert.Unit.GWEI)

            val transaction = Transaction().apply {
                hash = txReceipt.transactionHash
                timeStamp = System.currentTimeMillis() / 1000
                from = txReceipt.from
                to = txReceipt.to
                gasUsed = txReceipt.gasUsed.toString()
                gasPrice = gasPriceInWei.toBigInteger().toString()
                value = amountInWei.toBigIntegerExact().toString()
                blockNumber = txReceipt.blockNumber.toLong()
                blockHash = txReceipt.blockHash
            }
            it.executeTransaction { realm ->
                realm.insertOrUpdate(transaction)
            }
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

    private fun updateGasPrice() {
        web3j.ethGasPrice()
                .observable()
                .subscribeOn(Schedulers.io())
                .map {
                    Convert.fromWei(it.gasPrice.toBigDecimal(), Convert.Unit.GWEI).toDouble()
                }
                .onErrorReturn { DEFAULT_GAS_PRICE }
                .subscribe { gasPrice ->
                    realmFactory.realm.use { realm ->
                        realm.executeTransaction {
                            it.insertOrUpdate(GasPrice(gasPrice))
                        }
                    }
                }.let {
                    subscriptions.add(it)
                }
    }

    private fun updateBalance(completion: ((Throwable?) -> (Unit))? = null) {
        web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST)
                .observable()
                .subscribeOn(Schedulers.io())
                .map { Convert.fromWei(it.balance.toBigDecimal(), Convert.Unit.ETHER).toDouble() }
                .subscribe({ balance ->
                    realmFactory.realm.use { realm ->
                        realm.executeTransaction {
                            it.insertOrUpdate(Balance(address, balance))
                        }
                    }
                }, {
                    completion?.invoke(it)
                }).let {
                    subscriptions.add(it)
                }
    }

    private fun updateLastBlockHeight(completion: ((Throwable?) -> Unit)? = null) {
        web3j.ethBlockNumber()
                .observable()
                .subscribeOn(Schedulers.io())
                .map { it.blockNumber.toInt() }
                .subscribe({ lastBlockHeight ->
                    realmFactory.realm.use { realm ->
                        realm.executeTransaction {
                            it.insertOrUpdate(LastBlockHeight(lastBlockHeight))
                        }
                    }
                }, {
                    completion?.invoke(it)
                }).let {
                    subscriptions.add(it)
                }
    }

    private fun updateTransactions(completion: ((Throwable?) -> (Unit))? = null) {
        etherscanService.getTransactionList(address, (lastBlockHeight ?: 0) + 1)
                .subscribeOn(Schedulers.io())
                .subscribe({ etherscanResponse ->
                    realmFactory.realm.use { realm ->
                        realm.executeTransaction {
                            etherscanResponse.result.map { etherscanTx -> Transaction(etherscanTx) }.forEach { transaction ->
                                it.insertOrUpdate(transaction)
                            }
                        }
                    }
                }, {
                    completion?.invoke(it)
                }).let { subscriptions.add(it) }

    }

    private fun handleTransactions(collection: RealmResults<Transaction>, changeSet: OrderedCollectionChangeSet) {
        if (changeSet.state == OrderedCollectionChangeSet.State.UPDATE) {
            listener?.let { listener ->
                val inserted = changeSet.insertions.asList().mapNotNull { collection[it] }
                val updated = changeSet.changes.asList().mapNotNull { collection[it] }
                val deleted = changeSet.deletions.asList()

                listener.transactionsUpdated(this, inserted, updated, deleted)
            }
        }
    }

    private fun handleBalance(changeSet: OrderedCollectionChangeSet) {
        if (changeSet.state == OrderedCollectionChangeSet.State.UPDATE) {
            listener?.balanceUpdated(this, balance)
        }
    }

    private fun handleLastBlockHeight(changeSet: OrderedCollectionChangeSet) {
        if (changeSet.state == OrderedCollectionChangeSet.State.UPDATE) {
            lastBlockHeight?.let { listener?.lastBlockHeightUpdated(it) }
        }
    }

    private fun getInfuraUrl(network: NetworkType): String {
        val subDomain = when (network) {
            NetworkType.MainNet -> "mainnet."
            NetworkType.Kovan -> "kovan."
            NetworkType.Rinkeby -> "rinkeby."
            NetworkType.Ropsten -> "ropsten."
        }
        return "https://${subDomain}infura.io/v3/$infuraApiKey"
    }

    companion object {
        private const val DEFAULT_GAS_PRICE = 41.0 //in GWei
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

}

