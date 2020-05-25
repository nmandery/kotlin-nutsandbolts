package com.github.nmandery.nutsandbolts

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.ref.WeakReference
import java.time.LocalDateTime
import java.time.temporal.TemporalAmount
import kotlin.reflect.KProperty

interface ExpiringLazy {
    fun clearIfBeyond(now: LocalDateTime = LocalDateTime.now()): Boolean
}

internal object ExpringLazyRegistry {
    private var expiringLazyList: MutableList<WeakReference<ExpiringLazy>> = mutableListOf()
    private val mutex = Mutex()
    internal var cleanupTask: Deferred<Unit>? = null

    var clearIntervalMillis: Long = 30 * 1000

    suspend fun register(el: ExpiringLazy) {
        mutex.withLock {
            expiringLazyList.add(WeakReference(el))
            if (cleanupTask == null || cleanupTask!!.isCompleted || cleanupTask!!.isCancelled) {
                cleanupTask = GlobalScope.async {
                    gcCleanup()
                }
            }
        }
    }

    fun registerBlocking(el: ExpiringLazy) {
        runBlocking {
            register(el)
        }
    }

    private suspend fun gcCleanup() {
        while (this.expiringLazyList.isNotEmpty()) {
            delay(clearIntervalMillis)
            clearExpired()
        }
    }

    suspend fun clearExpired() {
        mutex.withLock {
            val now = LocalDateTime.now()
            expiringLazyList.retainAll { weakReference ->
                val el = weakReference.get()
                if (el != null) {
                    el.clearIfBeyond(now)
                    true
                } else {
                    false
                }
            }
        }
    }
}

internal object UNINITIALIZED_VALUE

/**
 * the interval between runs of the clearing coroutine.
 */
fun setExpiringLazyClearInterval(milliseconds: Long) {
    ExpringLazyRegistry.clearIntervalMillis = milliseconds
}

/**
 * delegate for a lazy value which expires after a given amount of time. Accessing after that, will
 * result in fn getting called again to refresh the value.
 *
 * `clearWhenExpired` defines how expired values are handled:
 * false:  Expired values will still be kept in memory and will not be garbagecollected before all references to the
 *         delegate are gone.
 * true:   A background coroutine is launched which removes the reference to the expired object to allow the garbagecollector
 *         to re-use the memory.
 *
 * Modelled after the implementation of kotlins `lazy`
 */
fun <T> expiringLazy(validFor: TemporalAmount, clearWhenExpired: Boolean = false, initialize: () -> T) =
    ExpiringLazyImpl(validFor, clearWhenExpired, initialize)

/**
 * a lazy value which expires after a given amount of time. Accessing after that, will
 * result in fn getting called again to refresh the value.
 *
 * `clearWhenExpired` defines how expired values are handled:
 * false:  Expired values will still be kept in memory and will not be garbagecollected before all references to the
 *         delegate are gone.
 * true:   A background coroutine is launched which removes the reference to the expired object to allow the garbagecollector
 *         to re-use the memory.
 *
 * Modelled after the implementation of kotlins `lazy`
 */
class ExpiringLazyImpl<out T>(
    private val validFor: TemporalAmount,
    clearWhenExpired: Boolean,
    private val initialize: () -> T
) : ExpiringLazy {
    @Volatile
    private var _value: Any? = UNINITIALIZED_VALUE
    private var _expiresAfter = LocalDateTime.now() + validFor

    init {
        if (clearWhenExpired) {
            ExpringLazyRegistry.registerBlocking(this)
        }
    }

    val value: T
        get() {
            val v = _value
            return if (v !== UNINITIALIZED_VALUE && LocalDateTime.now() <= _expiresAfter) {
                @Suppress("UNCHECKED_CAST")
                v as T
            } else {
                synchronized(this) {
                    // check once again in case multiple callers where waiting for this synchronized block
                    val v2 = _value
                    val now = LocalDateTime.now()
                    if (v2 !== UNINITIALIZED_VALUE && now <= _expiresAfter) {
                        @Suppress("UNCHECKED_CAST")
                        return v2 as T
                    }

                    val newValue = initialize()
                    _value = newValue
                    _expiresAfter = now + validFor
                    newValue
                }
            }
        }

    /**
     * makes this object a delegate
     */
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    override fun clearIfBeyond(now: LocalDateTime): Boolean {
        return synchronized(this) {
            if (_value !== UNINITIALIZED_VALUE && now > _expiresAfter) {
                _value = UNINITIALIZED_VALUE // remove the reference to the value object
                true
            } else {
                false
            }
        }
    }
}

