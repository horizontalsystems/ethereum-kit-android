package io.horizontalsystems.ethereumkit.spv.net

import io.horizontalsystems.ethereumkit.spv.models.BlockHeader

class BlockValidator {

    @Throws(BlockValidationError::class)
    fun validate(blockHeaders: List<BlockHeader>, fromBlockHeader: BlockHeader) {

        check(blockHeaders.isNotEmpty()) {
            throw InvalidChain()
        }

        check(blockHeaders[0].hashHex.contentEquals(fromBlockHeader.hashHex)) {
            throw ForkDetected()
        }

        var prevBlock = blockHeaders[0]

        for (blockHeader in blockHeaders.drop(1)) {
            check(blockHeader.parentHash.contentEquals(prevBlock.hashHex)) {
                throw InvalidChain()
            }
            prevBlock = blockHeader
        }
    }

    open class BlockValidationError : Exception()
    class ForkDetected : BlockValidationError()
    class InvalidChain : BlockValidationError()
    class InvalidProofOfWork : BlockValidationError()

}
