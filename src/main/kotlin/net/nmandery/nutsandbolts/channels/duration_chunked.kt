package net.nmandery.nutsandbolts.channels

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

@ExperimentalCoroutinesApi
class DurationChunked<T> constructor(
    inChannel: Channel<T>,
    durationMillis: Long,
    val scope: CoroutineScope = GlobalScope,
    private val channel: Channel<List<T>> = Channel()
) : Channel<List<T>> by channel {

    init {
        scope.launch {
            while (!isClosedForSend && !inChannel.isClosedForReceive) {
                val currentChunk = mutableListOf<T>()
                val startChunkAt = System.currentTimeMillis()
                while (startChunkAt + durationMillis > System.currentTimeMillis()) {
                    val value = inChannel.poll()
                    if (value != null) {
                        currentChunk.add(value)
                    } else {
                        delay((durationMillis / 100).coerceAtLeast(20))
                    }
                }
                if (!isClosedForSend && currentChunk.isNotEmpty()) {
                    send(currentChunk)
                }
            }
            channel.close()
        }
    }

    override fun close(cause: Throwable?): Boolean {
        return channel.close(cause)
    }
}

/**
 * chunk the values of a channel by the given time interval in milliseconds
 */
@ExperimentalCoroutinesApi
fun <T> Channel<T>.durationChunked(durationMillis: Long, scope: CoroutineScope = GlobalScope) =
    DurationChunked(this, durationMillis, scope=scope)
