package io.horizontalsystems.ethereumkit.sample.modules.addresswatch

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import io.horizontalsystems.ethereumkit.sample.R
import io.horizontalsystems.ethereumkit.sample.modules.main.ShowTxType
import io.horizontalsystems.ethereumkit.sample.modules.main.TransactionsAdapter
import kotlinx.android.synthetic.main.activity_address_watch.*
import kotlinx.android.synthetic.main.activity_address_watch.ethFilter
import kotlinx.android.synthetic.main.activity_address_watch.tokenFilter
import kotlinx.android.synthetic.main.activity_address_watch.transactionsRecyclerView

class AddressWatchActivity : AppCompatActivity() {

    private lateinit var viewModel: AddressWatchViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(AddressWatchViewModel::class.java)

        setContentView(R.layout.activity_address_watch)

        watchButton.setOnClickListener {
            val address = addressInput.text.toString()
            viewModel.watchAddress(address)
        }

        ethFilter.setOnClickListener {
            viewModel.filterTransactions(true)
        }

        tokenFilter.setOnClickListener {
            viewModel.filterTransactions(false)
        }

        val transactionsAdapter = TransactionsAdapter()
        transactionsRecyclerView.adapter = transactionsAdapter
        transactionsRecyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.transactions.observe(this, { txs ->
            txs?.let { transactions ->
                transactionsAdapter.items = transactions
                transactionsAdapter.notifyDataSetChanged()
            }
        })

        viewModel.lastBlockHeight.observe(this, { height ->
            height?.let {
                transactionsAdapter.lastBlockHeight = height
            }
        })

        viewModel.transactionsSyncingLiveData.observe(this, { syncing ->
            transactionSyncProgress.visibility = if (syncing) View.VISIBLE else View.GONE
        })

        viewModel.showWarningLiveEvent.observe(this, { warning ->
            Toast.makeText(this, warning, Toast.LENGTH_SHORT).show()
        })

        viewModel.showTxTypeLiveData.observe(this, { showTxType ->
            when (showTxType) {
                ShowTxType.Eth -> {
                    tokenFilter.setBackgroundColor(getColor(R.color.colorTab))
                    ethFilter.setBackgroundColor(getColor(R.color.colorTabSelected))
                }
                ShowTxType.Erc20 -> {
                    ethFilter.setBackgroundColor(getColor(R.color.colorTab))
                    tokenFilter.setBackgroundColor(getColor(R.color.colorTabSelected))
                }
            }
        })
    }
}
