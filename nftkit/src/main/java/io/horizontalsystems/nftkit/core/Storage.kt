package io.horizontalsystems.nftkit.core

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.nftkit.core.db.NftKitDatabase
import io.horizontalsystems.nftkit.models.*
import java.math.BigInteger

class Storage(
    database: NftKitDatabase
) {
    private val eip721EventDao = database.eip721EventDao()
    private val eip1155EventDao = database.eip1155EventDao()
    private val balanceDao = database.nftBalanceDao()

    fun nftBalances(type: NftType): List<NftBalance> =
        balanceDao.nftBalances(type)

    fun existingNftBalances(): List<NftBalance> =
        balanceDao.existingNftBalances()

    fun nonSyncedNftBalances(): List<NftBalance> =
        balanceDao.nonSyncedNftBalances()

    fun existingNftBalance(contractAddress: Address, tokenId: BigInteger): NftBalance? =
        balanceDao.existingNftBalance(contractAddress, tokenId)

    fun setNotSynced(nfts: List<Nft>) =
        nfts.forEach { balanceDao.setNotSynced(it.contractAddress, it.tokenId) }

    fun setSynced(nft: Nft, balance: Int) =
        balanceDao.setSynced(nft.contractAddress, nft.tokenId, balance)

    fun saveNftBalances(balances: List<NftBalance>) =
        balanceDao.insertAll(balances.map { NftBalanceRecord(it) })

    fun lastEip721Event(): Eip721Event? =
        eip721EventDao.lastEvent()

    fun eip721Events(): List<Eip721Event> =
        eip721EventDao.events()

    fun eip721Events(hashes: List<ByteArray>) =
        eip721EventDao.eventsByHash(hashes)

    fun saveEip721Events(events: List<Eip721Event>) =
        eip721EventDao.insertAll(events)

    fun lastEip1155Event(): Eip1155Event? =
        eip1155EventDao.lastEvent()

    fun eip1155Events(): List<Eip1155Event> =
        eip1155EventDao.events()

    fun eip1155Events(hashes: List<ByteArray>): List<Eip1155Event> =
        eip1155EventDao.eventsByHash(hashes)

    fun saveEip1155Events(events: List<Eip1155Event>) =
        eip1155EventDao.insertAll(events)
}