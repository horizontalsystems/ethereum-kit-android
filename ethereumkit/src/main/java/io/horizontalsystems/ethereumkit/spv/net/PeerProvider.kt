package io.horizontalsystems.ethereumkit.spv.net

import io.horizontalsystems.ethereumkit.core.ISpvStorage
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.network.INetwork
import io.horizontalsystems.ethereumkit.crypto.ECKey
import io.horizontalsystems.ethereumkit.spv.net.les.LESPeer

class PeerProvider(val connectionKey: ECKey,
                   val storage: ISpvStorage,
                   val network: INetwork) {

    fun getPeer(): LESPeer {

        /*val node = Node(id = "e679038c2e4f9f764acd788c3935cf526f7f630b55254a122452e63e2cfae3066ca6b6c44082c2dfbe9ddffc9df80546d40ef38a0e3dfc9c8720c732446ca8f3".hexStringToByteArray(),
                host = "192.168.4.39",
                port = 30303,
                discoveryPort = 30301)*/

        /* val node = Node(id = "053d2f57829e5785d10697fa6c5333e4d98cc564dbadd87805fd4fedeb09cbcb642306e3a73bd4191b27f821fb442fcf964317d6a520b29651e7dd09d1beb0ec".hexStringToByteArray(),
                 host = "79.98.29.154",
                 port = 30303,
                 discoveryPort = 30301)*/

/*
        val node = Node(id = "f9a9a1b2f68dc119b0f44ba579cbc40da1f817ddbdb1045a57fa8159c51eb0f826786ce9e8b327d04c9ad075f2c52da90e9f84ee4dde3a2a911bb1270ef23f6d".hexStringToByteArray(),
                host = "eth-testnet.horizontalsystems.xyz",
                port = 20303,
                discoveryPort = 30301)
*/

        val node = Node(id = "9cfc66931bd30d316b57c4e761a58110d882fc0a6387e26897499be4263cac7cbdb1a8ba43088b8b279ffa84db6c331e7968875191baeecf9d87c1221feec1eb".hexStringToByteArray(),
                host = "212.112.123.197",
                port = 30303,
                discoveryPort = 30301)

        return LESPeer.getInstance(connectionKey, node)
    }

}
