package io.horizontalsystems.merkleiokit

import io.horizontalsystems.ethereumkit.api.core.IRpcSyncer
import io.horizontalsystems.ethereumkit.api.jsonrpc.GetTransactionByHashJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.GetTransactionCountJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransaction
import io.horizontalsystems.ethereumkit.core.INonceProvider
import io.horizontalsystems.ethereumkit.core.TransactionBuilder
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.horizontalsystems.ethereumkit.models.RawTransaction
import io.horizontalsystems.ethereumkit.models.Signature
import io.horizontalsystems.ethereumkit.models.Transaction
import java.util.Optional

class MerkleRpcBlockchain(
    private val address: Address,
    private val manager: MerkleTransactionHashManager,
    private val syncer: IRpcSyncer,
    private val transactionBuilder: TransactionBuilder
) : INonceProvider {

    override suspend fun getNonce(defaultBlockParameter: DefaultBlockParameter): Long {
        // sync only if needed pending/ because others will be same with main blockchain
        if (defaultBlockParameter != DefaultBlockParameter.Pending) {
            return 0
        }

        return syncer.execute(GetTransactionCountJsonRpc(address, defaultBlockParameter))
    }

    suspend fun send(rawTransaction: RawTransaction, signature: Signature, sourceTag: String): Transaction {
        val tx = transactionBuilder.transaction(rawTransaction, signature)
        val encoded = transactionBuilder.encode(rawTransaction, signature)

        val txHash = syncer.execute(MerkleSendRawTransactionJsonRpc(encoded, sourceTag))
        manager.save(MerkleTransactionHash(txHash))

        return tx
    }

    suspend fun transaction(transactionHash: ByteArray): Optional<RpcTransaction> {
        return try {
            Optional.of(syncer.execute(GetTransactionByHashJsonRpc(transactionHash)))
        } catch (throwable: JsonRpc.ResponseError.InvalidResult) {
            Optional.empty()
        }
    }
}
