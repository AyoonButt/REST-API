package com.api.postgres.recommendations


import com.api.postgres.MLRecommendationRequest
import com.api.postgres.MLRecommendationResponse
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Client for communicating with the ML model service
 */
@Service
class MLModelClient(
    private val restTemplate: RestTemplate,
    @Value("\${ml.model.url:http://localhost:5000}") private val mlModelUrl: String,
    @Value("\${ml.connection.timeout:5000}") private val connectionTimeout: Int,
) {
    private val logger = LoggerFactory.getLogger(MLModelClient::class.java)

    // Cache for session IDs to prevent duplicates if Redis isn't available in ML service
    private val sessionRecommendations = ConcurrentHashMap<String, MutableSet<Int>>()

    // Executor for cleanup task
    private val cleanupExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    init {
        // Schedule cleanup of old session data every day
        cleanupExecutor.scheduleAtFixedRate(
            { cleanupOldSessions() },
            1,
            24,
            TimeUnit.HOURS
        )

        // Add shutdown hook to properly terminate the executor
        Runtime.getRuntime().addShutdownHook(Thread {
            cleanupExecutor.shutdown()
            try {
                if (!cleanupExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow()
                }
            } catch (e: InterruptedException) {
                cleanupExecutor.shutdownNow()
            }
        })
    }

    /**
     * Get recommendations from ML model
     */
    fun getRecommendations(request: MLRecommendationRequest): MLRecommendationResponse? {
        try {
            logger.info("Requesting recommendations from ML model for user ${request.userId}")

            // Apply local duplicate prevention if enabled
            val modifiedRequest = request


            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_JSON

            val entity = HttpEntity(modifiedRequest, headers)

            // Make the request to ML model service
            val response = restTemplate.postForObject(
                "$mlModelUrl/recommendations",
                entity,
                MLRecommendationResponse::class.java
            )

            return response

        } catch (e: Exception) {
            logger.error("Error communicating with ML model: ${e.message}")
            return null
        }
    }



    /**
     * Clean up old session data (older than 30 days)
     */
    private fun cleanupOldSessions() {
        try {
            logger.info("Cleaning up old session recommendations data")

            // Extract session timestamps
            val sessionsToRemove = sessionRecommendations.keys
                .filter { key ->
                    val parts = key.split("_")
                    if (parts.size >= 3) {
                        val timestamp = parts.last().toLongOrNull() ?: return@filter false
                        val age = System.currentTimeMillis() - timestamp
                        // Remove if older than 30 days
                        age > TimeUnit.DAYS.toMillis(30)
                    } else {
                        false
                    }
                }

            // Remove old sessions
            sessionsToRemove.forEach { sessionRecommendations.remove(it) }

            logger.info("Cleaned up ${sessionsToRemove.size} old sessions")
        } catch (e: Exception) {
            logger.error("Error cleaning up old sessions: ${e.message}")
        }
    }
}