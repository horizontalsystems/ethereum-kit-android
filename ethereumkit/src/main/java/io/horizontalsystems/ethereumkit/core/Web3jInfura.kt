package io.horizontalsystems.ethereumkit.core

import io.reactivex.Flowable
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction
import org.web3j.protocol.core.methods.response.EthGetTransactionCount
import org.web3j.protocol.core.methods.response.EthSendTransaction
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Convert
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

class Web3jInfura(testMode: Boolean, private val infuraApiKey: String): IWeb3jInfura {

    private val infuraUrl = getInfuraUrl(testMode)
    private val web3j: Web3j = Web3j.build(HttpService(infuraUrl))

    override fun shutdown() {
        web3j.shutdown()
    }

    override fun sendRawTransaction(rawTransaction: String): EthSendTransaction {
        return web3j.ethSendRawTransaction(rawTransaction).send()
    }

    override fun getTransactionCount(address: String): EthGetTransactionCount {
        return web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).send()
    }

    override fun getGasPrice(): Flowable<BigDecimal> {
        return web3j.ethGasPrice()
                .flowable()
                .map {
                    Convert.fromWei(it.gasPrice.toBigDecimal(), Convert.Unit.GWEI)
                }
    }

    override fun getLastBlockHeight(): Flowable<Int> {
        return web3j.ethBlockNumber()
                .flowable()
                .map { it.blockNumber.toInt() }
    }

    override fun getBalance(address: String): Flowable<BigDecimal> {
        return web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST)
                .flowable()
                .map {
                    Convert.fromWei(it.balance.toBigDecimal(), Convert.Unit.ETHER)
                }
    }

    override fun getBalanceErc20(address: String, contractAddress: String, decimal: Int): Flowable<BigDecimal> {
        val function = Function("balanceOf",
                Arrays.asList<Type<*>>(Address(address)),
                Arrays.asList<TypeReference<*>>(object : TypeReference<Uint256>() {}))

        return web3j.ethCall(createEthCallTransaction(address, contractAddress, FunctionEncoder.encode(function)), DefaultBlockParameterName.LATEST)
                .flowable()
                .map {
                    val result = FunctionReturnDecoder.decode(it.value, function.outputParameters)
                    if (result.isEmpty()) {
                        0.toBigInteger()
                    } else {
                        result[0].value as BigInteger
                    }
                }
                .map {
                    it.toBigDecimal().divide(BigDecimal.TEN.pow(decimal))
                }
    }

    private fun getInfuraUrl(testMode: Boolean): String {
        val subDomain = when (testMode) {
            true -> "ropsten"
            false -> "mainnet"
        }

        return "https://$subDomain.infura.io/$infuraApiKey"
    }

}
