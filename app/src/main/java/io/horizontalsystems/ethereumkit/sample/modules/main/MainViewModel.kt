package io.horizontalsystems.ethereumkit.sample.modules.main

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.erc20kit.core.Erc20Kit
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncState
import io.horizontalsystems.ethereumkit.core.eip1559.Eip1559GasPriceProvider
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.ethereumkit.models.TransactionSource
import io.horizontalsystems.ethereumkit.sample.App
import io.horizontalsystems.ethereumkit.sample.Configuration
import io.horizontalsystems.ethereumkit.sample.SingleLiveEvent
import io.horizontalsystems.ethereumkit.sample.core.Erc20Adapter
import io.horizontalsystems.ethereumkit.sample.core.EthereumAdapter
import io.horizontalsystems.ethereumkit.sample.core.TransactionRecord
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.oneinchkit.OneInchKit
import io.horizontalsystems.uniswapkit.UniswapKit
import io.horizontalsystems.uniswapkit.UniswapV3Kit
import io.horizontalsystems.uniswapkit.models.SwapData
import io.horizontalsystems.uniswapkit.models.Token
import io.horizontalsystems.uniswapkit.models.TradeData
import io.horizontalsystems.uniswapkit.models.TradeOptions
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.net.URI
import java.util.logging.Logger

class MainViewModel : ViewModel() {
    private val logger = Logger.getLogger("MainViewModel")

    lateinit var ethereumKit: EthereumKit
    lateinit var ethereumAdapter: EthereumAdapter
    lateinit var signer: Signer
    lateinit var rpcSource: RpcSource
    private lateinit var transactionSource: TransactionSource

    lateinit var erc20Adapter: Erc20Adapter

    val transactions = MutableLiveData<List<TransactionRecord>>()
    val balance = MutableLiveData<BigDecimal>()
    val lastBlockHeight = MutableLiveData<Long>()
    val syncState = MutableLiveData<SyncState>()
    val transactionsSyncState = MutableLiveData<SyncState>()
    val erc20SyncState = MutableLiveData<SyncState>()
    val erc20TransactionsSyncState = MutableLiveData<SyncState>()

    val erc20TokenBalance = MutableLiveData<BigDecimal>()
    val sendStatus = SingleLiveEvent<Throwable?>()
    val estimatedGas = SingleLiveEvent<String>()
    val showTxTypeLiveData = MutableLiveData<ShowTxType>()

    private var showTxType = ShowTxType.Eth

    private val recommendedPriorityFee: Long? = null
    private var gasPrice: GasPrice = GasPrice.Legacy(20_000_000_000)

    private var ethTxs = listOf<TransactionRecord>()
    private var erc20Txs = listOf<TransactionRecord>()

    private lateinit var uniswapKit: UniswapKit
    private val tradeOptions = TradeOptions(allowedSlippagePercent = BigDecimal("0.5"))
    var swapData = MutableLiveData<SwapData?>()
    var tradeData = MutableLiveData<TradeData?>()
    val swapStatus = SingleLiveEvent<Throwable?>()

    val fromToken: Erc20Token = Configuration.erc20Tokens[0]
    val toToken: Erc20Token = Configuration.erc20Tokens[1]
    lateinit var gasPriceHelper: GasPriceHelper

    private val chain: Chain
        get() = ethereumKit.chain

    fun init() {
        val words = Configuration.defaultsWords.split(" ")
        val seed = Mnemonic().toSeed(words)
        signer = Signer.getInstance(seed, Configuration.chain)
        ethereumKit = createKit()
        ethereumAdapter = EthereumAdapter(ethereumKit, signer)
        erc20Adapter = Erc20Adapter(
            App.instance, fromToken ?: toToken
            ?: Configuration.erc20Tokens.first(), ethereumKit, signer
        )
        uniswapKit = UniswapKit.getInstance()

        Erc20Kit.addTransactionSyncer(ethereumKit)
        Erc20Kit.addDecorators(ethereumKit)
        UniswapKit.addDecorators(ethereumKit)
        UniswapV3Kit.addDecorators(ethereumKit)
        OneInchKit.addDecorators(ethereumKit)

        updateBalance()
        updateErc20Balance()
        updateState()
        updateTransactionsSyncState()
        updateErc20State()
        updateErc20TransactionsSyncState()
        updateLastBlockHeight()

        filterTransactions(true)

        //
        // Ethereum
        //

        ethereumAdapter.lastBlockHeightFlow.onEach {
            updateLastBlockHeight()
            updateEthTransactions()
        }.launchIn(viewModelScope)

        ethereumAdapter.transactionsFlow.onEach {
            updateEthTransactions()
        }.launchIn(viewModelScope)

        ethereumAdapter.balanceFlow.onEach {
            updateBalance()
        }.launchIn(viewModelScope)

        ethereumAdapter.syncStateFlow.onEach {
            updateState()
        }.launchIn(viewModelScope)

        ethereumAdapter.transactionsSyncStateFlow.onEach {
            updateTransactionsSyncState()
        }.launchIn(viewModelScope)


        //
        // ERC20
        //

        erc20Adapter.transactionsFlow.onEach {
            updateErc20Transactions()
        }.launchIn(viewModelScope)

        erc20Adapter.balanceFlow.onEach {
            updateErc20Balance()
        }.launchIn(viewModelScope)

        erc20Adapter.syncStateFlow.onEach {
            updateErc20State()
        }.launchIn(viewModelScope)

        erc20Adapter.transactionsSyncStateFlow.onEach {
            updateErc20TransactionsSyncState()
        }.launchIn(viewModelScope)

        ethereumAdapter.start()
        erc20Adapter.start()

        gasPriceHelper = GasPriceHelper(Eip1559GasPriceProvider(ethereumKit))
        gasPriceHelper.gasPriceFlow()
            .onEach {
                gasPrice = it
                Log.e("AAA", "set gasPrice: $gasPrice")
            }
            .catch {
                Log.e(
                    "AAA",
                    "error: ${it.localizedMessage ?: it.message ?: it.javaClass.simpleName}"
                )
            }
            .launchIn(viewModelScope)
    }

    private fun createKit(): EthereumKit {
        when (Configuration.chain) {
            Chain.BinanceSmartChain -> {
                transactionSource = TransactionSource.binance(Configuration.etherscanKey.split(","))
                rpcSource = RpcSource.binanceSmartChainHttp()
            }

            Chain.Ethereum -> {
                transactionSource = TransactionSource.ethereum(Configuration.etherscanKey.split(","))
                rpcSource = RpcSource.Http(listOf(URI(Configuration.ethereumRpc)), null)
            }

            Chain.ArbitrumOne -> {
                transactionSource = TransactionSource.arbitrumOne(Configuration.etherscanKey.split(","))
                rpcSource = RpcSource.arbitrumOneRpcHttp()
            }

            else -> {
                throw Exception("Could not get rpcSource & transactionSource!")
            }
        }

        return if (Configuration.watchAddress != null) {
            EthereumKit.getInstance(
                App.instance, Address(Configuration.watchAddress),
                Configuration.chain, rpcSource, transactionSource,
                Configuration.walletId
            )
        } else {
            val words = Configuration.defaultsWords.split(" ")
            EthereumKit.getInstance(
                App.instance, words, "",
                Configuration.chain, rpcSource, transactionSource,
                Configuration.walletId
            )
        }
    }

    private fun updateLastBlockHeight() {
        lastBlockHeight.postValue(ethereumKit.lastBlockHeight)
    }

    private fun updateState() {
        syncState.postValue(ethereumAdapter.syncState)
    }

    private fun updateTransactionsSyncState() {
        transactionsSyncState.postValue(ethereumAdapter.transactionsSyncState)
    }

    private fun updateErc20State() {
        erc20SyncState.postValue(erc20Adapter.syncState)
    }

    private fun updateErc20TransactionsSyncState() {
        erc20TransactionsSyncState.postValue(erc20Adapter.transactionsSyncState)
    }

    private fun updateBalance() {
        balance.postValue(ethereumAdapter.balance)
    }

    private fun updateErc20Balance() {
        erc20TokenBalance.postValue(erc20Adapter.balance)
    }

    private fun updateEthTransactions() {
        viewModelScope.launch {
            try {
                ethTxs = ethereumAdapter.transactions()
                updateTransactionList()
            } catch (error: Throwable) {
                logger.warning("Eth transactions: ${error.message}")
            }
        }
    }

    private fun updateErc20Transactions() {
        viewModelScope.launch {
            try {
                erc20Txs = erc20Adapter.transactions()
                updateTransactionList()
            } catch (error: Throwable) {
                logger.warning("Erc20 transactions: ${error.message}")
            }
        }
    }

    private fun updateTransactionList() {
        val list = when (showTxType) {
            ShowTxType.Eth -> ethTxs
            ShowTxType.Erc20 -> erc20Txs
        }
        transactions.value = list
    }


    //
    // Ethereum
    //

    fun refresh() {
        ethereumAdapter.refresh()
        erc20Adapter.refresh()
    }

    fun clear() {
        EthereumKit.clear(App.instance, Configuration.chain, Configuration.walletId)
        Erc20Kit.clear(App.instance, Configuration.chain, Configuration.walletId)
        init()
    }

    fun receiveAddress(): String {
        return ethereumKit.receiveAddress.hex
    }

    fun estimateGas(toAddress: String?, value: BigDecimal, isErc20: Boolean) {
        estimatedGas.postValue(null)

        if (toAddress == null) return

        viewModelScope.launch {
            try {
                val gasLimit = if (isErc20)
                    erc20Adapter.estimatedGasLimit(Address(toAddress), value, gasPrice)
                else
                    ethereumAdapter.estimatedGasLimit(Address(toAddress), value, gasPrice)

                //success
                estimatedGas.value = gasLimit.toString()
            } catch (error: Throwable) {
                logger.warning("Gas estimate: ${error.message}")
                estimatedGas.value = error.message
            }
        }
    }

    fun send(toAddress: String, amount: BigDecimal) {
        val gasLimit = estimatedGas.value?.toLongOrNull() ?: kotlin.run {
            sendStatus.value = Exception("No gas limit!!")
            return
        }

        viewModelScope.launch {
            try {
                val fullTransaction = ethereumAdapter.send(Address(toAddress), amount, gasPrice, gasLimit)
                //success
                logger.info("Successfully sent, hash: ${fullTransaction.transaction.hash.toHexString()}")

                sendStatus.value = null
            } catch (error: Throwable) {
                logger.warning("Ether send failed: ${error.message}")
                sendStatus.value = error
            }
        }

    }

    //
    // ERC20
    //

    fun sendERC20(toAddress: String, amount: BigDecimal) {
        val gasLimit = estimatedGas.value?.toLongOrNull() ?: kotlin.run {
            sendStatus.value = Exception("No gas limit!!")
            return
        }

        viewModelScope.launch {
            try {
                val fullTransaction = erc20Adapter.send(Address(toAddress), amount, gasPrice, gasLimit)
                logger.info("Successfully sent, hash: ${fullTransaction.transaction.hash.toHexString()}")
                //success
                sendStatus.value = null
            } catch (error: Throwable) {
                logger.warning("Erc20 send failed: ${error.message}")
                sendStatus.value = error
            }
        }
    }

    fun filterTransactions(ethTx: Boolean) {
        showTxType = if (ethTx) {
            updateEthTransactions()
            ShowTxType.Eth
        } else {
            updateErc20Transactions()
            ShowTxType.Erc20
        }
        showTxTypeLiveData.postValue(showTxType)
    }

    //
    // SWAP
    //


    fun syncSwapData() {
        val tokenIn = uniswapToken(fromToken)
        val tokenOut = uniswapToken(toToken)

        viewModelScope.launch {
            try {
                swapData.value = uniswapKit.swapData(rpcSource, chain, tokenIn, tokenOut)
            } catch (error: Throwable) {
                logger.warning("swapData ERROR = ${error.message}")
            }
        }
    }

    fun syncAllowance() {
        viewModelScope.launch {
            try {
                val allowance = erc20Adapter.allowance(uniswapKit.routerAddress(chain))
                logger.info("allowance: ${allowance.toPlainString()}")
            } catch (error: Throwable) {
                logger.warning("swapData ERROR = ${error.message}")
            }
        }
    }

    fun approve(decimalAmount: BigDecimal) {
        val spenderAddress = uniswapKit.routerAddress(chain)

        val token = fromToken ?: return
        val amount = decimalAmount.movePointRight(token.decimals).toBigInteger()

        val transactionData = erc20Adapter.approveTransactionData(spenderAddress, amount)

        viewModelScope.launch {
            try {
                val gasLimit = ethereumKit.estimateGas(transactionData, gasPrice)
                logger.info("gas limit: $gasLimit")
                val rawTransaction = ethereumKit.rawTransaction(transactionData, gasPrice, gasLimit)
                val signature = signer.signature(rawTransaction)
                val fullTransaction = ethereumKit.send(rawTransaction, signature)
                logger.info("approve: ${fullTransaction.transaction.hash}")
            } catch (error: Throwable) {
                logger.warning("approve ERROR = ${error.message}")
            }
        }
    }

    private fun uniswapToken(token: Erc20Token?): Token {
        if (token == null)
            return uniswapKit.etherToken(chain)

        return uniswapKit.token(token.contractAddress, token.decimals)
    }


    fun onChangeAmountIn(amountIn: BigDecimal) {
        swapData.value?.let {
            tradeData.value = try {
                uniswapKit.bestTradeExactIn(it, amountIn, tradeOptions)
            } catch (error: Throwable) {
                logger.info("bestTradeExactIn error: ${error.javaClass.simpleName} (${error.localizedMessage})")
                null
            }
        }
    }

    fun onChangeAmountOut(amountOut: BigDecimal) {
        swapData.value?.let {
            tradeData.value = try {
                uniswapKit.bestTradeExactOut(it, amountOut, tradeOptions)
            } catch (error: Throwable) {
                logger.info("bestTradeExactOut error: ${error.javaClass.simpleName} (${error.localizedMessage})")
                null
            }
        }
    }


    fun swap() {
        tradeData.value?.let { tradeData ->

            val transactionData = uniswapKit.transactionData(ethereumKit.receiveAddress, chain, tradeData)
            viewModelScope.launch {
                try {
                    val gasLimit = ethereumKit.estimateGas(transactionData, gasPrice)
                    logger.info("gas limit: $gasLimit")

                    val transactionData = uniswapKit.transactionData(ethereumKit.receiveAddress, chain, tradeData)
                    val rawTransaction = ethereumKit.rawTransaction(transactionData, gasPrice, gasLimit)
                    val signature = signer.signature(rawTransaction)
                    val fullTransaction = ethereumKit.send(rawTransaction, signature)

                    swapStatus.value = null
                    logger.info("swap SUCCESS, txHash=${fullTransaction.transaction.hash.toHexString()}")
                } catch (error: Throwable) {
                    swapStatus.value = error
                    logger.info("swap ERROR, error=${error.message}")
                    error.printStackTrace()
                }
            }
        }
    }

}

enum class ShowTxType {
    Eth, Erc20
}

data class Erc20Token(
    val name: String,
    val code: String,
    val contractAddress: Address,
    val decimals: Int
)
