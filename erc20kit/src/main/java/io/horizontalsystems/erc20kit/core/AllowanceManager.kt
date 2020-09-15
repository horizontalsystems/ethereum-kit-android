package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.contract.ApproveMethod
import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.contracts.ContractMethod.Argument.AddressArgument
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.Single
import java.math.BigInteger

class AllowanceManager(
        private val ethereumKit: EthereumKit,
        private val contractAddress: Address,
        private val address: Address,
        private val storage: ITransactionStorage
) {

    fun allowance(spenderAddress: Address): Single<BigInteger> {
        val method = ContractMethod("allowance", listOf(AddressArgument(address), AddressArgument(spenderAddress)))
        return ethereumKit.call(contractAddress, method.encodedABI()).map { result ->
            BigInteger(result.sliceArray(0..31))
        }
    }

    fun estimateApprove(spenderAddress: Address, amount: BigInteger, gasPrice: Long): Single<Long> {
        return ethereumKit.estimateGas(contractAddress, null, gasPrice, ApproveMethod(spenderAddress, amount).encodedABI())
    }

    fun approve(spenderAddress: Address, amount: BigInteger, gasPrice: Long, gasLimit: Long): Single<Transaction> {
        val approveMethod = ApproveMethod(spenderAddress, amount)

        return ethereumKit.send(contractAddress, BigInteger.ZERO, approveMethod.encodedABI(), gasPrice, gasLimit)
                .map { transactionWithInternal ->
                    approveMethod.getErc20Transactions(transactionWithInternal.transaction).first()
                }.doOnSuccess { transaction ->
                    storage.save(listOf(transaction))
                }

    }
}
