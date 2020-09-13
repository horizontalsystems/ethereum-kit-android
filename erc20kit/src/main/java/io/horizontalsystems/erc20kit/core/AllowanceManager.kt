package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.contracts.ContractMethod.Argument.AddressArgument
import io.horizontalsystems.ethereumkit.contracts.ContractMethod.Argument.Uint256Argument
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
        return ethereumKit.estimateGas(contractAddress, null, gasPrice, approveMethod(spenderAddress, amount).encodedABI())
    }

    fun approve(spenderAddress: Address, amount: BigInteger, gasPrice: Long, gasLimit: Long): Single<Transaction> {
        return ethereumKit.send(contractAddress, BigInteger.ZERO, approveMethod(spenderAddress, amount).encodedABI(), gasPrice, gasLimit)
                .map { transactionWithInternal ->
                    Transaction(transactionHash = transactionWithInternal.transaction.hash,
                            from = address,
                            to = spenderAddress,
                            value = amount,
                            type = Transaction.TransactionType.APPROVE)
                }.doOnSuccess { transaction ->
                    storage.save(listOf(transaction))
                }

    }

    private fun approveMethod(spenderAddress: Address, amount: BigInteger): ContractMethod {
        return ContractMethod("approve", listOf(AddressArgument(spenderAddress), Uint256Argument(amount)))
    }

}
