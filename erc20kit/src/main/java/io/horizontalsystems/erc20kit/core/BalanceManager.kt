package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigInteger

class BalanceManager(private val contractAddress: Address,
                     private val address: Address,
                     private val storage: ITokenBalanceStorage,
                     private val dataProvider: IDataProvider) : IBalanceManager {

    private val disposables = CompositeDisposable()

    override var listener: IBalanceManagerListener? = null

    override val balance: BigInteger?
        get() = storage.getBalance()

    override fun sync() {
        dataProvider.getBalance(contractAddress, address)
                .subscribeOn(Schedulers.io())
                .subscribe({ balance ->
                    storage.save(balance)
                    listener?.onSyncBalanceSuccess(balance)
                }, {
                    listener?.onSyncBalanceError(it)
                })
                .let {
                    disposables.add(it)
                }
    }

}
