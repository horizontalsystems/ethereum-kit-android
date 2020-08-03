package io.horizontalsystems.ethereumkit.spv.core

import io.horizontalsystems.ethereumkit.core.ISpvStorage
import io.horizontalsystems.ethereumkit.core.TransactionBuilder
import io.horizontalsystems.ethereumkit.core.TransactionSigner
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.ethereumkit.spv.models.RawTransaction
import io.horizontalsystems.ethereumkit.spv.net.handlers.SendTransactionTaskHandler
import io.horizontalsystems.ethereumkit.spv.net.tasks.SendTransactionTask

class TransactionSender(private val storage: ISpvStorage,
                        private val transactionBuilder: TransactionBuilder,
                        private val transactionSigner: TransactionSigner) : SendTransactionTaskHandler.Listener {

    interface Listener {
        fun onSendSuccess(sendId: Int, transaction: Transaction)
        fun onSendFailure(sendId: Int, error: Throwable)
    }

    var listener: Listener? = null

    fun send(sendId: Int, taskPerformer: ITaskPerformer, rawTransaction: RawTransaction) {
        val accountState = storage.getAccountState() ?: throw NoAccountState()
        val signature = transactionSigner.signature(rawTransaction, accountState.nonce)

        taskPerformer.add(SendTransactionTask(sendId, rawTransaction, accountState.nonce, signature))
    }

    override fun onSendSuccess(task: SendTransactionTask) {
        val transaction = transactionBuilder.transaction(task.rawTransaction, task.nonce, task.signature)

        listener?.onSendSuccess(task.sendId, transaction)
    }

    override fun onSendFailure(task: SendTransactionTask, error: Throwable) {
        listener?.onSendFailure(task.sendId, error)
    }

    open class SendError : Exception()
    class NoAccountState : SendError()

}
