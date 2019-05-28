package io.horizontalsystems.ethereumkit.spv.net.tasks

import io.horizontalsystems.ethereumkit.spv.core.ITask
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader

class AccountStateTask(val address: ByteArray, val blockHeader: BlockHeader): ITask
