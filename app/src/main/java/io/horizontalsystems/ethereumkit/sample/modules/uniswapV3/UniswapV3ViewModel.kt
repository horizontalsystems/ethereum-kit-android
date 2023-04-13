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
import io.horizontalsystems.uniswapkit.v3.SwapPath
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

    private var uniswapV3Kit = UniswapV3Kit.getInstance(ethereumKit)
    private var gasPrice: GasPrice = GasPrice.Legacy(20_000_000_000)

    val fromToken: Erc20Token = Configuration.erc20Tokens[0]
    val toToken: Erc20Token = Configuration.erc20Tokens[1]

    private var amountIn: BigDecimal? = null
    private var amountOut: BigDecimal? = null
    private var tradeType: TradeType? = null
    private var loading = false
    private var error: Throwable? = null
    private var swapPath: SwapPath? = null

    var swapState by mutableStateOf(
        SwapState(
            amountIn = amountIn,
            amountOut = amountOut,
            tradeType = tradeType,
            loading = loading,
            error = error
        )
    )
        private set

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
        amountOut = null
        swapPath = null
        tradeType = TradeType.ExactIn
        error = null

        if (amountIn == null) {
            loading = false
            emitState()
        } else {
            loading = true
            emitState()

            job = viewModelScope.launch(Dispatchers.IO) {
                val bestTradeExactIn = uniswapV3Kit.bestTradeExactIn(
                    tokenIn = fromToken.contractAddress,
                    tokenOut = toToken.contractAddress,
                    amountIn = amountIn.movePointRight(fromToken.decimals).toBigInteger()
                )

                if (bestTradeExactIn != null) {
                    amountOut = BigDecimal(bestTradeExactIn.amountOut, toToken.decimals)
                    swapPath = bestTradeExactIn.swapPath
                } else {
                    error = Exception("No pool found for swap")
                }

                loading = false
                emitState()
            }
        }
    }

    fun onChangeAmountOut(amountOut: BigDecimal?) {
        job?.cancel()
        this.amountOut = amountOut
        amountIn = null
        swapPath = null
        tradeType = TradeType.ExactOut
        error = null

        if (amountOut == null) {
            loading = false
            emitState()
        } else {
            loading = true
            emitState()

            job = viewModelScope.launch(Dispatchers.IO) {
                val bestTradeExactOut = uniswapV3Kit.bestTradeExactOut(
                    tokenIn = fromToken.contractAddress,
                    tokenOut = toToken.contractAddress,
                    amountOut = amountOut.movePointRight(toToken.decimals).toBigInteger()
                )

                if (bestTradeExactOut != null) {
                    amountIn = BigDecimal(bestTradeExactOut.amountIn, fromToken.decimals)
                    swapPath = bestTradeExactOut.swapPath
                } else {
                    error = Exception("No pool found for swap")
                }

                loading = false
                emitState()
            }
        }
    }

    private fun emitState() {
        viewModelScope.launch {
            swapState = SwapState(
                amountOut = amountOut,
                amountIn = amountIn,
                tradeType = tradeType,
                loading = loading,
                error = error
            )
            Log.e("AAA", "swapState: $swapState")
        }
    }

    fun swap() {
        val tmpAmountIn = amountIn ?: return
        val tmpAmountOut = amountOut ?: return
        val tradeType = tradeType ?: return
        val swapPath = swapPath ?: return

        viewModelScope.launch {
            val amountIn = tmpAmountIn.movePointRight(fromToken.decimals).toBigInteger()
            val amountOut = tmpAmountOut.movePointRight(toToken.decimals).toBigInteger()
            val transactionData = uniswapV3Kit.transactionData(
                tradeType = tradeType,
                swapPath = swapPath,
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
                error = null
                Log.e("AAA", "swap SUCCESS, txHash=${fullTransaction.transaction.hash.toHexString()}")
            } catch (it: Throwable) {
                error = it
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
    val tradeType: TradeType?,
    val loading: Boolean,
    val error: Throwable?,
)
