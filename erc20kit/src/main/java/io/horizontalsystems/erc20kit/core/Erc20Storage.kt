package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.core.room.Erc20KitDatabase
import io.horizontalsystems.erc20kit.models.TokenBalance
import io.horizontalsystems.erc20kit.models.TransactionSyncOrder
import java.math.BigInteger

class Erc20Storage(
        database: Erc20KitDatabase
) : ITransactionStorage, ITokenBalanceStorage {

    private val tokenBalanceDao = database.tokenBalanceDao
    private val transactionSyncOrderDao = database.transactionSyncOrderDao

    // ITransactionStorage

    override fun getTransactionSyncOrder(): TransactionSyncOrder? {
        return transactionSyncOrderDao.getTransactionSyncOrder()
    }

    override fun save(transactionSyncOrder: TransactionSyncOrder) {
        transactionSyncOrderDao.insert(transactionSyncOrder)
    }

    // ITokenBalanceStorage

    override fun getBalance(): BigInteger? {
        return tokenBalanceDao.getBalance()?.value
    }

    override fun save(balance: BigInteger) {
        tokenBalanceDao.insert(TokenBalance(balance))
    }

}
