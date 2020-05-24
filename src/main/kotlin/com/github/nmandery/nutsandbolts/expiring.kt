package com.github.nmandery.nutsandbolts

import java.time.LocalDateTime
import java.time.temporal.TemporalAmount
import kotlin.reflect.KProperty


internal object UNINITIALIZED_VALUE

/**
 * delegate for a lazy value which expires after a given amount of time. Accessing after that, will
 * result in fn getting called again to refresh the value
 *
 * Modelled after the implementation of kotlins `lazy`
 */
fun <T> expiringLazy(validFor: TemporalAmount, initialize: () -> T) = ExpiringLazyImpl(validFor, initialize)

/**
 * a lazy value which expires after a given amount of time. Accessing after that, will
 * result in fn getting called again to refresh the value
 *
 * Modelled after the implementation of kotlins `lazy`
 */
class ExpiringLazyImpl<out T>(private val validFor: TemporalAmount, private val initialize: () -> T) {
    @Volatile
    private var _value: Any? = UNINITIALIZED_VALUE
    private var _expiresAfter = LocalDateTime.now() + validFor

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
}

