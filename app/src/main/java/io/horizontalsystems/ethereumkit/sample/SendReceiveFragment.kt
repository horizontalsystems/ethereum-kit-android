package io.horizontalsystems.ethereumkit.sample

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import io.horizontalsystems.ethereumkit.models.FeePriority

class SendReceiveFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var receiveAddressButton: Button
    private lateinit var receiveAddressText: TextView

    private lateinit var sendToken: Button
    private lateinit var sendButton: Button
    private lateinit var sendAmount: EditText
    private lateinit var sendAddress: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(MainViewModel::class.java)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_send_receive, null)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        receiveAddressText = view.findViewById(R.id.receiveAddressText)
        receiveAddressButton = view.findViewById(R.id.receiveAddressButton)
        receiveAddressButton.setOnClickListener {
            receiveAddressText.text = viewModel.receiveAddress()
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
                sendAddress.text.isEmpty() -> sendAddress.error = "Send address cannot be blank"
                sendAmount.text.isEmpty() -> sendAmount.error = "Send amount cannot be blank"
                else -> viewModel.sendERC20(sendAddress.text.toString(), sendAmount.text.toString().toBigDecimal())
            }
        }

        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroup)
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val feePriority = when (checkedId) {
                R.id.radioLowest ->  FeePriority.Lowest
                R.id.radioLow ->  FeePriority.Low
                R.id.radioMedium ->  FeePriority.Medium
                R.id.radioHigh ->  FeePriority.High
                else ->  FeePriority.Highest
            }
            viewModel.feePriority = feePriority
        }

        viewModel.sendStatus.observe(this, Observer { sendError ->

            val msg = if (sendError != null) sendError.localizedMessage else " Successfully sent!"

            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        })
    }

}
