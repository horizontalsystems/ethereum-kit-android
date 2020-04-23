package io.horizontalsystems.ethereumkit.spv.core.storage

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.horizontalsystems.ethereumkit.network.Ropsten
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpvRoomStorageTest {

    private lateinit var blockHeaderDao: BlockHeaderDao
    private lateinit var db: SpvDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, SpvDatabase::class.java).build()
        blockHeaderDao = db.blockHeaderDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun blockHeaderDao() {
        val blockHeader = Ropsten().checkpointBlock

        blockHeaderDao.insert(blockHeader)

        val headerFromDb = blockHeaderDao.getByHashHex(blockHeader.hashHex)
        assertNotNull(headerFromDb)
        assertArrayEquals(blockHeader.hashHex, headerFromDb.hashHex)
    }

}
