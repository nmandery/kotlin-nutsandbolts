package com.github.nmandery.nutsandbolts.coroutines.channels

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun <E> Channel<E>.processN(nConcurrentWorkers: Int, f: suspend (Int, E) -> Unit) {
    val chan = this
    coroutineScope {
        chan.process((0..nConcurrentWorkers).toList(), f)
    }
}

suspend fun <E, D> Channel<E>.process(workerData: Collection<D>, f: suspend (D, E) -> Unit) {
    val chan = this
    val workerChannel = Channel<D>(workerData.size)
    workerData.forEach { workerChannel.send(it) }
    coroutineScope {
        for (v in chan) {
            launch {
                val ld = workerChannel.receive() // block until a worker is available
                try {
                    f(ld, v)
                } finally {
                    workerChannel.send(ld)
                }
            }
        }
    }
}

/**
 * apply f to all values of the channel. All non-null-values produced by f will be send
 * using outChannel
 *
 * outChannel will be closed when all elements of inChannel have been processed.
 */
suspend fun <E, O> Channel<E>.processAndSendN(
    nConcurrentWorkers: Int,
    outChannel: Channel<O>,
    f: suspend (Int, E) -> O?
) {
    val chan = this
    coroutineScope {
        chan.processAndSend((0..nConcurrentWorkers).toList(), outChannel, f)
    }
}

suspend fun <E, O, D> Channel<E>.processAndSend(
    workerData: Collection<D>,
    outChannel: Channel<O>,
    f: suspend (D, E) -> O?
) {
    val chan = this
    val workerChannel = Channel<D>(workerData.size)
    workerData.forEach { workerChannel.send(it) }
    coroutineScope {
        for (v in chan) {
            launch {
                val worker = workerChannel.receive()
                try {
                    val out = f(worker, v)
                    if (out != null) {
                        outChannel.send(out)
                    }
                } finally {
                    workerChannel.send(worker)
                }
            }
        }
    }
    outChannel.close()
}
