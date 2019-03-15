package io.horizontalsystems.ethereumkit.core.storage

import android.arch.persistence.room.*
import io.horizontalsystems.ethereumkit.models.GasPrice

@Dao
interface GasPriceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(gasPrice: GasPrice)

    @Query("SELECT * FROM GasPrice")
    fun getGasPrice(): GasPrice?

    @Delete
    fun delete(gasPrice: GasPrice)

}
