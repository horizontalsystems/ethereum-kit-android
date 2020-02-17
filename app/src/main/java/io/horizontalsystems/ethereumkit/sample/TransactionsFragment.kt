package io.horizontalsystems.ethereumkit.sample

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.horizontalsystems.ethereumkit.sample.core.TransactionRecord
import java.text.SimpleDateFormat
import java.util.*

class TransactionsFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var transactionsRecyclerView: RecyclerView

    private val transactionsAdapter = TransactionsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(MainViewModel::class.java)

            viewModel.transactions.observe(this, Observer { txs ->
                txs?.let { transactions ->
                    transactionsAdapter.items = transactions
                    transactionsAdapter.notifyDataSetChanged()
                }
            })

            viewModel.lastBlockHeight.observe(this, Observer { height ->
                height?.let {
                    transactionsAdapter.lastBlockHeight = height
                }
            })
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_transactions, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionsRecyclerView = view.findViewById(R.id.transactions)
        transactionsRecyclerView.adapter = transactionsAdapter
        transactionsRecyclerView.layoutManager = LinearLayoutManager(context)

        val ethFilter = view.findViewById<TextView>(R.id.ethFilter)
        val tokenFilter = view.findViewById<TextView>(R.id.tokenFilter)

        ethFilter.setOnClickListener {
            viewModel.filterTransactions(true)
        }

        tokenFilter.setOnClickListener {
            viewModel.filterTransactions(false)
        }
    }
}

class TransactionsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var items = listOf<TransactionRecord>()
    var lastBlockHeight: Long = 0

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            ViewHolderTransaction(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_transaction, parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderTransaction -> holder.bind(items[position], itemCount - position, lastBlockHeight)
        }
    }
}

class ViewHolderTransaction(private val containerView: View) : RecyclerView.ViewHolder(containerView) {
    private val summary = containerView.findViewById<TextView>(R.id.summary)!!

    fun bind(tx: TransactionRecord, index: Int, lastBlockHeight: Long) {
        containerView.setBackgroundColor(if (index % 2 == 0)
            Color.parseColor("#dddddd") else
            Color.TRANSPARENT
        )

        val format = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")

        var value = """
            - #$index
            - Tx Hash: ${tx.transactionHash}
            - Block Number: ${tx.blockHeight ?: "N/A"}
            - Tx Index: ${tx.transactionIndex}
            - Inter Tx Index: ${tx.interTransactionIndex}
            - Time: ${format.format(Date(tx.timestamp * 1000))}
            - From: ${tx.from.address}
            - To: ${tx.to.address}
            - Amount: ${tx.amount.stripTrailingZeros()}
            - isError: ${tx.isError}
        """

        if (lastBlockHeight > 0)
            value += "\n- Confirmations: ${tx.blockHeight?.let { lastBlockHeight - it + 1 } ?: 0}"

        summary.text = value.trimIndent()
    }
}
