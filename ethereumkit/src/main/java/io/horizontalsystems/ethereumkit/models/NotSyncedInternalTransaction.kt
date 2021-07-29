package io.horizontalsystems.ethereumkit.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/*
 NotSyncedInternalTransaction holds a hash of transactions for which internal transactions need to be synced.
 These internal transactions may not be related to the user's address and they are needed to learn about the
 transactions' final state.

*/

@Entity
class NotSyncedInternalTransaction(@PrimaryKey val hash: ByteArray, var retryCount: Int)
