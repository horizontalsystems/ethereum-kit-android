package io.horizontalsystems.ethereumkit.sample.modules.main

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal
import java.net.URI
import java.util.logging.Logger

class MainViewModel : ViewModel() {
    private val logger = Logger.getLogger("MainViewModel")

    private val disposables = CompositeDisposable()

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

        ethereumAdapter.lastBlockHeightFlowable.subscribe {
            updateLastBlockHeight()
            updateEthTransactions()
        }.let {
            disposables.add(it)
        }

        ethereumAdapter.transactionsFlowable.subscribe {
            updateEthTransactions()
        }.let {
            disposables.add(it)
        }

        ethereumAdapter.balanceFlowable.subscribe {
            updateBalance()
        }.let {
            disposables.add(it)
        }

        ethereumAdapter.syncStateFlowable.subscribe {
            updateState()
        }.let {
            disposables.add(it)
        }

        ethereumAdapter.transactionsSyncStateFlowable.subscribe {
            updateTransactionsSyncState()
        }.let {
            disposables.add(it)
        }


        //
        // ERC20
        //

        erc20Adapter.transactionsFlowable.subscribe {
            updateErc20Transactions()
        }.let {
            disposables.add(it)
        }

        erc20Adapter.balanceFlowable.subscribe {
            updateErc20Balance()
        }.let {
            disposables.add(it)
        }

        erc20Adapter.syncStateFlowable.subscribe {
            updateErc20State()
        }.let {
            disposables.add(it)
        }

        erc20Adapter.transactionsSyncStateFlowable.subscribe {
            updateErc20TransactionsSyncState()
        }.let {
            disposables.add(it)
        }

        ethereumAdapter.start()
        erc20Adapter.start()

        gasPriceHelper = GasPriceHelper(Eip1559GasPriceProvider(ethereumKit))
        gasPriceHelper.gasPriceFlowable()
            .subscribe({
                gasPrice = it
                Log.e("AAA", "set gasPrice: $gasPrice")
            }, {
                Log.e(
                    "AAA",
                    "error: ${it.localizedMessage ?: it.message ?: it.javaClass.simpleName}"
                )
            }).let { disposables.add(it) }
    }

    private fun createKit(): EthereumKit {
        when (Configuration.chain) {
            Chain.BinanceSmartChain -> {
                transactionSource = TransactionSource.etherscanApi(Configuration.etherscanKey.split(","))
                rpcSource = RpcSource.binanceSmartChainHttp()
            }

            Chain.Ethereum -> {
                transactionSource = TransactionSource.etherscanApi(Configuration.etherscanKey.split(","))
                rpcSource = RpcSource.Http(listOf(URI(Configuration.ethereumRpc)), null)
            }

            Chain.ArbitrumOne -> {
                transactionSource = TransactionSource.etherscanApi(Configuration.etherscanKey.split(","))
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
        ethereumAdapter.transactions()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { list: List<TransactionRecord> ->
                ethTxs = list
                updateTransactionList()
            }.let {
                disposables.add(it)
            }
    }

    private fun updateErc20Transactions() {
        erc20Adapter.transactions()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { list: List<TransactionRecord> ->
                erc20Txs = list
                updateTransactionList()
            }.let {
                disposables.add(it)
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

        val estimateSingle = if (isErc20)
            erc20Adapter.estimatedGasLimit(Address(toAddress), value, gasPrice)
        else
            ethereumAdapter.estimatedGasLimit(Address(toAddress), value, gasPrice)

        estimateSingle
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                //success
                estimatedGas.value = it.toString()
            }, {
                logger.warning("Gas estimate: ${it.message}")
                estimatedGas.value = it.message
            })
            .let { disposables.add(it) }
    }

    fun send(toAddress: String, amount: BigDecimal) {
        val gasLimit = estimatedGas.value?.toLongOrNull() ?: kotlin.run {
            sendStatus.value = Exception("No gas limit!!")
            return
        }

        ethereumAdapter.send(Address(toAddress), amount, gasPrice, gasLimit)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ fullTransaction ->
                //success
                logger.info("Successfully sent, hash: ${fullTransaction.transaction.hash.toHexString()}")

                sendStatus.value = null
            }, {
                logger.warning("Ether send failed: ${it.message}")
                sendStatus.value = it
            }).let { disposables.add(it) }

    }

    //
    // ERC20
    //

    fun sendERC20(toAddress: String, amount: BigDecimal) {
        val gasLimit = estimatedGas.value?.toLongOrNull() ?: kotlin.run {
            sendStatus.value = Exception("No gas limit!!")
            return
        }

        erc20Adapter.send(Address(toAddress), amount, gasPrice, gasLimit)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ fullTransaction ->
                logger.info("Successfully sent, hash: ${fullTransaction.transaction.hash.toHexString()}")
                //success
                sendStatus.value = null
            }, {
                logger.warning("Erc20 send failed: ${it.message}")
                sendStatus.value = it
            }).let { disposables.add(it) }
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

        uniswapKit.swapData(rpcSource, chain, tokenIn, tokenOut)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                swapData.value = it
            }, {
                logger.warning("swapData ERROR = ${it.message}")
            }).let {
                disposables.add(it)
            }
    }

    fun syncAllowance() {
        erc20Adapter.allowance(uniswapKit.routerAddress(chain))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                logger.info("allowance: ${it.toPlainString()}")
            }, {
                logger.warning("swapData ERROR = ${it.message}")
            }).let {
                disposables.add(it)
            }
    }

    fun approve(decimalAmount: BigDecimal) {
        val spenderAddress = uniswapKit.routerAddress(chain)

        val token = fromToken ?: return
        val amount = decimalAmount.movePointRight(token.decimals).toBigInteger()

        val transactionData = erc20Adapter.approveTransactionData(spenderAddress, amount)

        ethereumKit.estimateGas(transactionData, gasPrice)
            .flatMap { gasLimit ->
                logger.info("gas limit: $gasLimit")
                ethereumKit.rawTransaction(transactionData, gasPrice, gasLimit)
            }
            .flatMap { rawTransaction ->
                val signature = signer.signature(rawTransaction)
                ethereumKit.send(rawTransaction, signature)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ fullTransaction ->
                logger.info("approve: ${fullTransaction.transaction.hash}")
            }, {
                logger.warning("approve ERROR = ${it.message}")
            }).let {
                disposables.add(it)
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
            ethereumKit.estimateGas(transactionData, gasPrice)
                .flatMap { gasLimit ->
                    logger.info("gas limit: $gasLimit")

                    val transactionData = uniswapKit.transactionData(ethereumKit.receiveAddress, chain, tradeData)
                    ethereumKit.rawTransaction(transactionData, gasPrice, gasLimit)
                }
                .flatMap { rawTransaction ->
                    val signature = signer.signature(rawTransaction)
                    ethereumKit.send(rawTransaction, signature)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ fullTransaction ->
                    swapStatus.value = null
                    logger.info("swap SUCCESS, txHash=${fullTransaction.transaction.hash.toHexString()}")
                }, {
                    swapStatus.value = it
                    logger.info("swap ERROR, error=${it.message}")
                    it.printStackTrace()
                }).let { disposables.add(it) }
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
