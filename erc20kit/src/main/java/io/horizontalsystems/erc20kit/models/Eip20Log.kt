package io.horizontalsystems.erc20kit.models

import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

sealed class Eip20Log {
    class Transfer(val from: Address, val to: Address, val value: BigInteger) : Eip20Log()
    class Approve(val owner: Address, val spender: Address, val value: BigInteger) : Eip20Log()
}
