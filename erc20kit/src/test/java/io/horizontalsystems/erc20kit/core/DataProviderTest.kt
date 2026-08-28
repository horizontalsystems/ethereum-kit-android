package io.horizontalsystems.erc20kit.core

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DataProviderTest {

    @Test
    fun zipTest() = runTest {

        val values = listOf("1_Value", "2_Value", "3_Value", "4_Value", "5_Value", "6_Value")

        val value = getZip(values)

        assertEquals(value.get("1_Value"), "1_Value_Return")
        assertEquals(value.get("2_Value"), "2_Value_Return")
        assertEquals(value.size, values.size)

        println("End")
    }

    private suspend fun get(value: String): String {
        println("Get-$value")
        return value + "_Return"
    }

    private suspend fun getZip(values: List<String>): Map<String, String> = coroutineScope {
        val results = values.map { hash ->
            async { Pair(hash, get(hash)) }
        }.awaitAll()

        val map = mutableMapOf<String, String>()
        results.forEach {
            map[it.first] = it.second
        }
        map
    }
}
