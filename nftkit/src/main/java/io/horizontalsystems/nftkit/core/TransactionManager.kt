package io.horizontalsystems.nftkit.core

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.nftkit.contracts.Eip1155SafeTransferFromMethod
import io.horizontalsystems.nftkit.contracts.Eip721SafeTransferFromMethod
import java.math.BigInteger

class TransactionManager(ethereumKit: EthereumKit) {
    private val address: Address = ethereumKit.receiveAddress

    fun transferEip721TransactionData(contractAddress: Address, to: Address, tokenId: BigInteger) =
        TransactionData(
            to = contractAddress,
            value = BigInteger.ZERO,
            input = Eip721SafeTransferFromMethod(address, to, tokenId, byteArrayOf()).encodedABI()
        )

    fun transferEip1155TransactionData(contractAddress: Address, to: Address, tokenId: BigInteger, value: BigInteger) =
        TransactionData(
            to = contractAddress,
            value = BigInteger.ZERO,
            input = Eip1155SafeTransferFromMethod(address, to, tokenId, value, byteArrayOf()).encodedABI()
        )
}