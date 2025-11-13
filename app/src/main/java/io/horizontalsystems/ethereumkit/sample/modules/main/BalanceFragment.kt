package io.horizontalsystems.ethereumkit.sample.modules.main

import androidx.lifecycle.Observer
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.sample.databinding.FragmentBalanceBinding

class BalanceFragment : Fragment() {

    private lateinit var viewModel: MainViewModel

    private var _binding: FragmentBalanceBinding? = null // 2. Add binding property
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBalanceBinding.inflate(inflater, container, false) // 3. Inflate with ViewBinding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        viewModel.balance.observe(viewLifecycleOwner, Observer { balance ->
            binding.balanceValue.text = (balance ?: 0).toString()
        })

        viewModel.erc20TokenBalance.observe(viewLifecycleOwner, Observer { balance ->
            binding. tokenBalanceValue.text = (balance ?: 0).toString()
        })

        viewModel.lastBlockHeight.observe(viewLifecycleOwner, Observer { lbh ->
            binding.lbhValue.text = (lbh ?: 0).toString()
        })

        viewModel.syncState.observe(viewLifecycleOwner, Observer { state ->
            val syncStateInfo = getSynStateInfo(state)
            binding.syncStateValue.text = syncStateInfo.description
            if (syncStateInfo.error == null) {
                binding.syncStateError.visibility = View.GONE
            } else {
                binding.syncStateError.text = syncStateInfo.error.message
                binding.syncStateError.visibility = View.VISIBLE
            }
        })

        viewModel.transactionsSyncState.observe(viewLifecycleOwner, Observer { state ->
            binding.txSyncStateValue.text = getSynStateInfo(state).description
        })

        viewModel.erc20SyncState.observe(viewLifecycleOwner, Observer { state ->
            val syncStateInfo = getSynStateInfo(state)
            binding.erc20SyncStateValue.text = syncStateInfo.description
            if (syncStateInfo.error == null) {
                binding.erc20SyncStateError.visibility = View.GONE
            } else {
                binding.erc20SyncStateError.text = syncStateInfo.error.message
                binding.erc20SyncStateError.visibility = View.VISIBLE
            }
        })

        viewModel.erc20TransactionsSyncState.observe(viewLifecycleOwner, Observer { state ->
            binding.erc20TxSyncStateValue.text = getSynStateInfo(state).description
        })

        binding.buttonRefresh.setOnClickListener {
            viewModel.refresh()
        }

        binding.buttonClear.setOnClickListener {
            viewModel.clear()
        }
    }

    private fun getSynStateInfo(syncState: EthereumKit.SyncState): SyncStateInfo =
            when (syncState) {
                is EthereumKit.SyncState.Synced -> SyncStateInfo("Synced")
                is EthereumKit.SyncState.Syncing -> SyncStateInfo("Syncing ${syncState.progress ?: ""}")
                is EthereumKit.SyncState.NotSynced -> SyncStateInfo("NotSynced", syncState.error)
            }

    data class SyncStateInfo(val description: String, val error: Throwable? = null)

}
