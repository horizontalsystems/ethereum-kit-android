package io.horizontalsystems.ethereumkit.sample.modules.main

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.ethereumkit.sample.R
import io.horizontalsystems.ethereumkit.sample.core.TransactionRecord
import kotlinx.android.synthetic.main.fragment_transactions.*
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*

class TransactionsFragment : Fragment() {

    private lateinit var viewModel: MainViewModel

    private val transactionsAdapter = TransactionsAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionsRecyclerView.adapter = transactionsAdapter
        transactionsRecyclerView.layoutManager = LinearLayoutManager(context)

        viewModel = activity?.let { ViewModelProvider(it).get(MainViewModel::class.java) } ?: return

        viewModel.transactions.observe(viewLifecycleOwner, Observer { txs ->
            txs?.let { transactions ->
                transactionsAdapter.items = transactions
                transactionsAdapter.notifyDataSetChanged()
            }
        })

        viewModel.lastBlockHeight.observe(viewLifecycleOwner, Observer { height ->
            height?.let {
                transactionsAdapter.lastBlockHeight = height
            }
        })

        viewModel.showTxTypeLiveData.observe(viewLifecycleOwner) { showTxType ->
            context?.let { ctx ->
                when (showTxType) {
                    ShowTxType.Eth -> {
                        ethFilter.setBackgroundColor(ctx.getColor(R.color.colorSelected))
                        tokenFilter.setBackgroundColor(Color.WHITE)
                    }
                    ShowTxType.Erc20 -> {
                        tokenFilter.setBackgroundColor(ctx.getColor(R.color.colorSelected))
                        ethFilter.setBackgroundColor(Color.WHITE)
                    }
                    else -> {
                    }
                }
            }

        }

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
        - Block Number: ${tx.blockHeight ?: "n/a"}
        - Tx Index: ${tx.transactionIndex}
        - Time: ${format.format(Date(tx.timestamp * 1000))}
        - From: ${tx.from ?: "n/a"}
        - To: ${tx.to ?: "n/a"}
        - Amount: ${tx.amount?.let { readableAmount(it) }} ETH
        - isError: ${tx.isError}
        - Decoration: ${tx.decoration}
        """

        if (lastBlockHeight > 0)
            value += "\n- Confirmations: ${tx.blockHeight?.let { lastBlockHeight - it + 1 } ?: 0}"

        summary.text = value.trimIndent()
    }

    private fun readableNumber(amount: BigInteger, tokenDecimal: Int = 18): String {
        val decimal = amount.toBigDecimal().movePointLeft(tokenDecimal)
        return readableAmount(decimal)
    }

    private fun readableAmount(value: BigDecimal): String {
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            return "0"
        }
        return value
            .setScale(8, RoundingMode.HALF_EVEN)
            .stripTrailingZeros()
            .toPlainString()
    }
}
