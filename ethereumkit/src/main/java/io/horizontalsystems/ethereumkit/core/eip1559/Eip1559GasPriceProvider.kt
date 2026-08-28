package io.horizontalsystems.ethereumkit.core.eip1559

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class Eip1559GasPriceProvider(
        private val evmKit: EthereumKit
) {
    fun feeHistoryFlow(
            blocksCount: Long,
            rewardPercentile: List<Int>,
            defaultBlockParameter: DefaultBlockParameter = DefaultBlockParameter.Latest
    ): Flow<FeeHistory> {
        return evmKit.lastBlockHeightFlow
                .map {
                    feeHistory(blocksCount, defaultBlockParameter, rewardPercentile)
                }
    }

    suspend fun feeHistory(blocksCount: Long, defaultBlockParameter: DefaultBlockParameter, rewardPercentile: List<Int>): FeeHistory {
        val feeHistoryRequest = FeeHistoryJsonRpc(
                blocksCount,
                defaultBlockParameter,
                rewardPercentile
        )
        return evmKit.rpc(feeHistoryRequest)
    }
}
