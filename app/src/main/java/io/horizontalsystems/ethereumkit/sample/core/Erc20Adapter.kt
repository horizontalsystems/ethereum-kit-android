package io.horizontalsystems.ethereumkit.sample.core

import io.horizontalsystems.erc20kit.core.Erc20Kit
import io.horizontalsystems.erc20kit.core.IErc20TokenListener
import io.horizontalsystems.erc20kit.models.TransactionInfo
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.reactivex.Single
import java.math.BigDecimal

class Erc20Adapter(private val erc20Kit: Erc20Kit,
                   ethereumKit: EthereumKit,
                   private val contractAddress: String,
                   position: Int,
                   decimal: Int) : BaseAdapter(ethereumKit, decimal), IErc20TokenListener {

    init {
        erc20Kit.register(contractAddress, position, this)
    }

    override val syncState: EthereumKit.SyncState
        get() = when (erc20Kit.syncState(contractAddress)) {
            Erc20Kit.SyncState.Synced -> EthereumKit.SyncState.Synced
            Erc20Kit.SyncState.NotSynced -> EthereumKit.SyncState.NotSynced
            Erc20Kit.SyncState.Syncing -> EthereumKit.SyncState.Syncing
        }

    override val balanceString: String?
        get() = erc20Kit.balance(contractAddress)

    override fun sendSingle(address: String, amount: String): Single<Unit> {
        return erc20Kit.sendSingle(contractAddress, address, amount, 5_000_000_000).map { Unit }
    }

    private fun transactionRecord(transaction: TransactionInfo): TransactionRecord {
        val mineAddress = ethereumKit.receiveAddress

        val from = TransactionAddress(transaction.from, transaction.from == mineAddress)
        val to = TransactionAddress(transaction.to, transaction.to == mineAddress)

        var amount: BigDecimal

        transaction.value.toBigDecimal().let {
            amount = it.movePointLeft(decimal)
            if (from.mine) {
                amount = -amount
            }
        }

        val logIndex = "${transaction.logIndex ?: ""}"

        return TransactionRecord(
                transactionHash = transaction.transactionHash + logIndex,
                blockHeight = transaction.blockNumber,
                amount = amount,
                timestamp = transaction.timestamp,
                from = from,
                to = to)
    }

    override fun transactionsSingle(hashFrom: String?, limit: Int?): Single<List<TransactionRecord>> {
        var resolvedHashFrom: ByteArray? = null
        var resolvedIndexFrom: Int? = null

        if (hashFrom != null) {
            resolvedHashFrom = hashFrom.substring(0, 32).hexStringToByteArray()
            resolvedIndexFrom = hashFrom.substring(32).toInt(10)
        }

        return erc20Kit.transactionsSingle(contractAddress, resolvedHashFrom, resolvedIndexFrom, limit)
                .map { transactions ->
                    transactions.map { transactionRecord(it) }
                }
    }


    // IErc20TokenListener

    override fun onUpdate(transactions: List<TransactionInfo>) {
        transactionSubject.onNext(Unit)
    }

    override fun onUpdateBalance() {
        balanceSubject.onNext(Unit)
    }

    override fun onUpdateSyncState() {
        syncStateUpdateSubject.onNext(Unit)
    }

}
