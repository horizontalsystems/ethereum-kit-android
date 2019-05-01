package io.horizontalsystems.erc20kit.core.room

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import io.horizontalsystems.erc20kit.models.TokenBalance

@Dao
interface TokenBalanceDao {

    @Query("SELECT * FROM TokenBalance LIMIT 1")
    fun getBalance(): TokenBalance?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(balance: TokenBalance)

    @Query("DELETE FROM TokenBalance")
    fun deleteAll()
}
