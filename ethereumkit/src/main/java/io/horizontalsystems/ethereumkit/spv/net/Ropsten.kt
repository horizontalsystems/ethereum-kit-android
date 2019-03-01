package io.horizontalsystems.ethereumkit.spv.net

import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import java.math.BigInteger

class Ropsten : INetwork {

    override val id: Int = 3

    override val genesisBlockHash: ByteArray =
            "41941023680923e0fe4d74a34bdac8141f2540e3ae90623718e47d66d1ca4a2d".hexStringToByteArray()

    override val checkpointBlock =
            BlockHeader(hashHex = "8e979e196f08a06ecd3e7bbbf83b387a5e429b43a6694df7b01b9402a272eec6".hexStringToByteArray(),
                    totalDifficulty = "18284610994619994".hexStringToByteArray(),
                    parentHash = "91690d0990e80aa73341926434746bb532194204d81b0736cc5f147a60c0824f".hexStringToByteArray(),
                    unclesHash = "1dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347".hexStringToByteArray(),
                    coinbase = "b17fc44dd79d21cd7f4d8c9686c98ae9039b3909".hexStringToByteArray(),
                    stateRoot = "a44e837fc749e7fb4dea349c37f6c4d0c2306fe7452d3865e4e75179bdf54e8c".hexStringToByteArray(),
                    transactionsRoot = "b3a7a2911892b1e26a8beadf9931861a66227698e352a976af8a00948bc9d547".hexStringToByteArray(),
                    receiptsRoot = "1b6d1dcc3549c4dbc1c211b17d075457a9930cd9effd82e68a21742350590782".hexStringToByteArray(),
                    logsBloom = "00000000000042000000000000000000000030000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000008000000200000000008000000000000000040000000000000000000000000000000000000000000000000000000000010000000000000000010000000000000000000000000000000100000000000000001000000010000000000000000080000000000000000000000000004000010000000000200000000000000000000000002000000000100000000000010000000000000000000000000002000000000000000000000000000000000000000004000000000000100000004000000".hexStringToByteArray(),
                    difficulty = "21F51199".hexStringToByteArray(),
                    height = BigInteger("5049204"),
                    gasLimit = "7A121D".hexStringToByteArray(),
                    gasUsed = 1098844,
                    timestamp = 1550569079,
                    extraData = "de830203018f5061726974792d457468657265756d86312e33312e31826c69".hexStringToByteArray(),
                    mixHash = "d95fe97e78ae762fbccf683c433fcfa72b137757e602dffd5e8e26b3ba3a02f8".hexStringToByteArray(),
                    nonce = "2bb183a1640b7c81".hexStringToByteArray())
}