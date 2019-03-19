package io.horizontalsystems.ethereumkit.spv.net.devp2p

class CapabilityHelper {
    fun sharedCapabilities(myCapabilities: List<Capability>, nodeCapabilities: List<Capability>): List<Capability> {
        val sharedCapabilities = mutableListOf<Capability>()

        myCapabilities.forEach { myCapability ->
            if (nodeCapabilities.contains(myCapability)) {
                val indexOfOlderVersion = sharedCapabilities.indexOfFirst { it.name == myCapability.name && it.version <= myCapability.version }
                if (indexOfOlderVersion != -1) {
                    sharedCapabilities[indexOfOlderVersion] = myCapability
                } else {
                    sharedCapabilities.add(myCapability)
                }
            }
        }

        return sharedCapabilities.sorted()
    }
}
