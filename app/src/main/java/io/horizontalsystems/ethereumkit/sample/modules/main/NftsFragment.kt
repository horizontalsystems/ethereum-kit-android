package io.horizontalsystems.ethereumkit.sample.modules.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.android.synthetic.main.activity_main.*

class NftsFragment : Fragment() {
    private lateinit var mainViewModel: MainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        toolbar?.title = "Nfts"

        mainViewModel = activity?.let { ViewModelProvider(it)[MainViewModel::class.java] } ?: return null

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                MaterialTheme {
                    val viewModel = viewModel<NftsViewModel>(factory = NftsViewModelFactory(mainViewModel.ethereumKit))

                    val nftBalances by viewModel.nftBalancesFlow.collectAsState(initial = listOf())

                    var textState by rememberSaveable("", stateSaver = TextFieldValue.Saver) {
                        mutableStateOf(TextFieldValue(""))
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
/*
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                modifier = Modifier
                                    .weight(1f),
                                value = textState,
                                textStyle = MaterialTheme.typography.body1,
                                onValueChange = {
                                    textState = it
                                },
                                label = { Text("Enter Address") }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {

                                }) {
                                Text(text = "OK")
                            }
                        }
*/
                        LazyColumn {
                            nftBalances.forEach {
                                item {
                                    Divider()
                                    Row(modifier = Modifier) {
                                        SelectionContainer {
                                            Text(
                                                text = "Name: ${it.nft.tokenName}\n" +
                                                        "Type: ${it.nft.type}\n" +
                                                        "Contract: ${it.nft.contractAddress.hex}\n" +
                                                        "ID: ${it.nft.tokenId}\n" +
                                                        "Balance: ${it.balance}\n" +
                                                        "Synced: ${it.synced}"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
