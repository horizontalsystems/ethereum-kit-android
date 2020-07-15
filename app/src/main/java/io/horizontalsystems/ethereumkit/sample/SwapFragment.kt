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
import kotlinx.android.synthetic.main.fragment_swap.*
import java.math.BigDecimal

class SwapFragment : Fragment() {

    private lateinit var viewModel: MainViewModel

    private val tokens = listOf(
            Erc20Token("GMO coins", "GMOLW", "0xbb74a24d83470f64d5f0c01688fbb49a5a251b32", 18),
            Erc20Token("DAI", "DAI", "0xad6d458402f60fd3bd25163575031acdce07538d", 18)
    )

    private val fromToken: Erc20Token? = tokens[1]
    private val toToken: Erc20Token? = null //tokens[1]

    private val fromAmountListener = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            setToAmount("")
            updateLabels(exactIn = true)

            if (s != null && s.isNotEmpty()) {
                viewModel.onChangeAmountIn(BigDecimal(s.toString()).scaleByPowerOfTen(fromToken?.decimals
                        ?: 18).toBigInteger())
            }
        }

        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    }

    private val toAmountListener = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            setFromAmount("")
            updateLabels(exactIn = false)

            if (s != null && s.isNotEmpty()) {
                viewModel.onChangeAmountOut(BigDecimal(s.toString()).scaleByPowerOfTen(toToken?.decimals
                        ?: 18).toBigInteger())
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
            setFromAmount(tradeData?.amountIn ?: "")
            setToAmount(tradeData?.amountOut ?: "")
        })

        fromAmount.addTextChangedListener(fromAmountListener)
        toAmount.addTextChangedListener(toAmountListener)

        updateLabels(exactIn = true)

        buttonSyncSwapData.setOnClickListener {
            fromAmount.isEnabled = false
            toAmount.isEnabled = false

            viewModel.syncSwapData(fromToken, toToken)
        }

        buttonSwap.setOnClickListener {
            viewModel.swap()
        }

    }

    private fun setFromAmount(amount: String) {
        fromAmount.removeTextChangedListener(fromAmountListener)
        fromAmount.setText(amount)
        fromAmount.setSelection(amount.length)
        fromAmount.addTextChangedListener(fromAmountListener)
    }

    private fun setToAmount(amount: String) {
        toAmount.removeTextChangedListener(toAmountListener)
        toAmount.setText(amount)
        toAmount.addTextChangedListener(toAmountListener)
    }

    private fun updateLabels(exactIn: Boolean) {
        var fromLabel = "From ${fromToken?.code ?: "ETH"}"
        var toLabel = "To ${toToken?.code ?: "ETH"}"

        if (exactIn) {
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
