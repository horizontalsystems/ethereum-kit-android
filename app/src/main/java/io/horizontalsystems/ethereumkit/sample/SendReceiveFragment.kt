package io.horizontalsystems.ethereumkit.sample

import androidx.lifecycle.Observer
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.fragment_send_receive.*

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
            when {
                sendAddress.text.isEmpty() -> sendAddress.error = "Send address cannot be blank"
                else -> viewModel.estimateGas(sendAddress.text.toString(), sendAmount.text.toString().toBigDecimal())
            }
            estimateGasText.text = viewModel.receiveAddress()
        }

        estimateErc20GasButton.setOnClickListener {
            when {
                sendAddress.text.isEmpty() -> sendAddress.error = "Send address cannot be blank"
                sendAmount.text.isEmpty() || sendAmount.text.toString().toBigDecimalOrNull() == null -> sendAmount.error = "Invalid amount"
                else -> viewModel.estimateERC20Gas(sendAddress.text.toString(), sendAmount.text.toString().toBigDecimal())
            }
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

}
