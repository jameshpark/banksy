package org.jameshpark.banksy.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import java.time.Duration

private val logger = KotlinLogging.logger { }

fun launchApp(block: suspend CoroutineScope.() -> Unit) = try {
    runBlocking {
        val applicationJob = launch(Dispatchers.Default + CoroutineName("application-coroutine")) {
            logger.info { "Application started." }
            try {
                block()
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
                            // Let the JVM shutdown and take the underlying thread with it.
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
