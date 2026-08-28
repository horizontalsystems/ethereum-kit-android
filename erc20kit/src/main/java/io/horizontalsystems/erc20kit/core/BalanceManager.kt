package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.ethereumkit.models.Address
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.math.BigInteger

class BalanceManager(private val contractAddress: Address,
                     private val address: Address,
                     private val storage: ITokenBalanceStorage,
                     private val dataProvider: IDataProvider) : IBalanceManager {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override var listener: IBalanceManagerListener? = null

    override val balance: BigInteger?
        get() = storage.getBalance()

    override fun sync() {
        scope.launch {
            try {
                val balance = dataProvider.getBalance(contractAddress, address)
                storage.save(balance)
                listener?.onSyncBalanceSuccess(balance)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                listener?.onSyncBalanceError(error)
            }
        }
    }

}
