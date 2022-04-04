package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.Single
import java.math.BigInteger

interface IBalanceManagerListener {
    fun onSyncBalanceSuccess(balance: BigInteger)
    fun onSyncBalanceError(error: Throwable)
}

interface IBalanceManager {
    var listener: IBalanceManagerListener?

    val balance: BigInteger?
    fun sync()
}

interface ITokenBalanceStorage {
    fun getBalance(): BigInteger?
    fun save(balance: BigInteger)
}

interface IDataProvider {
    fun getBalance(contractAddress: Address, address: Address): Single<BigInteger>
}
