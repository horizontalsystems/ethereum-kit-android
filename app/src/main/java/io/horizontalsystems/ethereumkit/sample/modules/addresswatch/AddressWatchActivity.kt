package io.horizontalsystems.ethereumkit.sample.modules.addresswatch

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import io.horizontalsystems.ethereumkit.sample.R
import io.horizontalsystems.ethereumkit.sample.databinding.ActivityAddressWatchBinding
import io.horizontalsystems.ethereumkit.sample.modules.main.ShowTxType
import io.horizontalsystems.ethereumkit.sample.modules.main.TransactionsAdapter

class AddressWatchActivity : AppCompatActivity() {

    private lateinit var viewModel: AddressWatchViewModel
    private lateinit var binding: ActivityAddressWatchBinding 

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddressWatchBinding.inflate(layoutInflater) 
        setContentView(binding.root) 

        viewModel = ViewModelProvider(this).get(AddressWatchViewModel::class.java)

        binding.watchButton.setOnClickListener { 
            val address = binding.addressInput.text.toString() 
            viewModel.watchAddress(address)
        }

        binding.ethFilter.setOnClickListener { 
            viewModel.filterTransactions(true)
        }

        binding.tokenFilter.setOnClickListener { 
            viewModel.filterTransactions(false)
        }

        val transactionsAdapter = TransactionsAdapter()
        binding.transactionsRecyclerView.adapter = transactionsAdapter 
        binding.transactionsRecyclerView.layoutManager = LinearLayoutManager(this) 

        viewModel.transactions.observe(this) { txs -> 
            txs?.let { transactions ->
                transactionsAdapter.items = transactions
                transactionsAdapter.notifyDataSetChanged()
            }
        }

        viewModel.lastBlockHeight.observe(this) { height -> 
            height?.let {
                transactionsAdapter.lastBlockHeight = height
            }
        }

        viewModel.transactionsSyncingLiveData.observe(this) { syncing -> 
            binding.transactionSyncProgress.visibility = if (syncing) View.VISIBLE else View.GONE 
        }

        viewModel.showWarningLiveEvent.observe(this) { warning -> 
            Toast.makeText(this, warning, Toast.LENGTH_SHORT).show()
        }

        viewModel.showTxTypeLiveData.observe(this) { showTxType -> 
            when (showTxType) {
                ShowTxType.Eth -> {
                    binding.tokenFilter.setBackgroundColor(getColor(R.color.colorTab)) 
                    binding.ethFilter.setBackgroundColor(getColor(R.color.colorTabSelected)) 
                }
                ShowTxType.Erc20 -> {
                    binding.ethFilter.setBackgroundColor(getColor(R.color.colorTab)) 
                    binding.tokenFilter.setBackgroundColor(getColor(R.color.colorTabSelected)) 
                }
                null -> {
                    // Handle null case if necessary, or ensure showTxType is never null
                }
            }
        }
    }
}
