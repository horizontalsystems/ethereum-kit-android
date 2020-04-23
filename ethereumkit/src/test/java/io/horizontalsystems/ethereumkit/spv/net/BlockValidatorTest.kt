package io.horizontalsystems.ethereumkit.spv.net

import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import org.mockito.Mockito.mock
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertFailsWith

class BlockValidatorTest : Spek({

    val blockValidator = BlockValidator()

    val fromBlockHeader = mock(BlockHeader::class.java)
    val fromBlockHeaderHashHex = ByteArray(32) { 1 }

    val block1 = mock(BlockHeader::class.java)
    val block1HashHex = fromBlockHeaderHashHex.copyOf()

    val block2 = mock(BlockHeader::class.java)
    val block2HashHex = ByteArray(32) { 2 }

    val block3 = mock(BlockHeader::class.java)
    val block3HashHex = ByteArray(32) { 3 }

    val blockHeaders = listOf(block1, block2, block3)

    beforeEachTest {
        whenever(fromBlockHeader.hashHex).thenReturn(fromBlockHeaderHashHex)

        whenever(block1.hashHex).thenReturn(block1HashHex)

        whenever(block2.hashHex).thenReturn(block2HashHex)
        whenever(block2.parentHash).thenReturn(block1HashHex)

        whenever(block3.hashHex).thenReturn(block3HashHex)
        whenever(block3.parentHash).thenReturn(block2HashHex)
    }

    describe("#validate") {

        context("when chain is empty") {
            it("throws InvalidChain exception") {
                assertFailsWith<BlockValidator.InvalidChain> {
                    blockValidator.validate(listOf(), fromBlockHeader)
                }
            }
        }

        context("when chain is valid") {
            it("does not throw exception") {
                blockValidator.validate(blockHeaders, fromBlockHeader)
            }
        }

        context("when first return blockHeader's hash is different from fromBlockHeader's hash") {
            beforeEachTest {
                val differentBlock1HashHex = fromBlockHeaderHashHex.map { (it + 1).toByte() }.toByteArray()
                whenever(block1.hashHex).thenReturn(differentBlock1HashHex)
            }
            it("throws ForkDetected exception") {
                assertFailsWith<BlockValidator.ForkDetected> {
                    blockValidator.validate(blockHeaders, fromBlockHeader)
                }
            }
        }

        context("when returned chain is not interconnected") {
            beforeEachTest {
                val differentBlock3ParentHash = block2HashHex.map { (it + 1).toByte() }.toByteArray()
                whenever(block3.parentHash).thenReturn(differentBlock3ParentHash)
            }
            it("throws InvalidChain exception") {
                assertFailsWith<BlockValidator.InvalidChain> {
                    blockValidator.validate(blockHeaders, fromBlockHeader)
                }
            }

        }
    }


})
