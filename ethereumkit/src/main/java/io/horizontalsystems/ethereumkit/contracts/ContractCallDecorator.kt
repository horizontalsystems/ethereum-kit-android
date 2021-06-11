package io.horizontalsystems.ethereumkit.contracts

import io.horizontalsystems.ethereumkit.core.IDecorator
import io.horizontalsystems.ethereumkit.decorations.EventDecoration
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.ethereumkit.models.TransactionLog
import java.math.BigInteger
import kotlin.reflect.KClass

class ContractCallDecorator : IDecorator {

    private var methods = mutableMapOf<ByteArray, RecognizedContractMethod>()

    init {
        addMethod("deposit", "deposit(uint256)", listOf(BigInteger::class))
        addMethod("tradeWithHintAndFee", "tradeWithHintAndFee(address,uint256,address,address,uint256,uint256,address,uint256,bytes)",
                listOf(Address::class, BigInteger::class, Address::class, Address::class, BigInteger::class, BigInteger::class, Address::class, BigInteger::class, ByteArray::class))
    }

    override fun decorate(transactionData: TransactionData, fullTransaction: FullTransaction?): TransactionDecoration? {
        val methodId = transactionData.input.take(4).toByteArray()
        val inputArguments = transactionData.input.takeLast(4).toByteArray()

        val method = methods[methodId] ?: return null

        val arguments = ContractMethodHelper.decodeABI(inputArguments, method.arguments)

        return TransactionDecoration.Recognized(method.name, arguments)
    }

    override fun decorate(logs: List<TransactionLog>): List<EventDecoration> {
        return emptyList()
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
