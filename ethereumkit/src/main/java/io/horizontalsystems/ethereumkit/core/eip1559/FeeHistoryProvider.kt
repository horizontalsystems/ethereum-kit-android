package io.horizontalsystems.ethereumkit.core.eip1559

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.reactivex.Flowable

class FeeHistoryProvider(
        private val evmKit: EthereumKit
) {
    fun feeHistory(
            blocksCount: Long,
            rewardPercentile: List<Int>,
            defaultBlockParameter: DefaultBlockParameter = DefaultBlockParameter.Latest
    ): Flowable<FeeHistory> {
        return evmKit.lastBlockHeightFlowable
                .flatMapSingle {
                    val feeHistoryRequest = FeeHistoryJsonRpc(
                            blocksCount,
                            defaultBlockParameter,
                            rewardPercentile
                    )
                    evmKit.rpcSingle(feeHistoryRequest)
                }
    }
}
