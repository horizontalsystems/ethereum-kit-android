package io.horizontalsystems.ethereumkit.spv.net.devp2p

class CapabilityHelper {
    fun sharedCapabilities(myCapabilities: List<Capability>, nodeCapabilities: List<Capability>): List<Capability> {
        val sharedCapabilities = mutableListOf<Capability>()

        myCapabilities.forEach { myCapability ->
            if (nodeCapabilities.contains(myCapability))
                sharedCapabilities.add(myCapability)
        }

        return sharedCapabilities.sorted()
    }
}
