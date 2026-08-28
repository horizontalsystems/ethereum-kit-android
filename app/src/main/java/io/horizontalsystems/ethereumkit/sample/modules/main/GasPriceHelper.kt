package io.horizontalsystems.ethereumkit.sample.modules.main

import io.horizontalsystems.ethereumkit.core.eip1559.Eip1559GasPriceProvider
import io.horizontalsystems.ethereumkit.core.eip1559.FeeHistory
import io.horizontalsystems.ethereumkit.models.GasPrice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

class GasPriceHelper(private val eip1559GasPriceProvider: Eip1559GasPriceProvider) {

    fun gasPriceFlow(): Flow<GasPrice.Eip1559> {
        return eip1559GasPriceProvider.feeHistoryFlow(4, rewardPercentile = listOf(50))
            .mapNotNull { map(it) }
    }

    private fun map(feeHistory: FeeHistory): GasPrice.Eip1559? {
        var recommendedBaseFee: Long? = null
        var recommendedPriorityFee: Long? = null

        feeHistory.baseFeePerGas.lastOrNull()?.let { currentBaseFee ->
            recommendedBaseFee = currentBaseFee
        }

        var priorityFeeSum: Long = 0
        var priorityFeesCount = 0
        feeHistory.reward.forEach { priorityFeeArray ->
            priorityFeeArray.firstOrNull()?.let { priorityFee ->
                priorityFeeSum += priorityFee
                priorityFeesCount += 1
            }
        }

        if (priorityFeesCount > 0) {
            recommendedPriorityFee = priorityFeeSum / priorityFeesCount
        }

        return recommendedBaseFee?.let { baseFee ->
            recommendedPriorityFee?.let { tip ->

                GasPrice.Eip1559(baseFee + tip, tip)
            }

        }
    }


}
