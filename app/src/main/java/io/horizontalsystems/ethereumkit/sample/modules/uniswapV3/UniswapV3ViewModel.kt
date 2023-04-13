package io.horizontalsystems.ethereumkit.sample.modules.uniswapV3

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.sample.Configuration
import io.horizontalsystems.ethereumkit.sample.core.Erc20Adapter
import io.horizontalsystems.ethereumkit.sample.modules.main.Erc20Token
import io.horizontalsystems.ethereumkit.sample.modules.main.GasPriceHelper
import io.horizontalsystems.uniswapkit.UniswapV3Kit
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.horizontalsystems.uniswapkit.models.TradeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.collect
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal

class UniswapV3ViewModel(
    private val ethereumKit: EthereumKit,
    private val erc20Adapter: Erc20Adapter,
    private val gasPriceHelper: GasPriceHelper,
    private val signer: Signer
) : ViewModel() {

    private var amountIn: BigDecimal? = null
    private var amountOut: BigDecimal? = null
    private var tradeType: TradeType? = null

    val fromToken: Erc20Token = Configuration.erc20Tokens[0]
    val toToken: Erc20Token = Configuration.erc20Tokens[1]

    private var uniswapV3Kit = UniswapV3Kit.getInstance(ethereumKit)

    var swapState by mutableStateOf<SwapState?>(null)
        private set
    private var gasPrice: GasPrice = GasPrice.Legacy(20_000_000_000)

    val swapStatus = mutableStateOf<Throwable?>(null)

    private val tradeOptions = TradeOptions(allowedSlippagePercent = BigDecimal("0.5"))

    private var job: Job? = null

    init {
        viewModelScope.launch {
            gasPriceHelper.gasPriceFlowable().collect {
                gasPrice = it
            }
        }
    }

    fun syncAllowance() {
        viewModelScope.launch {
            try {
                val it = erc20Adapter.allowance(uniswapV3Kit.routerAddress).await()
                Log.e("AAA", "allowance: ${it.toPlainString()}")
            } catch (it: Throwable) {
                Log.e("AAA", "swapData ERROR = ${it.message}")
            }
        }
    }

    fun approve(decimalAmount: BigDecimal) {
        val spenderAddress = uniswapV3Kit.routerAddress

        val token = fromToken
        val amount = decimalAmount.movePointRight(token.decimals).toBigInteger()

        val transactionData = erc20Adapter.approveTransactionData(spenderAddress, amount)

        viewModelScope.launch {
            try {
                val gasLimit = ethereumKit.estimateGas(transactionData, gasPrice).await()
                Log.e("AAA", "gas limit: $gasLimit")
                val rawTransaction = ethereumKit.rawTransaction(transactionData, gasPrice, gasLimit).await()
                val signature = signer.signature(rawTransaction)
                val fullTransaction = ethereumKit.send(rawTransaction, signature).await()
                Log.e("AAA", "approve: ${fullTransaction.transaction.hash}")

            } catch (it: Throwable) {
                Log.e("AAA", "approve ERROR = ${it.message}")
            }
        }
    }

    fun onChangeAmountIn(amountIn: BigDecimal?) {
        job?.cancel()
        this.amountIn = amountIn
        this.tradeType = TradeType.ExactIn

        if (amountIn == null) {
            amountOut = null
            emitState()
            return
        }

        job = viewModelScope.launch(Dispatchers.IO) {
            try {
                val bestTradeExactIn = uniswapV3Kit
                    .bestTradeExactIn(
                        tokenIn = fromToken.contractAddress,
                        tokenOut = toToken.contractAddress,
                        amountIn = amountIn.multiply(BigDecimal(10).pow(fromToken.decimals))
                            .toBigInteger()
                    )

                amountOut = BigDecimal(bestTradeExactIn.amountOut, toToken.decimals)
                emitState()
            } catch (error: Throwable) {
                Log.e(
                    "AAA",
                    "bestTradeExactIn error: ${error.javaClass.simpleName} (${error.localizedMessage})"
                )
            }
        }
    }

    fun onChangeAmountOut(amountOut: BigDecimal?) {
        job?.cancel()
        this.amountOut = amountOut
        this.tradeType = TradeType.ExactOut

        if (amountOut == null) {
            amountIn = null
            emitState()
            return
        }

        job = viewModelScope.launch(Dispatchers.IO) {
            try {
                val bestTradeExactOut = uniswapV3Kit
                    .bestTradeExactOut(
                        tokenIn = fromToken.contractAddress,
                        tokenOut = toToken.contractAddress,
                        amountOut = amountOut.multiply(BigDecimal(10).pow(toToken.decimals))
                            .toBigInteger()
                    )

                amountIn = BigDecimal(bestTradeExactOut.amountIn, fromToken.decimals)
                emitState()
            } catch (error: Throwable) {
                Log.e(
                    "AAA",
                    "bestTradeExactOut error: ${error.javaClass.simpleName} (${error.localizedMessage})"
                )
            }
        }
    }

    private fun emitState() {
        viewModelScope.launch {
            swapState = tradeType?.let {
                SwapState(
                    amountIn = amountIn,
                    amountOut = amountOut,
                    tradeType = it
                )
            }
        }
    }

    fun swap() {
        val swapState = swapState ?: return
        val amountInDecimal = swapState.amountIn ?: return
        val amountOutDecimal = swapState.amountOut ?: return

        viewModelScope.launch {
            val amountIn = amountInDecimal.multiply(BigDecimal(10).pow(fromToken.decimals)).toBigInteger()
            val amountOut = amountOutDecimal.multiply(BigDecimal(10).pow(toToken.decimals)).toBigInteger()
            val transactionData = uniswapV3Kit.transactionData(
                tradeType = swapState.tradeType,
                tokenIn = fromToken.contractAddress,
                tokenOut = toToken.contractAddress,
                amountIn = amountIn,
                amountOut = amountOut,
                tradeOptions = tradeOptions
            )

            try {
                val gasLimit = ethereumKit.estimateGas(transactionData, gasPrice).await()
                Log.e("AAA", "gas limit: $gasLimit")
                val rawTransaction = ethereumKit.rawTransaction(transactionData, gasPrice, gasLimit).await()
                val signature = signer.signature(rawTransaction)
                val fullTransaction = ethereumKit.send(rawTransaction, signature).await()
                swapStatus.value = null
                Log.e("AAA", "swap SUCCESS, txHash=${fullTransaction.transaction.hash.toHexString()}")
            } catch (it: Throwable) {
                swapStatus.value = it
                Log.e("AAA", "swap ERROR, error=${it.message}")
                it.printStackTrace()
            }
        }
    }

    class Factory(
        private val ethereumKit: EthereumKit,
        private val erc20Adapter: Erc20Adapter,
        private val gasPriceHelper: GasPriceHelper,
        private val signer: Signer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UniswapV3ViewModel(ethereumKit, erc20Adapter, gasPriceHelper, signer) as T
        }
    }
}


data class SwapState(
    val amountOut: BigDecimal?,
    val amountIn: BigDecimal?,
    val tradeType: TradeType,
)
