package io.horizontalsystems.ethereumkit.sample.modules.main

import androidx.lifecycle.Observer
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.sample.R
import kotlinx.android.synthetic.main.fragment_balance.*

class BalanceFragment : Fragment() {

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_balance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        viewModel.balance.observe(viewLifecycleOwner, Observer { balance ->
            balanceValue.text = (balance ?: 0).toString()
        })

        viewModel.erc20TokenBalance.observe(viewLifecycleOwner, Observer { balance ->
            tokenBalanceValue.text = (balance ?: 0).toString()
        })

        viewModel.lastBlockHeight.observe(viewLifecycleOwner, Observer { lbh ->
            lbhValue.text = (lbh ?: 0).toString()
        })

        viewModel.syncState.observe(viewLifecycleOwner, Observer { state ->
            val syncStateInfo = getSynStateInfo(state)
            syncStateValue.text = syncStateInfo.description
            if (syncStateInfo.error == null) {
                syncStateError.visibility = View.GONE
            } else {
                syncStateError.text = syncStateInfo.error.message
                syncStateError.visibility = View.VISIBLE
            }
        })

        viewModel.transactionsSyncState.observe(viewLifecycleOwner, Observer { state ->
            txSyncStateValue.text = getSynStateInfo(state).description
        })

        viewModel.erc20SyncState.observe(viewLifecycleOwner, Observer { state ->
            val syncStateInfo = getSynStateInfo(state)
            erc20SyncStateValue.text = syncStateInfo.description
            if (syncStateInfo.error == null) {
                erc20SyncStateError.visibility = View.GONE
            } else {
                erc20SyncStateError.text = syncStateInfo.error.message
                erc20SyncStateError.visibility = View.VISIBLE
            }
        })

        viewModel.erc20TransactionsSyncState.observe(viewLifecycleOwner, Observer { state ->
            erc20TxSyncStateValue.text = getSynStateInfo(state).description
        })

        buttonRefresh.setOnClickListener {
            viewModel.refresh()
        }

        buttonClear.setOnClickListener {
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
