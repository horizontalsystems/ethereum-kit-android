package io.horizontalsystems.ethereumkit.core.storage

import android.arch.persistence.room.*
import io.horizontalsystems.ethereumkit.models.BalanceRoom

@Dao
interface BalanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(balance: BalanceRoom)

    @Query("SELECT * FROM BalanceRoom WHERE address = :address")
    fun getBalance(address: String): BalanceRoom?

    @Delete
    fun delete(rate: BalanceRoom)

    @Query("DELETE FROM BalanceRoom")
    fun deleteAll()
}
