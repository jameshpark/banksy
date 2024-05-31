package org.jameshpark.banksy.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import java.time.Duration
import java.util.Properties
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger { }

/**
 * Launches the application by executing the provided [block] in a suspended manner.
 * This method sets up the application coroutine and manages its lifecycle.
 *
 * @param block The suspendable function that defines the behavior of the application.
 *              It is executed within the context of the [ApplicationScope].
 */
fun launchApp(block: suspend ApplicationScope.() -> Unit) = try {
    runBlocking {
        val applicationJob = launch(Dispatchers.Default + CoroutineName("application-coroutine")) {
            logger.info { "Application started." }
            try {
                applicationScope {
                    block()
                }
            } finally {
                logger.info { "Application finished." }
            }
        }

        Runtime.getRuntime().addShutdownHook(
            Thread(
                {
                    if (applicationJob.isActive) {
                        logger.info { "Received termination signal. Shutting down the application." }
                        try {
                            runBlocking {
                                withTimeout(Duration.ofSeconds(10).toMillis()) {
                                    applicationJob.cancelAndJoin()
                                }
                            }
                        } catch (_: CancellationException) {
                            // Application coroutine shutdown was interrupted/timed out.
                            // Let the JVM shutdown and take the underlying threads with it.
                        } catch (t: Throwable) {
                            logger.error(t) { "Error shutting down the application. Terminating the app." }
                        }
                    }
                },
                "shutdown"
            )
        )
    }
} catch (_: CancellationException) {
    // This is not an exception we want to float up to the caller's context.
    // Swallow this and just let the application coroutine scope cancel as normal.
}

/**
 * Launches the application by executing the provided [block] in a suspended manner.
 * This method sets up the application coroutine and manages its lifecycle.
 *
 * @param block The suspendable function that defines the behavior of the application.
 *              It is executed within the context of the [ApplicationScope].
 */
suspend fun applicationScope(block: suspend ApplicationScope.() -> Unit) {
    val registry = ThunkRegistry()
    try {
        coroutineScope {
            ApplicationScope(registry, loadProperties(), this).block()
        }
    } finally {
        registry.cleanup()
    }
}

/**
 * ApplicationScope class represents the scope in which the application runs.
 * It extends CoroutineScope, which allows it to launch and manage coroutines.
 *
 * @property registry The ThunkRegistry instance used to register resources.
 * @property properties The Properties instance containing application-specific properties.
 * @param scope The CoroutineScope instance that will be delegated to by this ApplicationScope.
 */
class ApplicationScope(
    private val registry: ThunkRegistry,
    val properties: Properties,
    scope: CoroutineScope
) : CoroutineScope by scope {
    fun <T> T.register(): T {
        registry.register(this)
        return this
    }
}

/**
 * ThunkRegistry is a class that manages the registration and cleanup of resources.
 * It provides a way to register resources and perform cleanup in a suspended manner.
 */
class ThunkRegistry {
    private val registry = ConcurrentLinkedDeque<Thunk>()
    private val isRunning = AtomicBoolean(true)

    fun <T> register(resource: T) {
        check(isRunning.get()) { "Application is not running" }
        when (resource) {
            is AutoCloseable -> registry.push(AutoCloseableThunk(resource))
        }
    }

    suspend fun cleanup() {
        if (isRunning.compareAndSet(true, false)) {
            withContext(NonCancellable) {
                while (!registry.isEmpty()) {
                    registry.pop().also {
                        try {
                            it.cleanup()
                        } catch (t: Throwable) {
                            logger.error(t) { "Error while cleaning up resource." }
                        }
                    }
                }
            }
        }
    }
}

interface Thunk {
    suspend fun cleanup()
}

class AutoCloseableThunk(private val resource: AutoCloseable) : Thunk {
    override suspend fun cleanup() {
        withContext(Dispatchers.IO) {
            resource.close()
        }
        logger.info { "Resource $resource closed." }
    }
}
