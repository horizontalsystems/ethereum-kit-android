package io.horizontalsystems.ethereumkit.sample

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
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.uniswapkit.models.Token
import io.horizontalsystems.uniswapkit.models.TradeType
import kotlinx.android.synthetic.main.fragment_swap.*
import java.math.BigDecimal

class SwapFragment : Fragment() {

    private lateinit var viewModel: MainViewModel

    private val tokens = listOf(
            Erc20Token("GMO coins", "GMOLW", Address("0xbb74a24d83470f64d5f0c01688fbb49a5a251b32"), 18),
            Erc20Token("DAI", "DAI", Address("0xad6d458402f60fd3bd25163575031acdce07538d"), 18),
            Erc20Token("DAI-MAINNET", "DAI", Address("0x6b175474e89094c44da98b954eedeac495271d0f"), 18)
    )

    private val fromToken: Erc20Token? = tokens[1]
    private val toToken: Erc20Token? = null //tokens[0]

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
                        minMax.text = "Minimum Received: ${tradeData.amountOutMin?.let { "${it.stripTrailingZeros().toPlainString()} ${tokenCode(toToken)}" } ?: ""}"
                    }
                    TradeType.ExactOut -> {
                        setFromAmount(tradeData.amountIn)
                        minMax.text = "Maximum Sold: ${tradeData.amountInMax?.let { "${it.stripTrailingZeros().toPlainString()} ${tokenCode(fromToken)}" } ?: ""}"
                    }
                }

                val executionPriceStr = tradeData.executionPrice?.let {
                    "${it.toPlainString()} ${tokenCode(toToken)} / ${tokenCode(fromToken)} "
                }
                executionPrice.text = "Execution Price: " + (executionPriceStr ?: "")

                val midPriceStr = tradeData.midPrice?.let {
                    "${it.toPlainString()} ${tokenCode(toToken)} / ${tokenCode(fromToken)} "
                }
                midPrice.text = "Mid Price: " + (midPriceStr ?: "")

                priceImpact.text = "Price Impact: ${tradeData.priceImpact?.toPlainString() ?: ""}%"

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

        fromAmount.addTextChangedListener(fromAmountListener)
        toAmount.addTextChangedListener(toAmountListener)

        updateLabels(TradeType.ExactIn)
        syncSwapData()
    }

    private fun pathDescription(path: List<Token>): String {
        val parts = path.map { token ->
            if (token.isEther) "ETH" else (tokens.firstOrNull { it.contractAddress == token.address }?.code
                    ?: token.address.hex)
        }
        return parts.joinToString(" > ")
    }

    private fun tokenCode(erc20Token: Erc20Token?): String {
        return erc20Token?.code ?: "ETH"
    }

    private fun syncSwapData() {
        fromAmount.isEnabled = false
        toAmount.isEnabled = false

        viewModel.syncSwapData(fromToken, toToken)
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
        var fromLabel = "From ${fromToken?.code ?: "ETH"}"
        var toLabel = "To ${toToken?.code ?: "ETH"}"

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
