package io.horizontalsystems.ethereumkit.sample.modules.addresswatch

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.ethereumkit.sample.R
import io.horizontalsystems.ethereumkit.sample.modules.main.ShowTxType
import io.horizontalsystems.ethereumkit.sample.modules.main.TransactionsAdapter

class AddressWatchActivity : AppCompatActivity() {

    private lateinit var viewModel: AddressWatchViewModel

    private lateinit var addressInput: EditText
    private lateinit var watchButton: Button
    private lateinit var ethFilter: Button
    private lateinit var tokenFilter: Button
    private lateinit var transactionsRecyclerView: RecyclerView
    private lateinit var transactionSyncProgress: ProgressBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(AddressWatchViewModel::class.java)

        addressInput = findViewById(R.id.addressInput)
        watchButton = findViewById(R.id.watchButton)
        ethFilter = findViewById(R.id.ethFilter)
        tokenFilter = findViewById(R.id.tokenFilter)
        transactionsRecyclerView = findViewById(R.id.transactionsRecyclerView)
        transactionSyncProgress = findViewById(R.id.transactionSyncProgress)

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
