package io.horizontalsystems.ethereumkit.sample

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class SendReceiveFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var receiveAddressButton: Button
    private lateinit var receiveAddressText: TextView
    private lateinit var estimateGasText: TextView

    private lateinit var sendToken: Button
    private lateinit var sendButton: Button
    private lateinit var estimateGasButton: Button
    private lateinit var estimateTGasButton: Button
    private lateinit var sendAmount: EditText
    private lateinit var sendAddress: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(MainViewModel::class.java)
        }

        viewModel.estimateGas.observe(this, Observer { estimeGasVal ->
            estimateGasText.text = "Estimage Gas:${(estimeGasVal ?: 0)}"
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_send_receive, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        receiveAddressText = view.findViewById(R.id.receiveAddressText)
        estimateGasText = view.findViewById(R.id.estimateGasText)
        estimateGasText = view.findViewById(R.id.estimateGasText)
        receiveAddressButton = view.findViewById(R.id.receiveAddressButton)
        estimateGasButton = view.findViewById(R.id.estimateGasButton)
        estimateTGasButton = view.findViewById(R.id.estimateTGasButton)

        receiveAddressButton.setOnClickListener {
            receiveAddressText.text = viewModel.receiveAddress()
        }
        estimateGasButton.setOnClickListener {
            when {
                sendAddress.text.isEmpty() -> sendAddress.error = "Send address cannot be blank"
                else -> viewModel.estimageGas(sendAddress.text.toString(), sendAmount.text.toString().toBigDecimal())
            }
            estimateGasText.text = viewModel.receiveAddress()
        }

        estimateTGasButton.setOnClickListener {
            when {
                sendAddress.text.isEmpty() -> sendAddress.error = "Send address cannot be blank"
                else -> viewModel.estimateERC20Gas(sendAddress.text.toString(), sendAmount.text.toString().toBigDecimal())
            }
            estimateGasText.text = viewModel.receiveAddress()
        }

        sendAddress = view.findViewById(R.id.sendAddress)
        sendAmount = view.findViewById(R.id.sendAmount)
        sendButton = view.findViewById(R.id.sendButton)
        sendButton.setOnClickListener {
            when {
                sendAddress.text.isEmpty() -> sendAddress.error = "Send address cannot be blank"
                sendAmount.text.isEmpty() -> sendAmount.error = "Send amount cannot be blank"
                else -> viewModel.send(sendAddress.text.toString(), sendAmount.text.toString().toBigDecimal())
            }
        }

        sendToken = view.findViewById(R.id.sendToken)
        sendToken.setOnClickListener {
            when {
                sendAmount.text.isEmpty() -> sendAmount.error = "Send amount cannot be blank"
                else -> viewModel.sendERC20(sendAddress.text.toString(), sendAmount.text.toString().toBigDecimal())
            }
        }

        viewModel.sendStatus.observe(this, Observer { sendError ->

            val msg = if (sendError != null) sendError.localizedMessage else " Successfully sent!"

            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        })
    }

}
