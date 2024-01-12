package io.horizontalsystems.ethereumkit.sample.modules.uniswapV3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.ethereumkit.sample.core.Erc20Adapter
import io.horizontalsystems.ethereumkit.sample.core.EthereumAdapter
import io.horizontalsystems.ethereumkit.sample.modules.main.GasPriceHelper
import io.horizontalsystems.ethereumkit.sample.modules.main.MainViewModel
import io.horizontalsystems.uniswapkit.models.TradeType
import kotlinx.android.synthetic.main.activity_main.*
import java.math.BigDecimal

class UniswapV3Fragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        toolbar?.title = "Uniswap V3"

        val mainViewModel = activity?.let { ViewModelProvider(it)[MainViewModel::class.java] } ?: return null

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                UniswapV3Screen(
                    mainViewModel.ethereumKit,
                    mainViewModel.erc20Adapter,
                    mainViewModel.ethereumAdapter,
                    mainViewModel.gasPriceHelper,
                    mainViewModel.signer,
                    mainViewModel.rpcSource
                )
            }
        }
    }
}

@Composable
fun UniswapV3Screen(
    ethereumKit: EthereumKit,
    erc20Adapter: Erc20Adapter,
    ethereumAdapter: EthereumAdapter,
    gasPriceHelper: GasPriceHelper,
    signer: Signer,
    rpcSource: RpcSource
) {
    val factory = UniswapV3ViewModel.Factory(
        ethereumKit,
        erc20Adapter,
        ethereumAdapter,
        gasPriceHelper,
        signer,
        rpcSource
    )
    val viewModel = viewModel<UniswapV3ViewModel>(factory = factory)

    val swapState = viewModel.swapState

    var amountIn by remember {
        mutableStateOf<BigDecimal?>(null)
    }
    var amountOut by remember {
        mutableStateOf<BigDecimal?>(null)
    }

    LaunchedEffect(swapState) {
        amountOut = swapState.amountOut
        amountIn = swapState.amountIn
    }

    MaterialTheme {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(text = "Allowance: ${swapState.allowance?.toPlainString() ?: "n/a"}")
            Spacer(modifier = Modifier.height(12.dp))

            AmountInput(
                title = "Amount In (${viewModel.fromToken?.code ?: "ETH"})",
                initial = amountIn
            ) {
                viewModel.onChangeAmountIn(it)
            }
            Spacer(modifier = Modifier.height(12.dp))
            AmountInput(
                title = "Amount Out (${viewModel.toToken?.code ?: "ETH"})",
                initial = amountOut
            ) {
                viewModel.onChangeAmountOut(it)
            }
            Spacer(modifier = Modifier.height(12.dp))
            AnimatedVisibility(visible = swapState.loading) {
                CircularProgressIndicator()
            }
            Spacer(modifier = Modifier.height(12.dp))
            swapState.error?.let {
                Text(text = "Error: ${it.message} ${it.javaClass.simpleName}", color = Color.Red)
            }

            if (swapState.error == null && swapState.amountIn != null && swapState.amountOut != null) {
                if (swapState.amountIn > swapState.allowance) {
                    Button(onClick = viewModel::approve) {
                        Text(text = "Approve")
                    }
                } else {
                    Button(onClick = viewModel::swap) {
                        Text(text = "Swap")
                    }
                }
            }
        }
    }
}

@Composable
fun AmountInput(
    title: String,
    initial: BigDecimal?,
    onValueChange: (BigDecimal?) -> Unit,
) {
    var amountText by remember(initial) {
        mutableStateOf(initial?.toPlainString() ?: "")
    }
    Text(text = title)
    TextField(
        value = amountText,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        onValueChange = {
            amountText = it

            onValueChange.invoke(it.toBigDecimalOrNull())
        }
    )
}
