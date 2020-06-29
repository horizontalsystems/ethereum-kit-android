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
import io.horizontalsystems.uniswapkit.SwapItem
import kotlinx.android.synthetic.main.fragment_swap.*

class SwapFragment : Fragment() {

    private lateinit var viewModel: MainViewModel

    private var mode: Mode = Mode.exactFrom
    private val tokens = listOf(
            Erc20Token("GMO coins", "GMOLW", "0xbb74a24d83470f64d5f0c01688fbb49a5a251b32", 18),
            Erc20Token("DAI", "DAI", "0xad6d458402f60fd3bd25163575031acdce07538d", 18)
    )

    private val fromToken: Erc20Token? = tokens[0]
    private val toToken: Erc20Token? = tokens[1]

    private val fromTokenDecimal = fromToken?.decimal ?: 18
    private val toTokenDecimal = toToken?.decimal ?: 18

    private val fromAmountListener = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            mode = Mode.exactFrom
            setToAmount("")
            updateLabels()
        }

        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    }

    private val toAmountListener = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            mode = Mode.exactTo
            setFromAmount("")
            updateLabels()
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

        viewModel.pathItems.observe(viewLifecycleOwner, Observer { pathItems ->
            val firstAmount = pathItems.first().amount.toBigDecimal().movePointLeft(fromTokenDecimal)
            setFromAmount(firstAmount.stripTrailingZeros().toPlainString())

            val lastAmount = pathItems.last().amount.toBigDecimal().movePointLeft(toTokenDecimal)
            setToAmount(lastAmount.stripTrailingZeros().toPlainString())
        })

        viewModel.swapStatus.observe(viewLifecycleOwner, Observer { swapError ->
            val msg = if (swapError != null) swapError.localizedMessage else " Successfully swapped!"
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        })

        fromAmount.addTextChangedListener(fromAmountListener)
        toAmount.addTextChangedListener(toAmountListener)

        updateLabels()

        buttonEstimate.setOnClickListener {
            val amount = if (mode == Mode.exactFrom)
                fromAmount.text.toString().toBigDecimal().scaleByPowerOfTen(fromTokenDecimal).toBigInteger()
            else
                toAmount.text.toString().toBigDecimal().scaleByPowerOfTen(toTokenDecimal).toBigInteger()

            viewModel.estimate(mode, amount,
                    fromToken?.let { SwapItem.Erc20SwapItem(it.contractAddress) }
                            ?: SwapItem.EthereumSwapItem(),
                    toToken?.let { SwapItem.Erc20SwapItem(it.contractAddress) }
                            ?: SwapItem.EthereumSwapItem())
        }

        buttonSwap.setOnClickListener {
            viewModel.swap(mode)
        }

    }

    private fun setFromAmount(amount: String) {
        fromAmount.removeTextChangedListener(fromAmountListener)
        fromAmount.setText(amount)
        fromAmount.addTextChangedListener(fromAmountListener)
    }

    private fun setToAmount(amount: String) {
        toAmount.removeTextChangedListener(toAmountListener)
        toAmount.setText(amount)
        toAmount.addTextChangedListener(toAmountListener)
    }

    private fun updateLabels() {
        var fromLabel = "From ${fromToken?.code ?: "ETH"}"
        var toLabel = "To ${toToken?.code ?: "ETH"}"

        if (mode == Mode.exactFrom) {
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
