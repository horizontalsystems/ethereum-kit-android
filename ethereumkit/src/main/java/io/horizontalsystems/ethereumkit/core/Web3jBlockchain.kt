package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.core.storage.RoomStorage
import io.horizontalsystems.ethereumkit.network.EtherscanService
import io.horizontalsystems.hdwalletkit.HDWallet
import java.math.BigDecimal

class Web3jBlockchain(
        storage: RoomStorage,
        seed: ByteArray,
        testMode: Boolean,
        infuraKey: String,
        etherscanKey: String) : IBlockchain {

    override val ethereumGasLimit: Int = 21_000
    override val erc20GasLimit: Int = 100_000

    private val hdWallet = HDWallet(seed, if (testMode) 1 else 60)

    private val etherscanService = EtherscanService(testMode, etherscanKey)

    override val gasPrice: BigDecimal
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val ethereumAddress: String
        get() = hdWallet.address()

    override var listener: IBlockchainListener?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}

    override fun start() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun stop() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clear() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun register(contractAddress: String, decimal: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun unregister(contractAddress: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun send(toAddress: String, amount: BigDecimal, gasPrice: BigDecimal?, onSuccess: (() -> Unit)?, onError: (() -> Unit)?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun sendErc20(toAddress: String, contractAddress: String, amount: BigDecimal, gasPrice: BigDecimal?, onSuccess: (() -> Unit)?, onError: (() -> Unit)?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {

        val ETH_DECIMAL = 18.0
        const val GAS_LIMIT = 21_000
        const val GAS_LIMIT_ERC20 = 100_000

        private val etherRate = Math.pow(10.0, ETH_DECIMAL).toBigDecimal()

        val DEFAULT_GAS_PRICE = BigDecimal.valueOf(10_000_000_000) / etherRate
    }
}
