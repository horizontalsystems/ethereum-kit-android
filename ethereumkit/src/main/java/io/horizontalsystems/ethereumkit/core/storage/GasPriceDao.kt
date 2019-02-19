package io.horizontalsystems.ethereumkit.core.storage

import android.arch.persistence.room.*
import io.horizontalsystems.ethereumkit.models.GasPriceRoom

@Dao
interface GasPriceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(gasPrice: GasPriceRoom)

    @Query("SELECT * FROM GasPriceRoom")
    fun getGasPrice(): GasPriceRoom?

    @Delete
    fun delete(gasPrice: GasPriceRoom)

    @Query("DELETE FROM GasPriceRoom")
    fun deleteAll()
}
