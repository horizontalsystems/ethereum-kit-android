package io.horizontalsystems.erc20kit.core

import io.reactivex.Single
import org.junit.Assert.*
import org.junit.Test

class DataProviderTest {

    @Test
    fun zipTest() {

        val values = listOf("1_Value", "2_Value", "3_Value", "4_Value", "5_Value", "6_Value")

        getSingleZip(values).subscribe(
                { value ->
                    assertEquals(value.get("1_Value"), "1_Value_Return")
                    assertEquals(value.get("2_Value"), "2_Value_Return")
                    assertEquals(value.size, values.size)
                },
                { error ->
                    println("Error:$error")
                })

        println("End")
    }

    private fun getSingle(value: String): Single<String> {
        println("GetSingle-$value")
        return Single.just(value + "_Return")
    }

    private fun getSingleZip(values: List<String>): Single<Map<String, String>> {

        val singles = values.map { hash ->
            getSingle(hash).flatMap { txStatus ->
                Single.just(Pair(hash, txStatus))
            }
        }

        return Single.zip(singles) { singleResults ->
            singleResults.map { it as? Pair<String, String> }
        }.flatMap { list ->
            val map = mutableMapOf<String, String>()
            list.forEach {
                if (it != null) {
                    map[it.first] = it.second
                }
            }
            Single.just(map)
        }
    }
}