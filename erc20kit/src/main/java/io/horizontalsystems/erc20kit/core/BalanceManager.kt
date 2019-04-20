package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.models.TokenBalance
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigInteger

class BalanceManager(private val address: ByteArray,
                     private val storage: ITokenBalanceStorage,
                     private val dataProvider: IDataProvider) : IBalanceManager {

    private val disposables = CompositeDisposable()

    override var listener: IBalanceManagerListener? = null

    override fun balance(contractAddress: ByteArray): TokenBalance {
        return storage.getTokenBalance(contractAddress)
                ?: TokenBalance(contractAddress, BigInteger.ZERO, 0)
    }

    override fun sync(blockHeight: Long, contractAddress: ByteArray, balancePosition: Int) {
        dataProvider.getStorageValue(contractAddress, balancePosition, address, blockHeight)
                .subscribeOn(Schedulers.io())
                .subscribe({ value ->
                    val balance = TokenBalance(contractAddress, value, blockHeight)

                    storage.save(balance)
                    listener?.onBalanceUpdate(balance, contractAddress)
                    listener?.onSyncBalanceSuccess(contractAddress)
                }, {
                    listener?.onSyncBalanceError(contractAddress)
                }).let {
                    disposables.add(it)
                }
    }

    override fun clear() {
        storage.clearTokenBalances()
        disposables.clear()
    }
}