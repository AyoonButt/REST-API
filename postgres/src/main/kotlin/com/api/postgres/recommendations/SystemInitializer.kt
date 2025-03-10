package com.api.postgres.recommendations

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class SystemInitializer(
    private val vectorInitializationService: VectorInitialization,
    private val jdbcTemplate: JdbcTemplate
) {
    private val logger = LoggerFactory.getLogger(SystemInitializer::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        try {
            logger.info("Checking if vector initialization is needed...")

            // Check if vectors exist
            val userVectorCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_vectors",
                Int::class.java
            ) ?: 0

            val postVectorCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM post_vectors",
                Int::class.java
            ) ?: 0

            // If no vectors exist, initialize them
            if (userVectorCount == 0 || postVectorCount == 0) {
                logger.info("Starting vector initialization...")
                vectorInitializationService.initializeAllVectors()
                logger.info("Vector initialization complete")
            } else {
                logger.info("Vectors already initialized: $userVectorCount users, $postVectorCount posts")
            }
        } catch (e: Exception) {
            logger.error("Error during vector initialization: ${e.message}")
        }
    }
}