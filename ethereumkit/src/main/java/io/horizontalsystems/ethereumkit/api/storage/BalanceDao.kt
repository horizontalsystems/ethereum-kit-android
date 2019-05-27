package io.horizontalsystems.ethereumkit.api.storage

import android.arch.persistence.room.*
import io.horizontalsystems.ethereumkit.api.models.EthereumBalance

@Dao
interface BalanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(ethereumBalance: EthereumBalance)

    @Query("SELECT * FROM EthereumBalance LIMIT 1")
    fun getBalance(): EthereumBalance?

    @Delete
    fun delete(rate: EthereumBalance)

    @Query("DELETE FROM EthereumBalance")
    fun deleteAll()
}
