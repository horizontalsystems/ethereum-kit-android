package io.horizontalsystems.ethereumkit.core.storage

import io.horizontalsystems.ethereumkit.core.IEip20Storage
import io.horizontalsystems.ethereumkit.models.Eip20Event

class Eip20Storage(database: Eip20Database) : IEip20Storage {
    private val erc20EventDao = database.eip20EventDao()

    override fun getLastEvent(): Eip20Event? =
        erc20EventDao.getLastEip20Event()

    override fun save(events: List<Eip20Event>) {
        erc20EventDao.insertEip20Events(events)
    }

    override fun getEvents(): List<Eip20Event> =
        erc20EventDao.getEip20Events()

    override fun getEventsByHashes(hashes: List<ByteArray>): List<Eip20Event> =
        erc20EventDao.getEip20EventsByHashes(hashes)

}
