package io.horizontalsystems.nftkit.core

import io.horizontalsystems.ethereumkit.contracts.ContractEventInstance
import io.horizontalsystems.ethereumkit.core.IEventDecorator
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.ethereumkit.models.TransactionLog
import io.horizontalsystems.nftkit.events.Eip1155TransferEventInstance
import io.horizontalsystems.nftkit.models.TokenInfo

class Eip1155EventDecorator(
    private val userAddress: Address,
    private val storage: Storage
) : IEventDecorator {

    override fun contractEventInstancesMap(transactions: List<Transaction>): Map<String, List<ContractEventInstance>> {
        val events = if (transactions.size > 100) {
            storage.eip1155Events()
        } else {
            storage.eip1155Events(transactions.map(Transaction::hash))
        }

        val map = mutableMapOf<String, List<ContractEventInstance>>()

        for (event in events) {
            val tokenInfo = when {
                event.tokenName.isEmpty() && event.tokenSymbol.isEmpty() -> {
                    null
                }
                else -> TokenInfo(
                    event.tokenName,
                    event.tokenSymbol,
                    1
                )
            }
            val eventInstance = Eip1155TransferEventInstance(
                event.contractAddress,
                event.from,
                event.to,
                event.tokenId,
                event.tokenValue.toBigInteger(),
                tokenInfo
            )

            map[event.hashString] = (map[event.hashString] ?: listOf()) + listOf(eventInstance)
        }

        return map
    }

    override fun contractEventInstances(logs: List<TransactionLog>): List<ContractEventInstance> {
        return logs.mapNotNull { log ->
            val eventInstance = log.getEip1155EventInstance() ?: return@mapNotNull null

            when (eventInstance) {
                is Eip1155TransferEventInstance -> {
                    if (eventInstance.from == userAddress || eventInstance.to == userAddress) {
                        return@mapNotNull eventInstance
                    }
                }
            }

            return@mapNotNull null
        }

    }
}