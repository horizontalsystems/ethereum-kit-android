package io.horizontalsystems.ethereumkit.sample

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.erc20kit.core.Erc20Kit
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.EthereumKit.NetworkType
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncState
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.sample.core.Erc20Adapter
import io.horizontalsystems.ethereumkit.sample.core.EthereumAdapter
import io.horizontalsystems.ethereumkit.sample.core.TransactionRecord
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.uniswapkit.UniswapKit
import io.horizontalsystems.uniswapkit.models.SwapData
import io.horizontalsystems.uniswapkit.models.Token
import io.horizontalsystems.uniswapkit.models.TradeData
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal
import java.util.logging.Logger

class MainViewModel : ViewModel() {
    private val logger = Logger.getLogger("MainViewModel")

    private val infuraProjectId = "2a1306f1d12f4c109a4d4fb9be46b02e"
    private val infuraSecret = "fc479a9290b64a84a15fa6544a130218"
    private val etherscanKey = "GKNHXT22ED7PRVCKZATFZQD1YI7FK9AAYE"
    private val bscScanKey = "5ZGSHWYHZVA8XZHB8PF6UUTRNNB4KT43ZZ"
    private val walletId = "walletId"
    private val networkType: NetworkType = NetworkType.BscMainNet
    private val webSocket = true
    private val words = "mom year father track attend frown loyal goddess crisp abandon juice roof".split(" ")

    private val disposables = CompositeDisposable()

    private lateinit var ethereumKit: EthereumKit
    private lateinit var ethereumAdapter: EthereumAdapter

    private lateinit var erc20Adapter: Erc20Adapter

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

    private val gasPrice: Long = 20_000_000_000

    private lateinit var uniswapKit: UniswapKit
    private val tradeOptions = TradeOptions(allowedSlippagePercent = BigDecimal("0.5"))
    var swapData = MutableLiveData<SwapData?>()
    var tradeData = MutableLiveData<TradeData?>()
    val swapStatus = SingleLiveEvent<Throwable?>()

    val tokens = listOf(
            Erc20Token("PancakeSwap", "CAKE", Address("0x0e09fabb73bd3ade0a17ecc321fd13a19e81ce82"), 18), //BEP20
            Erc20Token("Beefy.Finance", "BIFI", Address("0xCa3F508B8e4Dd382eE878A314789373D80A5190A"), 18), //BEP20

            Erc20Token("DAI", "DAI", Address("0xad6d458402f60fd3bd25163575031acdce07538d"), 18),
            Erc20Token("GMO coins", "GMOLW", Address("0xbb74a24d83470f64d5f0c01688fbb49a5a251b32"), 18),
            Erc20Token("USDT", "USDT", Address("0xdAC17F958D2ee523a2206206994597C13D831ec7"), 6),
            Erc20Token("DAI-MAINNET", "DAI", Address("0x6b175474e89094c44da98b954eedeac495271d0f"), 18)
    )
    val fromToken: Erc20Token? = tokens[0]
    val toToken: Erc20Token? = tokens[1]

    fun init() {
        ethereumKit = createKit()
        ethereumAdapter = EthereumAdapter(ethereumKit)

        erc20Adapter = Erc20Adapter(App.instance, fromToken ?: toToken ?: tokens.first(), ethereumKit)

        uniswapKit = UniswapKit.getInstance(ethereumKit)

        updateBalance()
        updateErc20Balance()
        updateState()
        updateTransactionsSyncState()
        updateErc20State()
        updateErc20TransactionsSyncState()
        updateLastBlockHeight()

        //
        // Ethereum
        //

        ethereumAdapter.lastBlockHeightFlowable.subscribe {
            updateLastBlockHeight()
            updateTransactions()
        }.let {
            disposables.add(it)
        }

        ethereumAdapter.transactionsFlowable.subscribe {
            updateTransactions()
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
    }

    private fun createKit(): EthereumKit {
        val syncSource: EthereumKit.SyncSource?
        val txApiProviderKey: String

        when (networkType) {
            NetworkType.BscMainNet -> {
                txApiProviderKey = bscScanKey
                syncSource = if (webSocket)
                    EthereumKit.defaultBscWebSocketSyncSource()
                else
                    EthereumKit.defaultBscHttpSyncSource()
            }
            else -> {
                txApiProviderKey = etherscanKey
                syncSource = if (webSocket)
                    EthereumKit.infuraWebSocketSyncSource(networkType, infuraProjectId, infuraSecret)
                else
                    EthereumKit.infuraHttpSyncSource(networkType, infuraProjectId, infuraSecret)
            }
        }
        checkNotNull(syncSource) {
            throw Exception("Could not get syncSource!")
        }

        val seed = Mnemonic().toSeed(words)
        return EthereumKit.getInstance(App.instance, seed, networkType, syncSource, txApiProviderKey, walletId)
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

    private fun updateTransactions() {
        ethereumAdapter.transactions()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { list: List<TransactionRecord> ->
                    transactions.value = list
                }.let {
                    disposables.add(it)
                }
    }

    private fun updateErc20Transactions() {
        erc20Adapter.transactions()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { list: List<TransactionRecord> ->
                    transactions.value = list
                }.let {
                    disposables.add(it)
                }
    }


    //
    // Ethereum
    //

    fun refresh() {
        ethereumAdapter.refresh()
        erc20Adapter.refresh()
    }

    fun clear() {
        EthereumKit.clear(App.instance, networkType, walletId)
        Erc20Kit.clear(App.instance, networkType, walletId)
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

        estimateSingle.subscribeOn(Schedulers.io())
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
        val transactionsSingle = if (ethTx) ethereumAdapter.transactions() else erc20Adapter.transactions()

        transactionsSingle.observeOn(AndroidSchedulers.mainThread())
                .subscribe { txList: List<TransactionRecord> ->
                    transactions.value = txList
                }.let {
                    disposables.add(it)
                }
    }

    //
    // SWAP
    //


    fun syncSwapData() {
        val tokenIn = uniswapToken(fromToken)
        val tokenOut = uniswapToken(toToken)

        uniswapKit.swapData(tokenIn, tokenOut)
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
        erc20Adapter.allowance(uniswapKit.routerAddress)
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
        val spenderAddress = uniswapKit.routerAddress

        val token = fromToken ?: return
        val amount = decimalAmount.movePointRight(token.decimals).toBigInteger()

        val transactionData = erc20Adapter.approveTransactionData(spenderAddress, amount)

        ethereumKit.estimateGas(transactionData, gasPrice)
                .flatMap { gasLimit ->
                    logger.info("gas limit: $gasLimit")
                    ethereumKit.send(transactionData, gasPrice, gasLimit)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ txHash ->
                    logger.info("approve: $txHash")
                }, {
                    logger.warning("approve ERROR = ${it.message}")
                }).let {
                    disposables.add(it)
                }
    }

    private fun uniswapToken(token: Erc20Token?): Token {
        if (token == null)
            return uniswapKit.etherToken()

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
            uniswapKit.estimateSwap(tradeData, gasPrice)
                    .flatMap { gasLimit ->
                        logger.info("gas limit: $gasLimit")

                        val transactionData = uniswapKit.transactionData(tradeData)
                        ethereumKit.send(transactionData, gasPrice, gasLimit)
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

data class Erc20Token(
        val name: String,
        val code: String,
        val contractAddress: Address,
        val decimals: Int)
