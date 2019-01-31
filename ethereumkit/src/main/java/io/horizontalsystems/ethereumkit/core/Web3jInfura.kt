package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.EthereumKit
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

class Web3jInfura(networkType: EthereumKit.NetworkType, private val infuraApiKey: String) {

    private val infuraUrl = getInfuraUrl(networkType)
    private val web3j: Web3j = Web3j.build(HttpService(infuraUrl))

    fun shutdown() {
        web3j.shutdown()
    }

    fun sendRawTransaction(raw: String): EthSendTransaction {
        return web3j.ethSendRawTransaction(raw).send()
    }

    fun getTransactionCount(address: String): EthGetTransactionCount {
        return web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).send()
    }

    fun getGasPrice(): Flowable<Double> {
        return web3j.ethGasPrice()
                .flowable()
                .map {
                    Convert.fromWei(it.gasPrice.toBigDecimal(), Convert.Unit.GWEI).toDouble()
                }
    }

    fun getBlockNumber(): Flowable<Int> {
        return web3j.ethBlockNumber()
                .flowable()
                .map { it.blockNumber.toInt() }
    }

    fun getBalance(address: String): Flowable<Double> {
        return web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST)
                .flowable()
                .map {
                    Convert.fromWei(it.balance.toBigDecimal(), Convert.Unit.ETHER).toDouble()
                }
    }

    fun getTokenBalance(address: String, contractAddress: String, decimal: Int): Flowable<Double> {
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
                    it.toBigDecimal().divide(BigDecimal.TEN.pow(decimal)).toDouble()
                }
    }

    private fun getInfuraUrl(network: EthereumKit.NetworkType): String {
        val subDomain = when (network) {
            EthereumKit.NetworkType.MainNet -> "mainnet"
            EthereumKit.NetworkType.Kovan -> "kovan"
            EthereumKit.NetworkType.Rinkeby -> "rinkeby"
            EthereumKit.NetworkType.Ropsten -> "ropsten"
        }

        return "https://$subDomain.infura.io/$infuraApiKey"
    }

}
