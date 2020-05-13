package io.horizontalsystems.ethereumkit.sample

import androidx.lifecycle.Observer
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.ethereumkit.core.EthereumKit

class BalanceFragment : Fragment() {

    lateinit var viewModel: MainViewModel
    lateinit var balanceValue: TextView
    lateinit var tokenBalanceValue: TextView
    lateinit var feeValue: TextView
    lateinit var lbhValue: TextView
    lateinit var syncStateValue: TextView
    lateinit var txSyncStateValue: TextView
    lateinit var erc20SyncStateValue: TextView
    lateinit var erc20TxSyncStateValue: TextView
    lateinit var refreshButton: Button
    lateinit var clearButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = activity?.let { ViewModelProvider(it).get(MainViewModel::class.java) } ?: return

        viewModel.balance.observe(this, Observer { balance ->
            balanceValue.text = (balance ?: 0).toString()
        })

        viewModel.erc20TokenBalance.observe(this, Observer { balance ->
            tokenBalanceValue.text = (balance ?: 0).toString()
        })

        viewModel.fee.observe(this, Observer { fee ->
            feeValue.text = fee?.toPlainString()
        })

        viewModel.lastBlockHeight.observe(this, Observer { lbh ->
            lbhValue.text = (lbh ?: 0).toString()
        })

        viewModel.syncState.observe(this, Observer { state ->
            syncStateValue.text = getSynStateText(state)
        })

        viewModel.transactionsSyncState.observe(this, Observer { state ->
            txSyncStateValue.text = getSynStateText(state)
        })

        viewModel.erc20SyncState.observe(this, Observer { state ->
            erc20SyncStateValue.text = getSynStateText(state)
        })

        viewModel.erc20TransactionsSyncState.observe(this, Observer { state ->
            erc20TxSyncStateValue.text = getSynStateText(state)
        })

    }

    private fun getSynStateText(syncState: EthereumKit.SyncState) =
            when (syncState) {
                is EthereumKit.SyncState.Synced -> "Synced"
                is EthereumKit.SyncState.Syncing -> "Syncing ${syncState.progress ?: ""}"
                is EthereumKit.SyncState.NotSynced -> "NotSynced"
            }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_balance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        balanceValue = view.findViewById(R.id.balanceValue)
        tokenBalanceValue = view.findViewById(R.id.tokenBalanceValue)
        refreshButton = view.findViewById(R.id.buttonRefresh)
        clearButton = view.findViewById(R.id.buttonClear)
        feeValue = view.findViewById(R.id.feeValue)
        lbhValue = view.findViewById(R.id.lbhValue)
        syncStateValue = view.findViewById(R.id.syncStateValue)
        txSyncStateValue = view.findViewById(R.id.txSyncStateValue)
        erc20SyncStateValue = view.findViewById(R.id.erc20SyncStateValue)
        erc20TxSyncStateValue = view.findViewById(R.id.erc20TxSyncStateValue)

        refreshButton.setOnClickListener {
            viewModel.refresh()
        }

        clearButton.setOnClickListener {
            viewModel.clear()
        }
    }
}
