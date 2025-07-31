package io.horizontalsystems.merkleiokit

import io.horizontalsystems.ethereumkit.api.core.IRpcSyncer
import io.horizontalsystems.ethereumkit.api.jsonrpc.GetTransactionByHashJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.GetTransactionCountJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.SendRawTransactionJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransaction
import io.horizontalsystems.ethereumkit.core.INonceProvider
import io.horizontalsystems.ethereumkit.core.TransactionBuilder
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.horizontalsystems.ethereumkit.models.RawTransaction
import io.horizontalsystems.ethereumkit.models.Signature
import io.horizontalsystems.ethereumkit.models.Transaction
import io.reactivex.Single

class MerkleRpcBlockchain(
    private val address: Address,
    val chain: Chain,
    private val manager: MerkleTransactionHashManager,
    private val syncer: IRpcSyncer,
    private val transactionBuilder: TransactionBuilder
) : INonceProvider {

    override fun getNonce(defaultBlockParameter: DefaultBlockParameter): Single<Long> {
        // sync only if needed pending/ because others will be same with main blockchain
        if (defaultBlockParameter != DefaultBlockParameter.Pending) {
            return Single.just(0)
        }

        return syncer.single(GetTransactionCountJsonRpc(address, defaultBlockParameter))
    }

    fun send(rawTransaction: RawTransaction, signature: Signature): Single<Transaction> {
        val tx = transactionBuilder.transaction(rawTransaction, signature)
        val encoded = transactionBuilder.encode(rawTransaction, signature)

        return syncer.single(SendRawTransactionJsonRpc(encoded))
            .map { txHash ->
                manager.save(MerkleTransactionHash(txHash, chain.id))

                tx
            }
    }

    fun transaction(transactionHash: ByteArray): Single<RpcTransaction> {
        return syncer.single(GetTransactionByHashJsonRpc(transactionHash))
    }
}
