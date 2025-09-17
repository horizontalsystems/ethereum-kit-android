package io.horizontalsystems.ethereumkit.sample.modules.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.ethereumkit.sample.databinding.FragmentSendReceiveBinding
import java.math.BigDecimal

class SendReceiveFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private var _binding: FragmentSendReceiveBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSendReceiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = activity?.let { ViewModelProvider(it).get(MainViewModel::class.java) } ?: return

        viewModel.estimatedGas.observe(viewLifecycleOwner, Observer { estimatedGas ->
            binding.estimateGasText.text = estimatedGas
        })

        viewModel.sendStatus.observe(viewLifecycleOwner, Observer { sendError ->
            val msg = if (sendError != null) sendError.localizedMessage else " Successfully sent!"
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        })

        binding.receiveAddressButton.setOnClickListener {
            binding.receiveAddressText.text = viewModel.receiveAddress()
        }

        binding.estimateGasButton.setOnClickListener {
            onTapEstimateGas(false)
        }

        binding.estimateErc20GasButton.setOnClickListener {
            onTapEstimateGas(true)
        }

        binding.sendButton.setOnClickListener {
            when {
                binding.sendAddress.text.isEmpty() -> binding.sendAddress.error = "Send address cannot be blank"
                binding.sendAmount.text.isEmpty() -> binding.sendAmount.error = "Send amount cannot be blank"
                else -> viewModel.send(binding.sendAddress.text.toString(), binding.sendAmount.text.toString().toBigDecimal())
            }
        }

        binding.sendErc20.setOnClickListener {
            when {
                binding.sendAmount.text.isEmpty() -> binding.sendAmount.error = "Send amount cannot be blank"
                else -> viewModel.sendERC20(binding.sendAddress.text.toString(), binding.sendAmount.text.toString().toBigDecimal())
            }
        }
    }

    private fun onTapEstimateGas(isErc20: Boolean) {
        val toAddress = binding.sendAddress.text.toString()
        val resolvedToAddress = if (toAddress.isNotBlank()) toAddress else null
        val resolvedAmount = binding.sendAmount.text.toString().toBigDecimalOrNull() ?: BigDecimal.ZERO

        viewModel.estimateGas(resolvedToAddress, resolvedAmount, isErc20)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
