package io.horizontalsystems.ethereumkit.decorations

import io.horizontalsystems.ethereumkit.contracts.ContractMethodHelper
import io.horizontalsystems.ethereumkit.core.IDecorator
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullRpcTransaction
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.TransactionData
import java.math.BigInteger
import kotlin.reflect.KClass

class ContractCallDecorator(val address: Address) : IDecorator {

    private var methods = mutableMapOf<ByteArray, RecognizedContractMethod>()

    init {
        addMethod("Deposit", "deposit(uint256)", listOf(BigInteger::class))
        addMethod("TradeWithHintAndFee", "tradeWithHintAndFee(address,uint256,address,address,uint256,uint256,address,uint256,bytes)",
                listOf(Address::class, BigInteger::class, Address::class, Address::class, BigInteger::class, BigInteger::class, Address::class, BigInteger::class, ByteArray::class))
    }

    override fun decorate(transactionData: TransactionData): ContractMethodDecoration? {
        val methodId = transactionData.input.take(4).toByteArray()
        val inputArguments = transactionData.input.takeLast(4).toByteArray()

        val method = methods[methodId] ?: return null

        val arguments = ContractMethodHelper.decodeABI(inputArguments, method.arguments)

        return RecognizedMethodDecoration(method.name, arguments)
    }

    override fun decorate(fullTransaction: FullTransaction, fullRpcTransaction: FullRpcTransaction) {
        decorateMain(fullTransaction)
    }

    override fun decorateTransactions(fullTransactions: Map<String, FullTransaction>) {
        for (fullTransaction in fullTransactions.values) {
            decorateMain(fullTransaction)
        }
    }

    private fun decorateMain(fullTransaction: FullTransaction) {
        if (fullTransaction.transaction.from != address) return
        val transactionData = fullTransaction.transactionData ?: return
        val decoration = decorate(transactionData) ?: return

        fullTransaction.mainDecoration = decoration
    }

    private fun addMethod(name: String, signature: String, arguments: List<KClass<out Any>>) {
        val method = RecognizedContractMethod(name, signature, arguments, ContractMethodHelper.getMethodId(signature))
        methods[method.methodId] = method
    }

    class RecognizedContractMethod(
            val name: String,
            val signature: String,
            val arguments: List<KClass<out Any>>,
            val methodId: ByteArray
    )

}
