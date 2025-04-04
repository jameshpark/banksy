package org.jameshpark.banksy.clients

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.basicAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.path
import io.ktor.serialization.jackson.jackson
import java.time.LocalDate
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import nl.altindag.ssl.SSLFactory
import nl.altindag.ssl.pem.util.PemUtils
import org.jameshpark.banksy.models.Status
import org.jameshpark.banksy.models.TellerTransaction
import org.jameshpark.banksy.utils.require


class TellerClient(private val httpClient: HttpClient) : AutoCloseable {

    override fun close() {
        httpClient.close()
    }

    // teller returns transactions in DESC order (i.e. newest to oldest)
    // a bookmark aka transaction id in the request returns a response
    // with transactions starting at the transaction just before the given id
    // (which is the one right after if we're traveling back in time)


    /**
     * strategy will be
     * 1. get transactions from teller
     * 2. takeWhile date > bookmark in database
     * 3. if all transactions' dates > bookmark, get the next page
     * 4. repeat
     */
    fun getTransactions(
        accountId: String,
        accessToken: String,
        bookmark: LocalDate,
        pageSize: Int = 1000,
        pageStartId: String? = null
    ): Flow<TellerTransaction> = flow {
        var fromId = pageStartId
        do {
            val page = withBackoff {
                nextPage(accessToken, accountId, pageSize, fromId)
            }
            val posted = page.filter { it.status == Status.POSTED }
            val filtered = posted.filter { it.date > bookmark }

            fromId = page.lastOrNull()?.id

            filtered.forEach { emit(it) }
        } while (posted.all { it.date > bookmark })
    }

    private suspend fun <T> withBackoff(attempts: Int = 5, block: suspend () -> T): T {
        var delayMillis = 1L
        for (attempt in 1..attempts) {
            try {
                return block()
            } catch (e: Exception) {
                logger.error(e) { "Error making request to teller. Attempt $attempt of $attempts" }
                if (attempt == attempts) {
                    throw e
                }
            }
            delayMillis *= 2_000L
            delay(delayMillis)
        }
        throw IllegalStateException("Should not be able to reach this point")
    }

    private suspend fun nextPage(
        accessToken: String,
        accountId: String,
        pageSize: Int,
        fromId: String?
    ): List<TellerTransaction> {
        val response = httpClient.get {
            url {
                path("accounts", accountId, "transactions")
                parameters.append("count", pageSize.toString())
                fromId?.let { parameters.append("from_id", it) }
            }
            basicAuth(accessToken, NO_PASSWORD)
        }

        return if (response.status.isSuccess()) {
            response.body<List<TellerTransaction>>()
        } else {
            val responseBody = response.bodyAsText()
            logger.error { "Error getting transactions for accountId=$accountId, accessToken=$accessToken. ${response.status}. Response body: $responseBody" }
            throw ClientRequestException(response, responseBody)
        }
    }

    companion object {

        private val logger = KotlinLogging.logger { }

        private const val NO_PASSWORD = ""

        fun fromProperties(properties: Properties): TellerClient {
            val httpClient = createHttpClient(
                properties.require("teller.client.certificate.path"),
                properties.require("teller.client.private-key.path")
            )
            return TellerClient(httpClient)
        }

        private fun createHttpClient(certificatePath: String, privateKeyPath: String): HttpClient {
            val keyManager = PemUtils.loadIdentityMaterial(certificatePath, privateKeyPath)
//            val keyManager = PemUtils.loadIdentityMaterial(
//                FileInputStream("secrets/certificate.pem"),
//                FileInputStream("secrets/private_key.pem")
//            )
            val sslFactory = SSLFactory.builder()
                .withIdentityMaterial(keyManager)
                .withDefaultTrustMaterial()
                .build()!!

            return HttpClient(OkHttp) {
                engine {
                    config {
                        sslSocketFactory(sslFactory.sslSocketFactory, sslFactory.trustManager.get())
                    }
                }
                install(ContentNegotiation) {
                    jackson {
                        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        registerModule(JavaTimeModule())
                    }
                }
                defaultRequest {
                    url("https://api.teller.io/")
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 60000
                    connectTimeoutMillis = 30000
                    socketTimeoutMillis = 60000
                }

            }
        }

    }

}