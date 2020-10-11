package net.nmandery.nutsandbolts.channels

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class DurationChunkedTest : StringSpec({
    "duration chunked" {
        val incomming = Channel<Int>()
        val resultsDef = async { incomming.durationChunked(750, scope = this).toList() }
        launch {
            (0..7).forEach {
                incomming.send(it)
                delay(500)
            }
            incomming.close()
        }
        resultsDef.await().shouldBe(
            listOf(
                listOf(0, 1),
                listOf(2),
                listOf(3, 4),
                listOf(5),
                listOf(6, 7)
            )
        )
    }
})