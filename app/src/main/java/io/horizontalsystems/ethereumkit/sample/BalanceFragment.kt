package io.horizontalsystems.ethereumkit.sample

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import io.horizontalsystems.ethereumkit.EthereumKit

class BalanceFragment : Fragment() {

    lateinit var viewModel: MainViewModel
    lateinit var balanceValue: TextView
    lateinit var tokenBalanceValue: TextView
    lateinit var feeValue: TextView
    lateinit var lbhValue: TextView
    lateinit var kitStateValue: TextView
    lateinit var erc20StateValue: TextView
    lateinit var refreshButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(MainViewModel::class.java)

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
            viewModel.etherState.observe(this, Observer { kitState ->
                kitStateValue.text = when (kitState) {
                    is EthereumKit.SyncState.Synced -> "Synced"
                    is EthereumKit.SyncState.Syncing -> "Syncing"
                    is EthereumKit.SyncState.NotSynced -> "NotSynced"
                    else -> "null"
                }
            })

            viewModel.erc20State.observe(this, Observer { kitState ->
                erc20StateValue.text = when (kitState) {
                    is EthereumKit.SyncState.Synced -> "Synced"
                    is EthereumKit.SyncState.Syncing -> "Syncing"
                    is EthereumKit.SyncState.NotSynced -> "NotSynced"
                    else -> "null"
                }
            })
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_balance, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        balanceValue = view.findViewById(R.id.balanceValue)
        tokenBalanceValue = view.findViewById(R.id.tokenBalanceValue)
        refreshButton = view.findViewById(R.id.buttonRefresh)
        feeValue = view.findViewById(R.id.feeValue)
        lbhValue = view.findViewById(R.id.lbhValue)
        kitStateValue = view.findViewById(R.id.kitStateValue)
        erc20StateValue = view.findViewById(R.id.erc20StateValue)

        refreshButton.setOnClickListener {
            viewModel.refresh()
        }
    }
}
