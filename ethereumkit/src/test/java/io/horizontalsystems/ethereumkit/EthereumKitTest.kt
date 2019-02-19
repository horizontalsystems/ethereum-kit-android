package io.horizontalsystems.ethereumkit

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.ethereumkit.core.IBlockchain
import io.horizontalsystems.ethereumkit.core.IStorage
import io.horizontalsystems.ethereumkit.models.State
import io.horizontalsystems.ethereumkit.models.TransactionRoom
import io.reactivex.Single
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.math.BigDecimal

class EthereumKitTest {

    private val blockchain = mock(IBlockchain::class.java)
    private val storage = mock(IStorage::class.java)
    private val addressValidator = mock(AddressValidator::class.java)
    private val state = mock(State::class.java)
    private val listener = mock(EthereumKit.Listener::class.java)
    private lateinit var kit: EthereumKit

    private val ethereumAddress = "ether"

    @Before
    fun setUp() {
        RxBaseTest.setup()

        whenever(storage.getBalance(any())).thenReturn(null)
        whenever(blockchain.ethereumAddress).thenReturn(ethereumAddress)
        kit = EthereumKit(blockchain, storage, addressValidator, state)
        kit.listener = listener
    }

    @Test
    fun testInit_balance() {
        val balance = BigDecimal.valueOf(123.45)
        val lastBlockHeight = 123

        whenever(storage.getBalance(ethereumAddress)).thenReturn(balance)
        whenever(storage.getLastBlockHeight()).thenReturn(lastBlockHeight)

        kit = EthereumKit(blockchain, storage, addressValidator, state)

        verify(state).balance = balance
        verify(state).lastBlockHeight = lastBlockHeight
    }

    @Test
    fun testStart_notSyncing() {
        whenever(state.isSyncing).thenReturn(false)
        kit.start()
        verify(blockchain).start()
    }

    @Test
    fun testStart_alreadySyncing() {
        whenever(state.isSyncing).thenReturn(true)
        kit.start()
        verify(blockchain, never()).start()
    }

    @Test
    fun testStop() {
        kit.stop()
        verify(blockchain).stop()
    }

    @Test
    fun testClear() {
        kit.clear()
        verify(blockchain).clear()
        verify(state).clear()
        verify(storage).clear()
    }

    @Test
    fun testReceiveAddress() {
        val ethereumAddress = "eth_address"
        whenever(blockchain.ethereumAddress).thenReturn(ethereumAddress)
        Assert.assertEquals(ethereumAddress, kit.receiveAddress)
    }

    @Test
    fun testRegister() {
        val address = "address"
        val decimal = 4
        val listenerMock = mock(EthereumKit.Listener::class.java)
        whenever(state.hasContract(address)).thenReturn(false)
        kit.register(address, decimal, listenerMock)

        verify(state).add(contractAddress = address, decimal = decimal, listener = listenerMock)
        verify(blockchain).register(contractAddress = address, decimal = decimal)
    }

    @Test
    fun testRegister_alreadyExists() {
        val address = "address"
        val decimal = 4
        val listenerMock = mock(EthereumKit.Listener::class.java)

        whenever(state.hasContract(address)).thenReturn(true)
        kit.register(address, decimal, listenerMock)

        verify(state, never()).add(contractAddress = address, decimal = decimal, listener = listenerMock)
        verify(blockchain, never()).register(contractAddress = address, decimal = decimal)
    }

    @Test
    fun testUnregister() {
        val address = "address"
        kit.unregister(address)

        verify(state).remove(address)
        verify(blockchain).unregister(address)
    }

    @Test
    fun testValidateAddress() {
        val address = "address"

        kit.validateAddress(address)

        verify(addressValidator).validate(address)
    }

    @Test(expected = AddressValidator.AddressValidationException::class)
    fun testValidateAddress_failed() {
        val address = "address"

        whenever(addressValidator.validate(address)).thenThrow(AddressValidator.AddressValidationException(""))

        kit.validateAddress(address)
    }

    @Test
    fun testFee() {
        val gasLimit = 21_000
        val gasPrice = BigDecimal.valueOf(123)

        whenever(blockchain.gasPrice).thenReturn(gasPrice)
        whenever(blockchain.ethereumGasLimit).thenReturn(gasLimit)

        val expectedFee = gasPrice * gasLimit.toBigDecimal()

        val fee = kit.fee()

        Assert.assertEquals(expectedFee, fee)
    }

    @Test
    fun testFee_customGasPrice() {
        val gasLimit = 21_000
        val customGasPrice = BigDecimal.valueOf(23)

        whenever(blockchain.ethereumGasLimit).thenReturn(gasLimit)

        val expectedFee = customGasPrice * gasLimit.toBigDecimal()

        val fee = kit.fee(customGasPrice)

        Assert.assertEquals(expectedFee, fee)
    }

    @Test
    fun testTransactions() {
        val fromHash = "hash"
        val limit = 5
        val expectedResult = Single.just(listOf<TransactionRoom>())

        whenever(storage.getTransactions(fromHash, limit, null)).thenReturn(expectedResult)

        val result = kit.transactions(fromHash, limit)

        Assert.assertEquals(expectedResult, result)
    }

    @Test
    fun testSend_gasPriceNull() {
        val amount = BigDecimal.valueOf(23.4)
        val gasPrice = null
        val toAddress = "address"
        val onSuccess: (() -> Unit)? = { }
        val onError: (() -> Unit)? = { }

        kit.send(toAddress, amount, gasPrice, onSuccess, onError)

        verify(blockchain).send(toAddress, amount, gasPrice, onSuccess, onError)
    }

    @Test
    fun testSend_withCustomGasPrice() {
        val amount = BigDecimal.valueOf(23.4)
        val gasPrice = BigDecimal.valueOf(3.4)
        val toAddress = "address"
        val onSuccess: (() -> Unit)? = { }
        val onError: (() -> Unit)? = { }

        kit.send(toAddress, amount, gasPrice, onSuccess, onError)

        verify(blockchain).send(toAddress, amount, gasPrice, onSuccess, onError)
    }

    @Test
    fun testBalance_null() {
        whenever(state.balance).thenReturn(null)
        val result = kit.balance

        Assert.assertEquals(BigDecimal.valueOf(0.0), result)
    }

    @Test
    fun testBalance() {
        val balance = BigDecimal.valueOf(32.3)
        whenever(state.balance).thenReturn(balance)
        val result = kit.balance

        Assert.assertEquals(balance, result)
    }

    @Test
    fun testSyncState_null() {
        whenever(state.syncState).thenReturn(null)
        val result = kit.syncState

        Assert.assertEquals(EthereumKit.SyncState.NotSynced, result)
    }

    @Test
    fun testState_syncing() {
        whenever(state.syncState).thenReturn(EthereumKit.SyncState.Syncing)
        val result = kit.syncState

        Assert.assertEquals(EthereumKit.SyncState.Syncing, result)
    }

    //
    //Erc20
    //


    @Test
    fun testErc20Fee() {
        val erc20GasLimit = 100_000
        val gasPrice = BigDecimal.valueOf(123)

        whenever(blockchain.gasPrice).thenReturn(gasPrice)
        whenever(blockchain.erc20GasLimit).thenReturn(erc20GasLimit)

        val expectedFee = gasPrice * erc20GasLimit.toBigDecimal()

        val fee = kit.feeERC20()

        Assert.assertEquals(expectedFee, fee)
    }

    @Test
    fun testErc20Fee_customGasPrice() {
        val erc20GasLimit = 100_000
        val customGasPrice = BigDecimal.valueOf(23)

        whenever(blockchain.erc20GasLimit).thenReturn(erc20GasLimit)

        val expectedFee = customGasPrice * erc20GasLimit.toBigDecimal()

        val fee = kit.feeERC20(customGasPrice)

        Assert.assertEquals(expectedFee, fee)
    }

    @Test
    fun testErc20Balance_null() {
        val address = "address"

        whenever(state.balance(address)).thenReturn(null)

        val result = kit.balanceERC20(address)
        Assert.assertEquals(BigDecimal.valueOf(0.0), result)
    }

    @Test
    fun testErc20Balance() {
        val balance = BigDecimal.valueOf(23.03)
        val address = "address"
        whenever(state.balance(address)).thenReturn(balance)

        val result = kit.balanceERC20(address)
        Assert.assertEquals(balance, result)
    }

    @Test
    fun testState_null() {
        val address = "address"

        val result = kit.syncStateErc20(address)
        Assert.assertEquals(EthereumKit.SyncState.NotSynced, result)
    }

    @Test
    fun testState_synced() {
        val address = "address"

        whenever(state.state(address)).thenReturn(EthereumKit.SyncState.Synced)

        val result = kit.syncStateErc20(address)
        Assert.assertEquals(EthereumKit.SyncState.Synced, result)
    }

    @Test
    fun testErc20Transaction() {
        val address = "address"
        val fromHash = "hash"
        val limit = 5
        val expectedResult = Single.just(listOf<TransactionRoom>())

        whenever(storage.getTransactions(fromHash, limit, address)).thenReturn(expectedResult)

        val result = kit.transactionsERC20(address, fromHash, limit)

        Assert.assertEquals(expectedResult, result)
    }

    @Test
    fun testErc20Send_gasPriceNull() {
        val amount = BigDecimal.valueOf(23.4)
        val gasPrice = null
        val toAddress = "address"
        val contractAddress = "contAddress"
        val onSuccess: (() -> Unit)? = { }
        val onError: (() -> Unit)? = { }

        kit.sendERC20(toAddress, contractAddress, amount, gasPrice, onSuccess, onError)

        verify(blockchain).sendErc20(toAddress, contractAddress, amount, gasPrice, onSuccess, onError)
    }

    @Test
    fun testErc20Send_withCustomGasPrice() {
        val amount = BigDecimal.valueOf(23.4)
        val gasPrice = BigDecimal.valueOf(3.4)
        val toAddress = "address"
        val contractAddress = "contAddress"
        val onSuccess: (() -> Unit)? = { }
        val onError: (() -> Unit)? = { }

        kit.sendERC20(toAddress, contractAddress, amount, gasPrice, onSuccess, onError)

        verify(blockchain).sendErc20(toAddress, contractAddress, amount, gasPrice, onSuccess, onError)
    }

    @Test
    fun testOnUpdateLastBlockHeight() {
        val height = 34
        val erc20Listener = mock(EthereumKit.Listener::class.java)

        whenever(state.erc20Listeners).thenReturn(listOf(erc20Listener))
        kit.onUpdateLastBlockHeight(height)

        verify(state).lastBlockHeight = height
        verify(listener).onLastBlockHeightUpdate()
        verify(erc20Listener).onLastBlockHeightUpdate()
    }

    @Test
    fun testOnUpdateState() {
        val syncState = EthereumKit.SyncState.Syncing
        kit.onUpdateState(syncState)

        verify(state).syncState = syncState
    }

    @Test
    fun testOnUpdateErc20State() {
        val contractAddress= "address"
        val syncState = EthereumKit.SyncState.Syncing
        kit.onUpdateErc20State(syncState, contractAddress)

        verify(state).setSyncState(syncState, contractAddress)
        verify(state).listener(contractAddress)?.onStateUpdate()
    }

    @Test
    fun testOnUpdateBalance() {
        val balance = BigDecimal.valueOf(345)
        kit.onUpdateBalance(balance)
        verify(state).balance = balance
        verify(listener).onBalanceUpdate()
    }

    @Test
    fun testOnUpdateErc20Balance() {
        val contractAddress= "address"
        val balance = BigDecimal.valueOf(345)

        kit.onUpdateErc20Balance(balance, contractAddress)

        verify(state).setBalance(balance, contractAddress)
        verify(state).listener(contractAddress)?.onBalanceUpdate()
    }

    @Test
    fun testOnUpdateTransactions() {
        val transactions = listOf<TransactionRoom>()

        kit.onUpdateTransactions(transactions)
        verify(listener).onTransactionsUpdate(transactions)
    }

    @Test
    fun testOnUpdateErc20Transactions() {
        val contractAddress= "address"
        val transactions = listOf<TransactionRoom>()

        kit.onUpdateErc20Transactions(transactions, contractAddress)
        verify(state).listener(contractAddress)?.onTransactionsUpdate(transactions)
    }

}
