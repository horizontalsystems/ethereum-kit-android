package io.horizontalsystems.ethereumkit.api.core

import io.horizontalsystems.ethereumkit.api.jsonrpc.GetTransactionByHashJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.SendRawTransactionJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransaction
import io.horizontalsystems.ethereumkit.api.models.AccountState
import io.horizontalsystems.ethereumkit.core.IApiStorage
import io.horizontalsystems.ethereumkit.core.TransactionBuilder
import io.horizontalsystems.ethereumkit.crypto.InternalBouncyCastleProvider
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.RawTransaction
import io.horizontalsystems.ethereumkit.models.Signature
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.math.BigInteger
import java.security.Security

// A send that fails is only a failure if no node has the transaction. Reporting a delivered
// transaction as failed makes the user send again, and that is a second payment.
class RpcBlockchainSendTest {

    init {
        // EthereumKit.init() does this in the app; hashing a transaction needs it
        Security.addProvider(InternalBouncyCastleProvider.getInstance())
    }

    private val address = Address("0x1111111111111111111111111111111111111111")
    private val recipient = Address("0x2222222222222222222222222222222222222222")
    private val transactionBuilder = TransactionBuilder(address, chainId = 1)

    private val rawTransaction = RawTransaction(GasPrice.Legacy(1_000_000_000), 21_000, recipient, BigInteger.TEN, nonce = 7)
    private val signature = Signature(37, ByteArray(32) { 1 }, ByteArray(32) { 2 })
    private val expectedHash = transactionBuilder.transaction(rawTransaction, signature).hash

    private val syncer = FakeSyncer()
    private val blockchain = RpcBlockchain(address, FakeStorage(), syncer, transactionBuilder, deliveryCheckTimeoutMs = 5_000)

    private fun rpcError(message: String) = JsonRpc.ResponseError.RpcError(RpcResponse.Error(-32000, message))

    private fun knownTransaction() = RpcTransaction(
        hash = expectedHash,
        nonce = 7,
        blockHash = null,
        blockNumber = null,
        transactionIndex = null,
        from = address,
        to = recipient,
        value = BigInteger.TEN,
        gasPrice = 1_000_000_000,
        maxFeePerGas = null,
        maxPriorityFeePerGas = null,
        gasLimit = 21_000,
        input = ByteArray(0),
    )

    // What a node answers for a hash it does not know: a null result, which fails to parse
    private fun unknownTransaction(): Nothing = throw JsonRpc.ResponseError.InvalidResult("null")

    @Test
    fun acceptedTransactionIsNotLookedUp() = runTest {
        val transaction = blockchain.send(rawTransaction, signature)

        assertArrayEquals(expectedHash, transaction.hash)
        assertEquals(listOf("eth_sendRawTransaction"), syncer.methods)
    }

    // The first node took the transaction but its answer was lost; by the time the next node
    // is asked the transaction is in a block, so that node rejects the nonce.
    @Test
    fun nonceTooLowForADeliveredTransactionIsSuccess() = runTest {
        syncer.onSend = { throw rpcError("nonce too low") }
        syncer.onLookup = { knownTransaction() }

        val transaction = blockchain.send(rawTransaction, signature)

        assertArrayEquals(expectedHash, transaction.hash)
        assertEquals(listOf("eth_sendRawTransaction", "eth_getTransactionByHash"), syncer.methods)
        assertArrayEquals(expectedHash, syncer.lookedUpHash)
    }

    @Test
    fun nonceTooLowForAnotherTransactionKeepsTheError() = runTest {
        val error = rpcError("nonce too low")
        syncer.onSend = { throw error }
        syncer.onLookup = { unknownTransaction() }

        assertSame(error, sendFailure())
    }

    @Test
    fun rejectedTransactionKeepsTheError() = runTest {
        val error = rpcError("insufficient funds for gas * price + value")
        syncer.onSend = { throw error }
        syncer.onLookup = { unknownTransaction() }

        assertSame(error, sendFailure())
    }

    @Test
    fun lostAnswerForADeliveredTransactionIsSuccess() = runTest {
        syncer.onSend = { throw IOException("timeout") }
        syncer.onLookup = { knownTransaction() }

        assertArrayEquals(expectedHash, blockchain.send(rawTransaction, signature).hash)
    }

    @Test
    fun unreachableNodesKeepTheSendError() = runTest {
        val error = IOException("send timeout")
        syncer.onSend = { throw error }
        syncer.onLookup = { throw IOException("lookup timeout") }

        assertSame(error, sendFailure())
    }

    @Test
    fun lookupThatTakesTooLongKeepsTheSendError() = runTest {
        val error = IOException("send timeout")
        syncer.onSend = { throw error }
        syncer.onLookup = {
            delay(60_000)
            knownTransaction()
        }

        assertSame(error, sendFailure())
    }

    @Test
    fun alreadyKnownIsSuccessWithoutLookup() = runTest {
        syncer.onSend = { throw rpcError("already known") }

        assertArrayEquals(expectedHash, blockchain.send(rawTransaction, signature).hash)
        assertEquals(listOf("eth_sendRawTransaction"), syncer.methods)
    }

    private suspend fun sendFailure(): Throwable {
        try {
            blockchain.send(rawTransaction, signature)
        } catch (error: Throwable) {
            return error
        }
        fail("expected the send to fail")
        throw IllegalStateException()
    }

    private class FakeSyncer : IRpcSyncer {
        override var listener: IRpcSyncerListener? = null
        override val source = "fake"
        override val state: SyncerState = SyncerState.Ready

        val methods = mutableListOf<String>()
        var lookedUpHash: ByteArray? = null
        var onSend: suspend () -> Unit = {}
        var onLookup: suspend () -> RpcTransaction = { throw IllegalStateException("unexpected lookup") }

        override fun start() = Unit
        override fun stop() = Unit
        override fun pause() = Unit
        override fun resume() = Unit

        @Suppress("UNCHECKED_CAST")
        override suspend fun <T : Any> execute(rpc: JsonRpc<T>): T {
            methods.add(rpc.method)
            return when (rpc) {
                is SendRawTransactionJsonRpc -> {
                    onSend()
                    ByteArray(32) as T
                }
                is GetTransactionByHashJsonRpc -> {
                    lookedUpHash = rpc.transactionHash
                    onLookup() as T
                }
                else -> throw IllegalStateException("unexpected ${rpc.method}")
            }
        }
    }

    private class FakeStorage : IApiStorage {
        override fun getLastBlockHeight(): Long? = null
        override fun saveLastBlockHeight(lastBlockHeight: Long) = Unit
        override fun getAccountState(): AccountState? = null
        override fun saveAccountState(state: AccountState) = Unit
    }
}
