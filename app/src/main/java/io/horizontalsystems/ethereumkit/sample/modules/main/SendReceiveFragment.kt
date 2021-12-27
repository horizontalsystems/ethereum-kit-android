package io.horizontalsystems.ethereumkit.sample.modules.main

import androidx.lifecycle.Observer
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.ethereumkit.sample.R
import kotlinx.android.synthetic.main.fragment_send_receive.*
import java.math.BigDecimal

class SendReceiveFragment : Fragment() {

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_send_receive, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = activity?.let { ViewModelProvider(it).get(MainViewModel::class.java) } ?: return

        viewModel.estimatedGas.observe(viewLifecycleOwner, Observer { estimatedGas ->
            estimateGasText.text = estimatedGas
        })

        viewModel.sendStatus.observe(viewLifecycleOwner, Observer { sendError ->
            val msg = if (sendError != null) sendError.localizedMessage else " Successfully sent!"
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        })

        receiveAddressButton.setOnClickListener {
            receiveAddressText.text = viewModel.receiveAddress()
        }

        estimateGasButton.setOnClickListener {
            onTapEstimateGas(false)
        }

        estimateErc20GasButton.setOnClickListener {
            onTapEstimateGas(true)
        }

        sendButton.setOnClickListener {
            when {
                sendAddress.text.isEmpty() -> sendAddress.error = "Send address cannot be blank"
                sendAmount.text.isEmpty() -> sendAmount.error = "Send amount cannot be blank"
                else -> viewModel.send(sendAddress.text.toString(), sendAmount.text.toString().toBigDecimal())
            }
        }

        sendErc20.setOnClickListener {
            when {
                sendAmount.text.isEmpty() -> sendAmount.error = "Send amount cannot be blank"
                else -> viewModel.sendERC20(sendAddress.text.toString(), sendAmount.text.toString().toBigDecimal())
            }
        }
    }

    private fun onTapEstimateGas(isErc20: Boolean) {
        val toAddress = sendAddress.text.toString()
        val resolvedToAddress = if (toAddress.isNotBlank()) toAddress else null
        val resolvedAmount = sendAmount.text.toString().toBigDecimalOrNull() ?: BigDecimal.ZERO

        viewModel.estimateGas(resolvedToAddress, resolvedAmount, isErc20)
    }

}
