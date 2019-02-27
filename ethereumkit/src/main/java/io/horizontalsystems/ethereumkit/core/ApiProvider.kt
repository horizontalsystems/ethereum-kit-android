package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.network.Configuration
import io.horizontalsystems.ethereumkit.network.EtherscanService
import io.horizontalsystems.hdwalletkit.HDWallet
import io.reactivex.Flowable
import io.reactivex.Single
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.util.*

class ApiProvider(configuration: Configuration, private val hdWallet: HDWallet) : IApiProvider {

    private val web3j: Web3j = Web3j.build(HttpService(configuration.infuraUrl))
    private val etherscanService = EtherscanService(configuration.etherScanUrl, configuration.etherscanAPIKey)

    override fun getGasPriceInWei(): Single<Long> {
        return web3j.ethGasPrice()
                .flowable()
                .map {
                    it.gasPrice.toLong()
                }
                .firstOrError()
    }

    override fun getLastBlockHeight(): Single<Int> {
        return web3j.ethBlockNumber()
                .flowable()
                .map { it.blockNumber.toInt() }
                .firstOrError()
    }

    override fun getTransactionCount(address: String): Single<Int> {
        return web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST)
                .flowable()
                .map { it.transactionCount.toInt() }
                .firstOrError()
    }

    override fun getBalance(address: String): Single<String> {
        return web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST)
                .flowable()
                .map {
                    it.balance.toString()
                }
                .firstOrError()
    }

    override fun getBalanceErc20(address: String, contractAddress: String): Single<String> {
        val function = Function("balanceOf",
                Arrays.asList<Type<*>>(Address(address)),
                Arrays.asList<TypeReference<*>>(object : TypeReference<Uint256>() {}))

        return web3j.ethCall(Transaction.createEthCallTransaction(address, contractAddress, FunctionEncoder.encode(function)), DefaultBlockParameterName.LATEST)
                .flowable()
                .map {
                    val result = FunctionReturnDecoder.decode(it.value, function.outputParameters)
                    if (result.isEmpty()) {
                        0.toBigInteger()
                    } else {
                        result[0].value as BigInteger
                    }
                }
                .map{ it.toString() }
                .firstOrError()
    }

    override fun getTransactions(address: String, startBlock: Int): Single<List<EthereumTransaction>> {
        return etherscanService.getTransactionList(address, startBlock)
                .map { it.result.map { etherscanTransaction -> EthereumTransaction(etherscanTransaction) } }
                .firstOrError()
    }

    override fun getTransactionsErc20(address: String, startBlock: Int): Single<List<EthereumTransaction>> {
        return etherscanService.getTokenTransactions(address, startBlock)
                .map { it.result.map { etherscanTransaction -> EthereumTransaction(etherscanTransaction) } }
                .firstOrError()
    }

    override fun send(fromAddress: String, toAddress: String, nonce: Int, amount: String, gasPriceInWei: Long, gasLimit: Int): Single<EthereumTransaction> {
        val rawTransaction = RawTransaction.createEtherTransaction(nonce.toBigInteger(), gasPriceInWei.toBigInteger(), gasLimit.toBigInteger(), toAddress, amount.toBigInteger())

        //  sign & send our transaction
        val signedMessage = TransactionEncoder.signMessage(rawTransaction, hdWallet.credentials())
        val hexValue = Numeric.toHexString(signedMessage)

        return web3j.ethSendRawTransaction(hexValue)
                .flowable()
                .flatMap {
                    if (it.hasError()) {
                        Flowable.error(Throwable(it.error.message))
                    } else {
                        val pendingTx = EthereumTransaction().apply {
                            hash = it.transactionHash
                            timeStamp = System.currentTimeMillis() / 1000
                            from = fromAddress
                            to = toAddress
                            value = amount
                            input = "0x"
                        }
                        Flowable.just(pendingTx)
                    }
                }
                .firstOrError()
    }

    override fun sendErc20(contractAddress: String, fromAddress: String, toAddress: String, nonce: Int, amount: String, gasPriceInWei: Long, gasLimit: Int): Single<EthereumTransaction> {
        val transferFN = Function("transfer",
                Arrays.asList<Type<*>>(Address(toAddress), Uint256(amount.toBigInteger())),
                Arrays.asList<TypeReference<*>>(object : TypeReference<Uint256>() {}))

        val encodeData = FunctionEncoder.encode(transferFN)
        val rawTransaction = RawTransaction.createTransaction(nonce.toBigInteger(), gasPriceInWei.toBigInteger(), gasLimit.toBigInteger(), contractAddress, encodeData)

        //  sign & send our transaction
        val signedMessage = TransactionEncoder.signMessage(rawTransaction, hdWallet.credentials())
        val hexValue = Numeric.toHexString(signedMessage)

        return web3j.ethSendRawTransaction(hexValue)
                .flowable()
                .flatMap {
                    if (it.hasError()) {
                        Flowable.error(Throwable(it.error.message))
                    } else {
                        val data = Numeric.prependHexPrefix(rawTransaction.data)
                        val pendingTx = EthereumTransaction().apply {
                            hash = it.transactionHash
                            timeStamp = System.currentTimeMillis() / 1000
                            from = fromAddress
                            to = toAddress
                            value = amount
                            input = data
                            this.contractAddress = contractAddress
                        }
                        Flowable.just(pendingTx)
                    }
                }
                .firstOrError()
    }

}
