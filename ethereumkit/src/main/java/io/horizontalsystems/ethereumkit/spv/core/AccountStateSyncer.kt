package io.horizontalsystems.ethereumkit.spv.core

import io.horizontalsystems.ethereumkit.core.ISpvStorage
import io.horizontalsystems.ethereumkit.spv.models.AccountState
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import io.horizontalsystems.ethereumkit.spv.net.handlers.AccountStateTaskHandler
import io.horizontalsystems.ethereumkit.spv.net.tasks.AccountStateTask

class AccountStateSyncer(private val storage: ISpvStorage,
                         private val address: ByteArray) : AccountStateTaskHandler.Listener {

    interface Listener {
        fun onUpdate(accountState: AccountState)
    }

    var listener: Listener? = null

    fun sync(taskPerformer: ITaskPerformer, blockHeader: BlockHeader) {
        taskPerformer.add(AccountStateTask(address, blockHeader))
    }

    override fun didReceive(accountState: AccountState, address: ByteArray, blockHeader: BlockHeader) {
        storage.saveAccountSate(accountState)
        listener?.onUpdate(accountState)
    }

}
