package io.horizontalsystems.ethereumkit.spv.net.devp2p

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class CapabilityHelperTest : Spek({

    val les1 = Capability("les", 1)
    val les2 = Capability("les", 2)
    val eth1 = Capability("eth", 1)
    val pip1 = Capability("pip", 1)

    val helper = CapabilityHelper()

    describe("#sharedCapabilities") {
        it("returns empty when there are no intersections by name and version") {
            val result = helper.sharedCapabilities(listOf(les1), listOf(les2, eth1))

            assertTrue(result.isEmpty())
        }

        it("returns intersections by name and version") {
            val result = helper.sharedCapabilities(listOf(les1, eth1), listOf(eth1))
            assertEquals(listOf(eth1), result)
        }

        it("returns sorted intersections by name") {
            val result = helper.sharedCapabilities(listOf(les1, eth1), listOf(pip1, les1, eth1))
            assertEquals(listOf(eth1, les1), result)
        }

        it("returns only latest version intersection if there are several capabilities for same name") {
            val result = helper.sharedCapabilities(listOf(les1, les2, eth1), listOf(pip1, les1, les2, eth1))
            assertEquals(listOf(eth1, les2), result)
        }
    }
})
