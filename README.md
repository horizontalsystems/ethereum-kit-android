# EthereumKit

`EthereumKit` is a native(Kotlin) toolkit for EVM compatible networks. It's implemented and used by [Unstoppable Wallet](https://github.com/horizontalsystems/unstoppable-wallet-android), a multi-currency crypto wallet. It implements a lot of features of the DeFi world natively *(no need for WalletConnect)* out-of-the-box.

## Core Features

- [x] Restore with **mnemonic phrase**, **BIP39 Seed**, **EVM private key**, or simply an **Ethereum address**
- [x] Local storage of account data (ETH, Token/NFT balance and transactions)
- [x] Synchronization over **HTTP/WebSocket**
- [x] **Watch accounts**. Restore with any address
- [x] Ethereum Name Service **(ENS) support**
- [x] **EIP-1559** Gas Prices with live updates
- [x] Reactive-functional API by [`RxAndroid`](https://github.com/ReactiveX/RxAndroid)
- [x] Implementation of Ethereum's JSON-RPC API
- [x] Support for Infura and Etherscan
- [x] Can be extended to natively support any smart contract
- [x] EIP20 token standard support
- [x] EIP721 and EIP1155 non-fungible tokens(NFT)
- [x] Uniswap (PancakeSwap, QuickSwap, Trader Joe) support
- [x] 1Inch support

## Blockchains supported

Any EVM blockchain that supports the Ethereum's RPC API and has an Etherscan-like block explorer can be easily integrated to your wallet using `EthereumKit`. The following blockchains are currently integrated to `Unstoppable Wallet`:

- Ethereum
- Binance Smart Chain
- Polygon
- ArbitrumOne
- Optimism
- Avalanche C-Chain


## Usage

### Initialization

First you need to initialize an `EthereumKit` instance

```kotlin
val context = Application()
val address = Address("0x..your..address")

val evmKit = EthereumKit.getInstance(
    context,
    address,
    Chain.Ethereum,
    RpcSource.ethereumInfuraHttp("projectId", "projectSecret"),
    TransactionSource.etherscanApi("apiKey"),
    "unique_wallet_id"
)
```

### Starting and Stopping

`EthereumKit` instance requires to be started with `start` command. This start the process of synchronization with the blockchain state.

```kotlin
evmKit.start()
evmKit.stop()
```

### Get wallet data

You can get `account state`, `last block height`, `sync state`, `transactions sync state` and some others synchronously:

```kotlin
evmKit.accountState?.let { state ->
    state.balance
    state.nonce
}

evmKit.lastBlockHeight
```

You also can subscribe to Rx observables of those and more:

```kotlin
evmKit.accountStateFlowable.subscribe { state -> println("balance: ${state.balance}); nonce: ${state.nonce}") }
evmKit.lastBlockHeightFlowable.subscribe { height -> println(height) }
evmKit.syncStateFlowable.subscribe { state -> println(state) }
evmKit.transactionsSyncStateFlowable.subscribe { state -> println(state) }

// Subscribe to ETH transactions synced by the kit
evmKit.getFullTransactionsFlowable(listOf(listOf("ETH"))).subscribe { transactions -> 
    println(transactions.size) 
}

// Subscribe to all EVM transactions
evmKit.allTransactionsFlowable.subscribe { transactionsPair -> 
    println(transactionsPair.first.size) 
}
```

### Send Transaction

To send a transaction you need a Signer object. Here's how you can create it using Mnemonic seed phrase:

```kotlin
val seed = Mnemonic().toSeed(listOf("mnemonic", "phrase"), "passphrase_if_exists'")
val signer = Signer.getInstance(seed, Chain.Ethereum)
```


Now you can use it to sign an Ethereum transaction:


```kotlin
val toAddress = Address("0x..recipient..address..here")
val amount = BigInteger("100000000000000000")                         // 0.1 ETH in WEIs
val gasPrice = GasPrice.Legacy(50_000_000_000)

// Construct TransactionData which is the key payload of any EVM transaction
val transactionData = ethereumKit.transferTransactionData(toAddress, amount)

// Estimate gas for the transaction
val estimateGasSingle = ethereumKit.estimateGas(transactionData, gasPrice)

// Generate a raw transaction which is ready to be signed. This step also synchronizes the nonce
val rawTransactionSingle = estimateGasSingle.flatMap { estimateGasSingle ->
    ethereumKit.rawTransaction(transactionData, gasPrice, estimateGasSingle)
}

val sendSingle = rawTransactionSingle.flatMap { rawTransaction ->
    // Sign the transaction
    val signature = signer.signature(rawTransaction)

    // Send the transaction to RPC node
    ethereumKit.send(rawTransaction, signature)
}

// This step is needed for Rx reactive code to run
val disposables = CompositeDisposable()

sendSingle.subscribe { fullTransaction ->
    // ethereumKit.send returns FullTransaction object that contains transaction and a transaction decoration
    val transaction = fullTransaction.transaction

    println("Transaction sent: ${transaction.hash.toHexString()}")
    println("To: ${transaction.to?.let { it.eip55 }}")
    println("Amount: ${transaction.value?.let { it.toString(10) }}")
}.let {
    disposables.add(it)
}
```

### Get ETH transactions

The following code retrieves the transactions that have `ETH` coin incoming or outgoing, including the transactions where `ETH` is received in internal transactions.

```kotlin
ethereumKit.getFullTransactionsAsync(listOf(listOf("ETH")))
        .subscribe { fullTransactions ->
            for (fullTransaction in fullTransactions) {
                println("Transaction hash: ${fullTransaction.transaction.hash.toHexString()}")

                when (val decoration = fullTransaction.decoration) {
                    is IncomingDecoration -> {
                        println("From: ${decoration.from.eip55}")
                        println("Amount: ${decoration.value.toString(10)}")
                    }

                    is OutgoingDecoration -> {
                        println("To: ${decoration.to.eip55}")
                        println("Amount: ${decoration.value.toString(10)}")
                    }
                    
                    else -> {}
                }
            }
        }.let {
            disposables.add(it)
        }
```

## EIP20 tokens

### Initialization

```kotlin
val contractAddress = Address("0x..token..contract..address..")
val erc20Kit = Erc20Kit.getInstance(context, ethereumKit, contractAddress)

// Decorators are needed to detect transactions as `Erc20` transfer/approve transactions
Erc20Kit.addTransactionSyncer(ethereumKit)
        
// Erc20 transactions syncer is needed to pull Eip20 transfer transactions from Etherscan
Erc20Kit.addDecorators(ethereumKit)
```

### Get token balance

```kotlin
erc20Kit.balance?.let { balance ->
    println(balance.toString(10))
}
```

### Send Erc20 transfer transaction

```kotlin
val toAddress = Address("0x..recipient..address..here")
val amount = BigInteger("100000000000000000")
val gasPrice = GasPrice.Legacy(50_000_000_000)

// Construct TransactionData which calls a `Transfer` method of the EIP20 compatible smart contract
val transactionData = erc20Kit.buildTransferTransactionData(toAddress, amount)

ethereumKit.estimateGas(transactionData, gasPrice)
        .flatMap { estimateGasSingle ->
            ethereumKit.rawTransaction(transactionData, gasPrice, estimateGasSingle)
        }
        .flatMap { rawTransaction ->
            val signature = signer.signature(rawTransaction)
            ethereumKit.send(rawTransaction, signature)
        }
        .subscribe { fullTransaction ->
            println("Transaction sent: ${fullTransaction.transaction.hash.toHexString()}")

            val decoration = fullTransaction.decoration as? OutgoingDecoration ?: return@subscribe
                    
            println("To: ${decoration.to.eip55}")
            println("Amount: ${decoration.value.toString(10)}")
        }.let {
            disposables.add(it)
        }
```


### Get Erc20 transactions

```kotlin
ethereumKit.getFullTransactionsAsync(listOf(listOf(contractAddress.eip55)))
        .subscribe { fullTransactions ->
            for (fullTransaction in fullTransactions) {
                println("Transaction sent: ${fullTransaction.transaction.hash.toHexString()}")

                when (val decoration = fullTransaction.decoration) {
                    is IncomingDecoration -> {
                        println("From: ${decoration.from.eip55}")
                        println("Amount: ${decoration.value.toString(10)}")
                    }

                    is OutgoingDecoration -> {
                        println("To: ${decoration.to.eip55}")
                        println("Amount: ${decoration.value.toString(10)}")
                    }

                    else -> {}
                }
            }
        }.let {
            disposables.add(it)
        }
```

## Uniswap

### Initialization

```kotlin
val uniswapKit = UniswapKit.getInstance(ethereumKit)

// Decorators are needed to detect and decorate transactions as `Uniswap` transactions
UniswapKit.addDecorators(ethereumKit)
```

### Send sample swap transaction

```kotlin
// Sample swap data
val tokenIn = uniswapKit.etherToken()
val tokenOut = uniswapKit.token(Address("0x..token..address"), 18)
val amount = BigDecimal(1)
val gasPrice = GasPrice.Legacy(50_000_000_000)

// Get SwapData. SwapData is a list of pairs available in Uniswap smart contract at the moment
uniswapKit.swapData(tokenIn, tokenOut)
        .map { swapData ->
            // Get TradeData. TradeData is the best swap route evaluated by UniswapKit
            val tradeData = uniswapKit.bestTradeExactIn(swapData, amount)

            // Convert TradeData to EvmKit TransactionData
            uniswapKit.transactionData(tradeData)
        }
        .flatMap { transactionData ->
            ethereumKit.estimateGas(transactionData, gasPrice)
                    .flatMap { estimateGasSingle ->
                        ethereumKit.rawTransaction(transactionData, gasPrice, estimateGasSingle)
                    }
        }
        .flatMap { rawTransaction ->
            val signature = signer.signature(rawTransaction)
            ethereumKit.send(rawTransaction, signature)
        }
        .subscribe { fullTransaction ->
            println("Transaction sent: ${fullTransaction.transaction.hash.toHexString()}")
        }.let {
            disposables.add(it)
        }
```

### ExactIn/ExactOut

With `UniswapKit` you can build swap transaction that either has an exact `In` or exact `Out` amount. That is, if you want to swap exactly 1 ETH to USDT, you get `TradeData` using `bestTradeExactIn` method. Similarly, if you want to swap ETH to USDT and you want to get exactly 1000 USDT, then you get `TradeData` using `bestTradeExactOut`

### Trade Options

`UniswapKit` supports `Price Impact/Deadline/Recipient` options. You can set them in `TradeOptions` object passed to `bestTradeExactIn/bestTradeExactOut` methods. Please, look at official Uniswap app documentation to learn about those options.


## 1Inch

`OneInchKit` is an extension that wraps interactions with [`1Inch API`](https://docs.1inch.io/docs/aggregation-protocol/api/swagger/).

### Initialization


```kotlin
val oneInchKit = OneInchKit.getInstance(ethereumKit)
OneInchKit.addDecorators(ethereumKit)
```

### Sample code to get swap data from 1Inch API, sign it and send to RPC node

```kotlin
// Sample swap data
val tokenFromAddress = Address("0x..from..token..address")
val tokenToAddress = Address("0x..to..token..address")
val amount = BigInteger("100000000000000000")
val gasPrice = GasPrice.Legacy(50_000_000_000)

// Get Swap object, evaluated transaction data by 1Inch aggregator
oneInchKit.getSwapAsync(
        fromToken = tokenFromAddress,
        toToken = tokenToAddress,
        amount = amount,
        slippagePercentage = 1F,
        recipient = null,
        gasPrice = gasPrice
)
        .flatMap { swap ->
            val tx = swap.transaction
            val transactionData = TransactionData(tx.to, tx.value, tx.data)

            ethereumKit.rawTransaction(transactionData, gasPrice, tx.gasLimit)
        }
        .flatMap { rawTransaction ->
            val signature = signer.signature(rawTransaction)
            ethereumKit.send(rawTransaction, signature)
        }
        .subscribe { fullTransaction ->
            println("Transaction sent: ${fullTransaction.transaction.hash.toHexString()}")
        }.let {
            disposables.add(it)
        }
```

## NFTs

NftKit support EIP721 and EIP1155

### Initialization

```kotlin
val nftKit = NftKit.getInstance(App.instance, ethereumKit)

nftKit.addEip721Decorators()
nftKit.addEip1155Decorators()

nftKit.addEip721TransactionSyncer()
nftKit.addEip1155TransactionSyncer()
```

### Get NFTs owned by the user

```kotlin
val nftBalances = nftKit.nftBalances

for (nftBalance in nftBalances) {
    println("---- ${nftBalance.balance} pieces of ${nftBalance.nft.tokenName} ---")
    println("Contract Address: ${nftBalance.nft.contractAddress.eip55}")
    println("TokenID: ${nftBalance.nft.tokenId.toString(10)}")
}
```


### Send an NFT

```kotlin
val nftContractAddress = Address("0x..contract..address")
val tokenId = BigInteger("234123894712031638516723498")
val to = Address("0x..recipient..address")
val gasPrice = GasPrice.Legacy(50_000_000_000)

// Construct a TransactionData
val transactionData = nftKit.transferEip721TransactionData(nftContractAddress, to, tokenId)

ethereumKit.estimateGas(transactionData, gasPrice)
        .flatMap { estimateGasSingle ->
            ethereumKit.rawTransaction(transactionData, gasPrice, estimateGasSingle)
        }
        .flatMap { rawTransaction ->
            val signature = signer.signature(rawTransaction)
            ethereumKit.send(rawTransaction, signature)
        }
        .subscribe { fullTransaction ->
            println("Transaction sent: ${fullTransaction.transaction.hash.toHexString()}")
        }.let {
            disposables.add(it)
        }
```


## Extending

### Smart contract call

In order to send an EVM smart contract call transaction, you need to create an instance of `TransactionData` object. Then you can sign and send it as seen above.


## Prerequisites
* JDK >= 11
* Android 8 (minSdkVersion 26) or greater

## Installation
Add the JitPack to module build.gradle
```
repositories {
    maven { url 'https://jitpack.io' }
}
```
Add the following dependency to your build.gradle file:
```
dependencies {
    implementation 'com.github.horizontalsystems:ethereum-kit-android:master-SNAPSHOT'
}
```

## Example App

All features of the library are used in example project. It can be referred as a starting point for usage of the library.
* [Example App](https://github.com/horizontalsystems/ethereum-kit-android/tree/master/app)

## License

The `EthereumKit` is open source and available under the terms of the [MIT License](https://github.com/horizontalsystems/ethereum-kit-android/blob/master/LICENSE)