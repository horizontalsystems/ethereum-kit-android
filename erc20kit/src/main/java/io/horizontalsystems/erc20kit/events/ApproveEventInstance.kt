package io.horizontalsystems.erc20kit.events

import io.horizontalsystems.ethereumkit.contracts.ContractEventInstance
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class ApproveEventInstance(
    contractAddress: Address,
    val owner: Address,
    val spender: Address,
    val value: BigInteger
) : ContractEventInstance(contractAddress)
