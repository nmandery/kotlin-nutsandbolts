package net.nmandery.nutsandbolts.filesystem


import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchService


data class WatchEvent<T>(
    val path: Path,
    val kind: Kind,
    val attachedData: T?
) {

    enum class Kind(val value: String) {
        Created("created"),
        Modified("modified"),
        Deleted("deleted"),
        Initialized("initialized")

    }
}

/**
 * kotlin wrapper around javas WatchService
 * */
@ExperimentalCoroutinesApi
fun <T> Path.asWatchChannel(
    eventKinds: Collection<WatchEvent.Kind>,
    attachedData: T?,
    scope: CoroutineScope = GlobalScope,
    pollIntervalMillis: Long = 100,
) = WatchChannel(this, eventKinds, scope = scope, attachedData = attachedData, pollIntervalMillis = pollIntervalMillis)

@ExperimentalCoroutinesApi
class WatchChannel<T>(
    val watchPath: Path,
    val eventKinds: Collection<WatchEvent.Kind>,
    val attachedData: T?,
    val scope: CoroutineScope = GlobalScope,
    val pollIntervalMillis: Long = 100,
    private val channel: Channel<WatchEvent<T>> = Channel()
) : Channel<WatchEvent<T>> by channel {

    private val watchService: WatchService = FileSystems.getDefault().newWatchService()
    private val watchKey = watchPath.register(watchService, eventKinds.mapNotNull {
        when (it) {
            WatchEvent.Kind.Created -> ENTRY_CREATE
            WatchEvent.Kind.Modified -> ENTRY_MODIFY
            WatchEvent.Kind.Deleted -> ENTRY_DELETE
            else -> null
        }
    }
        .distinct()
        .toTypedArray()
    )

    init {
        scope.launch(Dispatchers.IO) {
            channel.send(
                WatchEvent(
                    watchPath,
                    WatchEvent.Kind.Initialized,
                    attachedData
                )
            )

            while (!isClosedForSend) {
                val key = watchService.take()
                if (key != null) {
                    val keyWatchPath = key.watchable() as Path
                    key.pollEvents().forEach { evt ->
                        val evtKind = evt.kind()
                        if (evtKind == OVERFLOW) {
                            return@forEach
                        }
                        val eventPath = keyWatchPath.resolve(evt.context() as Path)

                        val watchEventKind = when (evtKind) {
                            ENTRY_MODIFY -> WatchEvent.Kind.Modified
                            ENTRY_CREATE -> WatchEvent.Kind.Created
                            ENTRY_DELETE -> WatchEvent.Kind.Deleted
                            else -> null
                        }
                        if (watchEventKind != null) {
                            channel.send(
                                WatchEvent(
                                    eventPath, watchEventKind, attachedData
                                )
                            )
                        }
                    }
                    if (!key.reset()) {
                        key.cancel()
                        close()
                    }
                }

                delay(pollIntervalMillis)
            }
        }
    }

    override fun close(cause: Throwable?): Boolean {
        watchKey.cancel()
        return channel.close(cause)
    }
}