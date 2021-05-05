package io.horizontalsystems.ethereumkit.network

import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import java.math.BigInteger

class EthRopsten : INetwork {

    override val id: Int = 3

    override val genesisBlockHash: ByteArray =
            "41941023680923e0fe4d74a34bdac8141f2540e3ae90623718e47d66d1ca4a2d".hexStringToByteArray()

    override val checkpointBlock =
            BlockHeader(hashHex = "bce9c39107fd4b58a31ab28975e4b5689a8a5e41d06b2736795cd6f643ac2d73".hexStringToByteArray(),
                    totalDifficulty = BigInteger("18601822522462629"),
                    parentHash = "c272ad832f06865beabc3dd4c83699e039740fba7239a0aa2c20c5ae434bec54".hexStringToByteArray(),
                    unclesHash = "1dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347".hexStringToByteArray(),
                    coinbase = "cd626bc764e1d553e0d75a42f5c4156b91a63f23".hexStringToByteArray(),
                    stateRoot = "4f3513d052a8a85a7015359f17e9e61c4fc8169443017268500e09df991c0c55".hexStringToByteArray(),
                    transactionsRoot = "b82f3e93274ff1f68d10c7f6acef7d239794ae603e1a17f880793ed100f5be95".hexStringToByteArray(),
                    receiptsRoot = "6b1dfc93f7aa063b150605b05f6976c0d23159f7a27c708d9b7e9a8e19469199".hexStringToByteArray(),
                    logsBloom = "00000001000000000001000004000000002040000080000008000004000000000000000000000000000000000000000010044000002000000002000000000000010000000028000000008008800020000020000000808000800001080000000001000000020000000004000000000801000004000002400404000010000000000000010000000400000040000000000100000020000000000000000020020000000080200000000000002000000000000000000000000800000000000000000200000042000000000020000080000000008070810000040000000000000020080000000008000000000010200000020004000040000000000000000004000000".hexStringToByteArray(),
                    difficulty = ("ccc2825").hexStringToByteArray(),
                    height = 5227842,
                    gasLimit = "7a1200".hexStringToByteArray(),
                    gasUsed = 0x3161d0,
                    timestamp = 0x5c8f815b,
                    extraData = "d883010817846765746888676f312e31302e34856c696e7578".hexStringToByteArray(),
                    mixHash = ("cae580f260efcee4ed8789380528e9770b5c260d289321ff8c6352eb031d830").hexStringToByteArray(),
                    nonce = "004d35d804eac7d8".hexStringToByteArray())

    override val blockTime: Long = 10

}
