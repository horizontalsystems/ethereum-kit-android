package io.horizontalsystems.ethereumkit.core

class ERC20(val contractAddress: String, val decimal: Int) {

//    fun getBalance(fromAddress: String): Flowable<Double> {
//        val function = Function("balanceOf",
//                Arrays.asList<Type<*>>(Address(fromAddress)),
//                Arrays.asList<TypeReference<*>>(object : TypeReference<Uint256>() {}))
//
//
//        val transaction = Transaction.createEthCallTransaction(fromAddress, contractAddress, FunctionEncoder.encode(function))
//
//        return web3j.ethCall(transaction, DefaultBlockParameterName.LATEST)
//                .flowable()
//                .map {
//                    val result = FunctionReturnDecoder.decode(it.value, function.outputParameters)
//                    result[0].value as BigInteger
//                }
//                .map {
//                    Convert.fromWei(it.toBigDecimal(), Convert.Unit.ETHER).toDouble()
//                }
//    }
//
//    fun getGasLimit(fromAddress: String, contractAddress: String): Flowable<Double> {
//        val transaction = makeTransaction(fromAddress, contractAddress, null, null, null, value = 0.toBigInteger())
//
//        return web3j.ethEstimateGas(transaction)
//                .flowable()
//                .map {
//                    it.amountUsed ?: BigInteger.ZERO
//                }
//                .map {
//                    it.toDouble()
//                }
//    }
//
//    private fun makeTransaction(fromAddress: String, toAddress: String, nonce: BigInteger?, gasPrice: BigInteger?, gasLimit: BigInteger?, value: BigInteger): Transaction {
//        return Transaction.createEtherTransaction(fromAddress, nonce, gasPrice, gasLimit, toAddress, value)
//    }
}

