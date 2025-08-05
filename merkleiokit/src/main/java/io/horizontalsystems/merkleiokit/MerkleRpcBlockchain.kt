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
import io.reactivex.Single
import java.util.Optional

class MerkleRpcBlockchain(
    private val address: Address,
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

    fun send(rawTransaction: RawTransaction, signature: Signature, sourceTag: String): Single<Transaction> {
        val tx = transactionBuilder.transaction(rawTransaction, signature)
        val encoded = transactionBuilder.encode(rawTransaction, signature)

        return syncer.single(MerkleSendRawTransactionJsonRpc(encoded, sourceTag))
            .doOnSuccess { txHash ->
                manager.save(MerkleTransactionHash(txHash))
            }
            .map { tx }
    }

    fun transaction(transactionHash: ByteArray): Single<Optional<RpcTransaction>> {
        return syncer.single(GetTransactionByHashJsonRpc(transactionHash))
            .map { Optional.of(it) }
            .onErrorResumeNext { throwable ->
                if (throwable is JsonRpc.ResponseError.InvalidResult) {
                    Single.just(Optional.empty())
                } else {
                    Single.error(throwable)
                }
            }
    }
}
