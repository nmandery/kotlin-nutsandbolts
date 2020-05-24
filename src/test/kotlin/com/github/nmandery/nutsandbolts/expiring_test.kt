package com.github.nmandery.nutsandbolts

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.delay
import java.time.Duration


class ExpiringTest : StringSpec({

    "test expiring" {
        var fetchCalled = 0
        val expv by expiringLazy(Duration.ofSeconds(1)) {
            fetchCalled += 1
            fetchCalled
        }
        fetchCalled.shouldBe(0)
        expv.shouldBe(1)
        fetchCalled.shouldBe(1)
        expv.shouldBe(1)
        fetchCalled.shouldBe(1)

        delay(1500)
        expv.shouldBe(2)
        fetchCalled.shouldBe(2)
    }

    "test expiring mutliple threads" {
        var fetchCalled = 0
        val expv by expiringLazy(Duration.ofSeconds(5)) {
            fetchCalled += 1
            fetchCalled
        }

        fetchCalled.shouldBe(0)
        repeat(5) {
            Thread {
                Thread.sleep(100)
                expv.shouldBe(1)
            }
        }
        expv.shouldBe(1)
    }
})
