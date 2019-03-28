package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.network.Configuration
import io.horizontalsystems.ethereumkit.network.EtherscanService
import io.horizontalsystems.hdwalletkit.HDWallet
import io.reactivex.Single
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.http.HttpService
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

    override fun getLastBlockHeight(): Single<Long> {
        return web3j.ethBlockNumber()
                .flowable()
                .map { it.blockNumber.toLong() }
                .firstOrError()
    }

    override fun getTransactionCount(address: ByteArray): Single<Long> {
        return web3j.ethGetTransactionCount("0x${address.toHexString()}", DefaultBlockParameterName.LATEST)
                .flowable()
                .map { it.transactionCount.toLong() }
                .firstOrError()
    }

    override fun getBalance(address: ByteArray): Single<BigInteger> {
        return web3j.ethGetBalance("0x${address.toHexString()}", DefaultBlockParameterName.LATEST)
                .flowable()
                .map {
                    it.balance
                }
                .firstOrError()
    }

    override fun getBalanceErc20(address: ByteArray, contractAddress: ByteArray): Single<BigInteger> {
        val function = Function("balanceOf",
                Arrays.asList<Type<*>>(Address(address.toHexString())),
                Arrays.asList<TypeReference<*>>(object : TypeReference<Uint256>() {}))

        return web3j.ethCall(Transaction.createEthCallTransaction(address.toHexString(), contractAddress.toHexString(), FunctionEncoder.encode(function)), DefaultBlockParameterName.LATEST)
                .flowable()
                .map {
                    val result = FunctionReturnDecoder.decode(it.value, function.outputParameters)
                    if (result.isEmpty()) {
                        0.toBigInteger()
                    } else {
                        result[0].value as BigInteger
                    }
                }
                .map { it }
                .firstOrError()
    }

    override fun getTransactions(address: ByteArray, startBlock: Long): Single<List<EthereumTransaction>> {
        return etherscanService.getTransactionList(address.toHexString(), startBlock.toInt())
                .map { response -> response.result.distinctBy { it.blockHash }.map { etherscanTransaction -> EthereumTransaction(etherscanTransaction) } }
                .firstOrError()
    }

    override fun getTransactionsErc20(address: ByteArray, startBlock: Long): Single<List<EthereumTransaction>> {
        return etherscanService.getTokenTransactions(address.toHexString(), startBlock.toInt())
                .map { response -> response.result.distinctBy { it.blockHash }.map { etherscanTransaction -> EthereumTransaction(etherscanTransaction) } }
                .firstOrError()
    }

    override fun send(signedTranaction: ByteArray): Single<Unit> {
        TODO("not implemented")
    }

    //    override fun send(fromAddress: String, toAddress: String, nonce: Int, amount: String, gasPrice: Long, gasLimit: Long): Single<EthereumTransaction> {
//        val rawTransaction = RawTransaction.createEtherTransaction(nonce.toBigInteger(), gasPrice.toBigInteger(), gasLimit.toBigInteger(), toAddress, amount.toBigInteger())
//
//        //  sign & send our transaction
//        val signedMessage = TransactionEncoder.signMessage(rawTransaction, hdWallet.credentials())
//        val hexValue = Numeric.toHexString(signedMessage)
//
//        return web3j.ethSendRawTransaction(hexValue)
//                .flowable()
//                .flatMap {
//                    if (it.hasError()) {
//                        Flowable.error(Throwable(it.error.message))
//                    } else {
//                        val pendingTx = EthereumTransaction().apply {
//                            hash = it.transactionHash
//                            timestamp = System.currentTimeMillis() / 1000
//                            from = fromAddress
//                            to = toAddress
//                            value = amount
//                            input = "0x"
//                        }
//                        Flowable.just(pendingTx)
//                    }
//                }
//                .firstOrError()
//    }

//    override fun sendErc20(contractAddress: String, fromAddress: String, toAddress: String, nonce: Int, amount: String, gasPrice: Long, gasLimit: Long): Single<EthereumTransaction> {
//        val transferFN = Function("transfer",
//                Arrays.asList<Type<*>>(Address(toAddress), Uint256(amount.toBigInteger())),
//                Arrays.asList<TypeReference<*>>(object : TypeReference<Uint256>() {}))
//
//        val encodeData = FunctionEncoder.encode(transferFN)
//        val rawTransaction = RawTransaction.createTransaction(nonce.toBigInteger(), gasPrice.toBigInteger(), gasLimit.toBigInteger(), contractAddress, encodeData)
//
//        //  sign & send our transaction
//        val signedMessage = TransactionEncoder.signMessage(rawTransaction, hdWallet.credentials())
//        val hexValue = Numeric.toHexString(signedMessage)
//
//        return web3j.ethSendRawTransaction(hexValue)
//                .flowable()
//                .flatMap {
//                    if (it.hasError()) {
//                        Flowable.error(Throwable(it.error.message))
//                    } else {
//                        val data = Numeric.prependHexPrefix(rawTransaction.data)
//                        val pendingTx = EthereumTransaction().apply {
//                            hash = it.transactionHash
//                            timestamp = System.currentTimeMillis() / 1000
//                            from = fromAddress
//                            to = toAddress
//                            value = amount
//                            input = data
//                            this.contractAddress = contractAddress
//                        }
//                        Flowable.just(pendingTx)
//                    }
//                }
//                .firstOrError()
//    }

}
