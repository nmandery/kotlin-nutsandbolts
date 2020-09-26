package net.nmandery.nutsandbolts.coroutines.channels

import io.kotlintest.matchers.numerics.shouldBeGreaterThanOrEqual
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ProcessTest : StringSpec({

    "test processAndSendN" {
        val results = mutableListOf<Int>()
        runBlocking {
            val cin = Channel<Int>()
            val cout = Channel<Int>()

            launch {
                cin.processAndSendN(3, cout) { workerNum, value ->
                    (value * 10) + workerNum + 1
                }
            }
            launch {
                for (value in cout) {
                    results.add(value)
                }
            }
            (1..10).forEach { cin.send(it) }
            cin.close() // close the input to make the whole chain shutdown
        }
        results.size.shouldBe(10)
        results.forEach {
            it.shouldBeGreaterThanOrEqual(10)
        }
    }
})
