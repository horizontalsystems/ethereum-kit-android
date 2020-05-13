package io.horizontalsystems.ethereumkit.sample

import androidx.lifecycle.Observer
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.ethereumkit.core.EthereumKit
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

        viewModel.fee.observe(viewLifecycleOwner, Observer { fee ->
            feeValue.text = fee?.toPlainString()
        })

        viewModel.lastBlockHeight.observe(viewLifecycleOwner, Observer { lbh ->
            lbhValue.text = (lbh ?: 0).toString()
        })

        viewModel.syncState.observe(viewLifecycleOwner, Observer { state ->
            syncStateValue.text = getSynStateText(state)
        })

        viewModel.transactionsSyncState.observe(viewLifecycleOwner, Observer { state ->
            txSyncStateValue.text = getSynStateText(state)
        })

        viewModel.erc20SyncState.observe(viewLifecycleOwner, Observer { state ->
            erc20SyncStateValue.text = getSynStateText(state)
        })

        viewModel.erc20TransactionsSyncState.observe(viewLifecycleOwner, Observer { state ->
            erc20TxSyncStateValue.text = getSynStateText(state)
        })

        buttonRefresh.setOnClickListener {
            viewModel.refresh()
        }

        buttonClear.setOnClickListener {
            viewModel.clear()
        }
    }

    private fun getSynStateText(syncState: EthereumKit.SyncState) =
            when (syncState) {
                is EthereumKit.SyncState.Synced -> "Synced"
                is EthereumKit.SyncState.Syncing -> "Syncing ${syncState.progress ?: ""}"
                is EthereumKit.SyncState.NotSynced -> "NotSynced"
            }

}
