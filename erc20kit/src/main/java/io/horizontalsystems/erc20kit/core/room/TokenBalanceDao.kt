package io.horizontalsystems.erc20kit.core.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.erc20kit.models.TokenBalance

@Dao
interface TokenBalanceDao {

    @Query("SELECT * FROM TokenBalance LIMIT 1")
    fun getBalance(): TokenBalance?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(balance: TokenBalance)

}
