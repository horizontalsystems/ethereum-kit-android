package io.horizontalsystems.ethereumkit

import android.content.Context
import io.horizontalsystems.ethereumkit.core.RealmFactory
import io.horizontalsystems.ethereumkit.core.address
import io.horizontalsystems.ethereumkit.core.credentials
import io.horizontalsystems.ethereumkit.models.Balance
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.ethereumkit.network.EtherscanService
import io.horizontalsystems.ethereumkit.network.NetworkType
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.realm.OrderedCollectionChangeSet
import io.realm.Realm
import io.realm.RealmResults
import io.realm.annotations.RealmModule
import org.web3j.protocol.Web3j
import org.web3j.protocol.Web3jFactory
import org.web3j.protocol.core.DefaultBlockParameterName
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
        fun progressUpdated(ethereumKit: EthereumKit, progress: Double)
    }

    var listener: Listener? = null

    val transactions: List<Transaction>
        get() = transactionRealmResults.map { it }.sortedByDescending { it.blockNumber }

    val balance: Double
        get() = balanceRealmResults.firstOrNull()?.balance ?: 0.0

    private var realmFactory: RealmFactory = RealmFactory(networkType.name)
    private val transactionRealmResults: RealmResults<Transaction>
    private val balanceRealmResults: RealmResults<Balance>

    private val web3j: Web3j = Web3jFactory.build(HttpService("https://kovan.infura.io/v3/2a1306f1d12f4c109a4d4fb9be46b02e"))
    private val hdWallet: HDWallet = HDWallet(Mnemonic().toSeed(words), 60)

    private val etherscanService = EtherscanService(networkType)
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

//        val web3ClientVersion = web3j.web3ClientVersion().send().web3ClientVersion
//
//        println("web3clientVersion = $web3ClientVersion")
//
    }

    fun start() {
        refresh()
    }

    private fun refresh() {
        updateBalance()
        updateTransactions()
    }

    fun clear() {
        val realm = realmFactory.realm
        realm.executeTransaction {
            it.deleteAll()
        }
        realm.close()

        subscriptions.clear()
    }

    fun receiveAddress(): String {
        return hdWallet.address()
    }

    fun send(address: String, value: Double, completion: ((Throwable?) -> (Unit))? = null) {
        Transfer.sendFunds(web3j, hdWallet.credentials(), address, BigDecimal.valueOf(value), Convert.Unit.ETHER)
                .observable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError { throwable ->
                    completion?.invoke(throwable)
                }
                .subscribe {
                    completion?.invoke(null)
                    refresh()
                }.let {
                    subscriptions.add(it)
                }
    }

    private fun updateBalance() {
        val address = hdWallet.address()
        web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST)
                .observable()
                .subscribeOn(Schedulers.io())
                .map { Convert.fromWei(it.balance.toBigDecimal(), Convert.Unit.ETHER).toDouble() }
                .subscribe { balance ->
                    realmFactory.realm.use { realm ->
                        realm.executeTransaction {
                            it.insertOrUpdate(Balance(address, balance))
                        }
                    }
                }.let {
                    subscriptions.add(it)
                }
    }

    private fun updateTransactions() {
        etherscanService.getTransactionList(hdWallet.address())
                .subscribeOn(Schedulers.io())
                .subscribe { etherscanResponse ->
                    realmFactory.realm.use { realm ->
                        realm.executeTransaction {
                            etherscanResponse.result.map { etherscanTx -> Transaction(etherscanTx) }.forEach { transaction ->
                                it.insertOrUpdate(transaction)
                            }
                        }
                    }
                }.let { subscriptions.add(it) }

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

    companion object {
        fun init(context: Context) {
            Realm.init(context)
        }
    }

}

