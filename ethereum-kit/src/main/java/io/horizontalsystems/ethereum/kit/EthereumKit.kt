package io.horizontalsystems.ethereum.kit

import android.content.Context
import io.horizontalsystems.ethereum.kit.core.RealmFactory
import io.horizontalsystems.ethereum.kit.core.address
import io.horizontalsystems.ethereum.kit.core.credentials
import io.horizontalsystems.ethereum.kit.models.Transaction
import io.horizontalsystems.ethereum.kit.network.EtherscanService
import io.horizontalsystems.ethereum.kit.network.NetworkType
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.realm.OrderedCollectionChangeSet
import io.realm.Realm
import io.realm.RealmResults
import io.realm.annotations.RealmModule
import org.web3j.protocol.Web3j
import org.web3j.protocol.Web3jFactory
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
        fun balanceUpdated(ethereumKit: EthereumKit, balance: Long)
        fun progressUpdated(ethereumKit: EthereumKit, progress: Double)
    }

    var listener: Listener? = null

    val transactions: List<Transaction>
        get() = transactionRealmResults.map { it }.sortedByDescending { it.blockNumber }

    private var realmFactory: RealmFactory = RealmFactory(networkType.name)
    private val transactionRealmResults: RealmResults<Transaction>

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

//        val web3ClientVersion = web3j.web3ClientVersion().send().web3ClientVersion
//
//        println("web3clientVersion = $web3ClientVersion")
//
    }

    fun start() {
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
        Transfer.sendFunds(
                web3j, hdWallet.credentials(),
                address,
                BigDecimal.valueOf(value), Convert.Unit.ETHER)
                .observable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError { throwable ->
                    completion?.invoke(throwable)
                }
                .subscribe {
                    completion?.invoke(null)
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

    companion object {
        fun init(context: Context) {
            Realm.init(context)
        }
    }

}

