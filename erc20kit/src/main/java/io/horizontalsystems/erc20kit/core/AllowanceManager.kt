package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.contract.AllowanceMethod
import io.horizontalsystems.erc20kit.contract.ApproveMethod
import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.reactivex.Single
import java.math.BigInteger

class AllowanceManager(
        private val ethereumKit: EthereumKit,
        private val contractAddress: Address,
        private val address: Address,
        private val storage: ITransactionStorage
) {

    fun allowance(spenderAddress: Address, defaultBlockParameter: DefaultBlockParameter): Single<BigInteger> {
        return ethereumKit
                .call(contractAddress, AllowanceMethod(address, spenderAddress).encodedABI(), defaultBlockParameter)
                .map { result ->
                    BigInteger(result.sliceArray(0..31))
                }
    }

    fun approveTransactionData(spenderAddress: Address, amount: BigInteger): TransactionData {
        return TransactionData(contractAddress, BigInteger.ZERO, ApproveMethod(spenderAddress, amount).encodedABI())
    }

    fun approve(spenderAddress: Address, amount: BigInteger, gasPrice: Long, gasLimit: Long): Single<Transaction> {
        val approveMethod = ApproveMethod(spenderAddress, amount)

        return ethereumKit.send(contractAddress, BigInteger.ZERO, approveMethod.encodedABI(), gasPrice, gasLimit)
                .map { transaction ->
                    approveMethod.getErc20Transactions(transaction).first()
                }.doOnSuccess { transaction ->
                    storage.save(listOf(transaction))
                }

    }

}
