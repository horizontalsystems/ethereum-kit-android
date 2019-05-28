package io.horizontalsystems.ethereumkit.spv.core.storage

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import io.horizontalsystems.ethereumkit.spv.models.AccountState

@Dao
interface AccountStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(accountState: AccountState)

    @Query("SELECT * FROM AccountState LIMIT 1")
    fun getAccountState(): AccountState?
}
