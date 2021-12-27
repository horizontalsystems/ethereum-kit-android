package io.horizontalsystems.ethereumkit.sample.modules.main

import androidx.lifecycle.Observer
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.erc20kit.decorations.ApproveEventDecoration
import io.horizontalsystems.erc20kit.decorations.ApproveMethodDecoration
import io.horizontalsystems.erc20kit.decorations.TransferEventDecoration
import io.horizontalsystems.erc20kit.decorations.TransferMethodDecoration
import io.horizontalsystems.ethereumkit.decorations.ContractEventDecoration
import io.horizontalsystems.ethereumkit.decorations.RecognizedMethodDecoration
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import io.horizontalsystems.ethereumkit.sample.Configuration
import io.horizontalsystems.ethereumkit.sample.R
import io.horizontalsystems.ethereumkit.sample.core.TransactionRecord
import io.horizontalsystems.oneinchkit.decorations.OneInchSwapMethodDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchUnoswapMethodDecoration
import io.horizontalsystems.uniswapkit.decorations.SwapMethodDecoration
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

        viewModel.showTxTypeLiveData.observe(viewLifecycleOwner, { showTxType ->
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

        })

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
        - Inter Tx Index: ${tx.interTransactionIndex}
        - Time: ${format.format(Date(tx.timestamp * 1000))}
        - From: ${tx.from.address}
        - To: ${tx.to.address}
        - Amount: ${readableAmount(tx.amount)} ETH
        - isError: ${tx.isError}
        - Decoration: ${tx.mainDecoration?.let { stringify(it, tx) } ?: "n/a"}
        - EventDecorations: ${stringify(tx.eventsDecorations, tx)}
        """

        if (lastBlockHeight > 0)
            value += "\n- Confirmations: ${tx.blockHeight?.let { lastBlockHeight - it + 1 } ?: 0}"

        summary.text = value.trimIndent()
    }

    private fun stringify(eventsDecorations: List<ContractEventDecoration>, transactionRecord: TransactionRecord): String {
        return eventsDecorations.map { event ->
            when (event) {
                is TransferEventDecoration -> {
                    val coin = Configuration.erc20Tokens.firstOrNull { it.contractAddress.eip55 == event.contractAddress.eip55 }?.name
                            ?: "n/a"
                    val fromAddress = event.from.eip55.take(6)
                    val toAddress = event.to.eip55.take(6)
                    return "${readableNumber(event.value)} $coin ($fromAddress -> $toAddress)"
                }
                is ApproveEventDecoration -> {
                    val coin = Configuration.erc20Tokens.firstOrNull { it.contractAddress.eip55 == event.contractAddress.eip55 }?.name
                            ?: "n/a"
                    val owner = event.owner.eip55.take(6)
                    val spender = event.spender.eip55.take(6)
                    return "${readableNumber(event.value)} $coin ($owner - approved -> $spender)"
                }
                else -> return "unknown event"
            }
        }.joinToString("\n")

    }

    private fun stringify(decoration: TransactionDecoration, transaction: TransactionRecord): String {
        val coinName = Configuration.erc20Tokens.firstOrNull { it.contractAddress.hex == transaction.to.address }?.name
                ?: "n/a"
        val fromAddress = transaction.from.address?.take(6)

        return when (decoration) {
            is SwapMethodDecoration -> {
                "${amountIn(decoration.trade)} ${stringify(decoration.tokenIn)} <-> ${amountOut(decoration.trade)} ${stringify(decoration.tokenOut)}"
            }
            is TransferMethodDecoration -> {
                "${readableNumber(decoration.value)} $coinName $fromAddress -> ${decoration.to.eip55.take(6)}"
            }
            is ApproveMethodDecoration -> {
                "${readableNumber(decoration.value)} $coinName approved"
            }
            is RecognizedMethodDecoration -> {
                "${decoration.method} ${decoration.arguments.size} arguments"
            }
            is OneInchSwapMethodDecoration -> {
                "1inch swap ${decoration.fromAmount} ${decoration.fromToken} -> ${decoration.toAmount ?: decoration.toAmountMin} ${decoration.toToken}"
            }
            is OneInchUnoswapMethodDecoration -> {
                "1inch unoswap ${decoration.fromAmount} ${decoration.fromToken} -> ${decoration.toAmount ?: decoration.toAmountMin} ${decoration.toToken}"
            }
            else -> "contract call"
        }
    }

    private fun stringify(token: SwapMethodDecoration.Token): String {
        return when (token) {
            is SwapMethodDecoration.Token.EvmCoin -> "ETH"
            is SwapMethodDecoration.Token.Eip20Coin -> Configuration.erc20Tokens.firstOrNull { it.contractAddress.eip55 == token.address.eip55 }?.code
                    ?: "n/a"
        }
    }

    private fun amountIn(trade: SwapMethodDecoration.Trade): String {
        val amount: BigInteger = when (trade) {
            is SwapMethodDecoration.Trade.ExactIn -> trade.amountIn
            is SwapMethodDecoration.Trade.ExactOut -> trade.amountIn ?: trade.amountInMax
        }

        return readableNumber(amount)
    }

    private fun amountOut(trade: SwapMethodDecoration.Trade): String {
        val amount: BigInteger = when (trade) {
            is SwapMethodDecoration.Trade.ExactIn -> trade.amountOut ?: trade.amountOutMin
            is SwapMethodDecoration.Trade.ExactOut -> trade.amountOut
        }

        return readableNumber(amount)
    }

    private fun readableNumber(amount: BigInteger): String {
        val decimal = amount.toBigDecimal()
                .movePointLeft(18)
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
