package io.horizontalsystems.ethereumkit.sample.core

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.sample.FeePriority
import io.reactivex.Single

class Erc20Adapter(ethereumKit: EthereumKit, private val contractAddress: String, decimal: Int) : BaseAdapter(ethereumKit, decimal) {

    init {
        ethereumKit.register(contractAddress, this)
    }

    override val syncState: EthereumKit.SyncState
        get() = ethereumKit.syncStateErc20(contractAddress)


    override val balanceString: String?
        get() = ethereumKit.balanceERC20(contractAddress)

    override fun sendSingle(address: String, amount: String, feePriority: FeePriority): Single<Unit> {
        return ethereumKit.sendERC20(
                toAddress = address,
                contractAddress = contractAddress,
                amount = amount,
                gasPrice = 5_000_000_000).map { Unit }
    }

    override fun transactionsObservable(hashFrom: String?, limit: Int?): Single<List<EthereumTransaction>> {
        return ethereumKit.transactionsERC20(contractAddress, hashFrom, limit)
    }

}
