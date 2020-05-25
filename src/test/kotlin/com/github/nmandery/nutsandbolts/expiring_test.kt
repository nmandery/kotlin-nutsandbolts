package com.github.nmandery.nutsandbolts

import io.kotlintest.matchers.string.shouldStartWith
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.delay
import java.lang.ref.WeakReference
import java.time.Duration
import kotlin.random.Random


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

    "expired value gets cleared" {
        // stop the coroutine in the case there is one running with another interval
        ExpringLazyRegistry.cleanupTask?.cancel()

        setExpiringLazyClearInterval(1500)
        var ref = WeakReference("nothing")
        val expv by expiringLazy(Duration.ofSeconds(1), clearWhenExpired = true) {
            // create a random value which is not statically allocated by adding something dynamic
            val value = "something ${Random.nextInt()}"
            ref = WeakReference(value)
            value
        }
        expv.shouldStartWith("something")
        ref.get().shouldStartWith("something")
        delay(5000)
        System.gc() // clears objects referenced only by weak references
        ref.get().shouldBe(null)
    }
})
