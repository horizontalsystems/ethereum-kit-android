package io.horizontalsystems.ethereumkit.spv.net.tasks

import io.horizontalsystems.ethereumkit.spv.core.ITask
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader

class BlockHeadersTask(val blockHeader: BlockHeader, val limit: Int, val reverse: Boolean = false) : ITask