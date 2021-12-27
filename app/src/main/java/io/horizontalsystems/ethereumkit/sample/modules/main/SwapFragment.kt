package io.horizontalsystems.ethereumkit.sample.modules.main

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.ethereumkit.sample.Configuration
import io.horizontalsystems.ethereumkit.sample.R
import io.horizontalsystems.uniswapkit.models.Token
import io.horizontalsystems.uniswapkit.models.TradeType
import kotlinx.android.synthetic.main.fragment_swap.*
import java.math.BigDecimal

class SwapFragment : Fragment() {

    private lateinit var viewModel: MainViewModel

    private val fromAmountListener = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            setToAmount(null)

            if (s != null && s.isNotEmpty()) {
                viewModel.onChangeAmountIn(BigDecimal(s.toString()))
            }
        }

        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    }

    private val toAmountListener = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            setFromAmount(null)

            if (s != null && s.isNotEmpty()) {
                viewModel.onChangeAmountOut(BigDecimal(s.toString()))
            }
        }

        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_swap, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = activity?.let { ViewModelProvider(it).get(MainViewModel::class.java) } ?: return

        viewModel.swapStatus.observe(viewLifecycleOwner, Observer { swapError ->
            val msg = if (swapError != null) swapError.localizedMessage else " Successfully swapped!"
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        })

        viewModel.swapData.observe(viewLifecycleOwner, Observer { swapData ->
            fromAmount.isEnabled = swapData != null
            toAmount.isEnabled = swapData != null
        })

        viewModel.tradeData.observe(viewLifecycleOwner, Observer { tradeData ->

            if (tradeData == null) {
                minMax.text = null
                executionPrice.text = null
                midPrice.text = null
                priceImpact.text = null
            } else {
                when (tradeData.type) {
                    TradeType.ExactIn -> {
                        setToAmount(tradeData.amountOut)
                        minMax.text = "Minimum Received: ${tradeData.amountOutMin?.let { "${it.stripTrailingZeros().toPlainString()} $toTokenCode" } ?: ""}"
                    }
                    TradeType.ExactOut -> {
                        setFromAmount(tradeData.amountIn)
                        minMax.text = "Maximum Sold: ${tradeData.amountInMax?.let { "${it.stripTrailingZeros().toPlainString()} $fromTokenCode" } ?: ""}"
                    }
                }

                val executionPriceStr = tradeData.executionPrice?.let {
                    "${it.toPlainString()} $toTokenCode / $fromTokenCode "
                }
                executionPrice.text = "Execution Price: " + (executionPriceStr ?: "")

                val midPriceStr = tradeData.midPrice?.let {
                    "${it.toPlainString()} $toTokenCode / $fromTokenCode "
                }
                midPrice.text = "Mid Price: " + (midPriceStr ?: "")

                priceImpact.text = "Price Impact: ${tradeData.priceImpact?.toPlainString() ?: ""}%"

                providerFee.text = "Provider Fee: ${tradeData.providerFee?.toPlainString() ?: ""}"

                path.text = "Path: ${pathDescription(tradeData.path)}"

                updateLabels(tradeData.type)
            }
        })

        buttonSyncSwapData.setOnClickListener {
            syncSwapData()
        }

        buttonSwap.setOnClickListener {
            viewModel.swap()
        }

        buttonSyncAllowance.setOnClickListener {
            viewModel.syncAllowance()
        }

        buttonApprove.setOnClickListener {
            fromAmount.text?.let {
                if (it.isNotBlank()) {
                    viewModel.approve(BigDecimal(it.toString()))
                }
            }
        }

        fromAmount.addTextChangedListener(fromAmountListener)
        toAmount.addTextChangedListener(toAmountListener)

        updateLabels(TradeType.ExactIn)
    }

    private fun pathDescription(path: List<Token>): String {
        val parts = path.map { token ->
            if (token.isEther) "ETH" else (Configuration.erc20Tokens.firstOrNull { it.contractAddress == token.address }?.code
                    ?: token.address.hex)
        }
        return parts.joinToString(" > ")
    }

    private val fromTokenCode: String
        get() = viewModel.fromToken?.code ?: "ETH"

    private val toTokenCode: String
        get() = viewModel.toToken?.code ?: "ETH"

    private fun syncSwapData() {
        fromAmount.isEnabled = false
        toAmount.isEnabled = false

        viewModel.syncSwapData()
    }

    private fun setFromAmount(amount: BigDecimal?) {
        fromAmount.removeTextChangedListener(fromAmountListener)
        fromAmount.setText(amount?.stripTrailingZeros()?.toPlainString())
        fromAmount.addTextChangedListener(fromAmountListener)
    }

    private fun setToAmount(amount: BigDecimal?) {
        toAmount.removeTextChangedListener(toAmountListener)
        toAmount.setText(amount?.stripTrailingZeros()?.toPlainString())
        toAmount.addTextChangedListener(toAmountListener)
    }

    private fun updateLabels(tradeType: TradeType) {
        var fromLabel = "From $fromTokenCode"
        var toLabel = "To $toTokenCode"

        if (tradeType == TradeType.ExactIn) {
            toLabel += " (estimated)"
        } else {
            fromLabel += " (estimated)"
        }

        fromAmount.hint = fromLabel
        fromAmountLayout.hint = fromLabel
        toAmount.hint = toLabel
        toAmountLayout.hint = toLabel
    }

}
