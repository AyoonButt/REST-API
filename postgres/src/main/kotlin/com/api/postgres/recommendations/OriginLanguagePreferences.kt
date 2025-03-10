package com.api.postgres.recommendations

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Service for calculating and updating user language preferences
 * based on their liked and saved posts
 */
@Service
class OriginLanguagePreferences(
    private val jdbcTemplate: JdbcTemplate,
    private val metadataService: MetadataService
) {
    private val logger = LoggerFactory.getLogger(OriginLanguagePreferences::class.java)
    private val objectMapper = ObjectMapper()

    /**
     * Ensure the user_vector_metadata table has the language_weights column
     */
    fun initializeSchema() {
        try {
            // Add language_weights column if it doesn't exist
            jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF NOT EXISTS (
                        SELECT 1 FROM information_schema.columns 
                        WHERE table_name = 'user_vector_metadata' AND column_name = 'language_weights'
                    ) THEN
                        ALTER TABLE user_vector_metadata ADD COLUMN language_weights JSONB;
                    END IF;
                END $$;
            """)

            logger.info("Schema updated to support language weights")
        } catch (e: Exception) {
            logger.error("Error updating schema: ${e.message}")
            throw e
        }
    }

    /**
     * Calculate and update a user's language preferences based on liked and saved posts
     */
    @Transactional
    fun updateUserLanguagePreferences(userId: Int) {
        try {
            // First get languages from liked posts
            val likedLanguages = jdbcTemplate.queryForList("""
                SELECT DISTINCT p.original_language
                FROM user_post_interactions upi
                JOIN posts p ON upi.post_id = p.post_id
                WHERE upi.user_id = ? 
                AND upi.like_state = true
                AND p.original_language IS NOT NULL
            """, String::class.java, userId)

            // Then get languages from saved posts
            val savedLanguages = jdbcTemplate.queryForList("""
                SELECT DISTINCT p.original_language
                FROM user_post_interactions upi
                JOIN posts p ON upi.post_id = p.post_id
                WHERE upi.user_id = ? 
                AND upi.save_state = true
                AND p.original_language IS NOT NULL
            """, String::class.java, userId)

            // Combine both lists
            val allLanguages = likedLanguages + savedLanguages

            if (allLanguages.isEmpty()) {
                logger.info("No language data found for user $userId")
                return
            }

            // Count frequency of each language
            val languageCounts = allLanguages.groupingBy { it }.eachCount()

            // Calculate weights (normalize counts)
            val totalInteractions = languageCounts.values.sum().toFloat()
            val languageWeights = languageCounts.mapValues { (_, count) ->
                (count.toFloat() / totalInteractions).toDouble()
            }

            // Add language name mappings
            val languageNames = getCommonLanguageNames()

            // Create the weights JSON object
            val weights = mapOf(
                "weights" to languageWeights,
                "languageNames" to languageNames,
                "topLanguages" to languageWeights.entries.sortedByDescending { it.value }.take(5)
                    .associate { it.key to it.value },
                "updatedAt" to LocalDateTime.now().toString()
            )

            // Convert to JSON
            val weightsJson = objectMapper.writeValueAsString(weights)

            // Update the user's vector metadata with language weights
            jdbcTemplate.update("""
                UPDATE user_vector_metadata
                SET language_weights = ?::jsonb, updated_at = NOW()
                WHERE user_id = ?
            """, weightsJson, userId)

            // Check if row was updated
            val rowsAffected = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_vector_metadata WHERE user_id = ?",
                Int::class.java,
                userId
            ) ?: 0

            // If no rows updated, insert a new row
            if (rowsAffected == 0) {
                jdbcTemplate.update("""
                    INSERT INTO user_vector_metadata (user_id, language_weights, created_at, updated_at)
                    VALUES (?, ?::jsonb, NOW(), NOW())
                """, userId, weightsJson)
            }

            // Also update the more_information field for backward compatibility
            updateMoreInfoField(userId, weights)

            logger.info("Updated language preferences for user $userId with ${languageWeights.size} languages")
        } catch (e: Exception) {
            logger.error("Error updating language preferences for user $userId: ${e.message}")
            throw e
        }
    }

    /**
     * Update more_information JSONB field with language preferences
     */
    private fun updateMoreInfoField(userId: Int, weights: Map<String, Any>) {
        try {
            val metadataResult = metadataService.getUserMetadata(userId)

            if (metadataResult != null) {
                val moreInfo = metadataResult.moreInformation?.toMutableMap() ?: mutableMapOf()

                // Add language preferences to more_information
                moreInfo["languagePreferences"] = weights

                // Store updated metadata
                metadataService.storeUserMetadata(userId, metadataResult.comment, moreInfo)
            }
        } catch (e: Exception) {
            logger.warn("Error updating more_information field with language preferences: ${e.message}")
            // This is not critical enough to throw an exception
        }
    }

    /**
     * Get language name mappings for common language codes
     */
    private fun getCommonLanguageNames(): Map<String, String> {
        return mapOf(
            "en" to "English",
            "ja" to "Japanese",
            "ko" to "Korean",
            "hi" to "Hindi",
            "fr" to "French",
            "es" to "Spanish",
            "de" to "German",
            "it" to "Italian",
            "zh" to "Chinese",
            "ru" to "Russian",
            "pt" to "Portuguese",
            "tr" to "Turkish",
            "ar" to "Arabic",
            "th" to "Thai",
            "id" to "Indonesian",
            "tl" to "Tagalog",
            "vi" to "Vietnamese",
            "sv" to "Swedish",
            "da" to "Danish",
            "fi" to "Finnish",
            "nl" to "Dutch",
            "no" to "Norwegian",
            "pl" to "Polish",
            "hu" to "Hungarian",
            "cs" to "Czech",
            "el" to "Greek",
            "he" to "Hebrew",
            "fa" to "Persian"
        )
    }


    /**
     * Update language preferences for all users
     * This can be scheduled to run periodically
     */
    fun updateAllUsersLanguagePreferences() {
        try {
            val userIds = jdbcTemplate.queryForList(
                "SELECT user_id FROM users", Int::class.java)

            var successCount = 0
            var errorCount = 0

            for (userId in userIds) {
                try {
                    updateUserLanguagePreferences(userId)
                    successCount++

                    if (successCount % 100 == 0) {
                        logger.info("Updated language preferences for $successCount users")
                    }
                } catch (e: Exception) {
                    errorCount++
                    logger.warn("Error updating language preferences for user $userId: ${e.message}")
                }
            }

            logger.info("Completed updating language preferences. Success: $successCount, Errors: $errorCount")
        } catch (e: Exception) {
            logger.error("Error updating language preferences for all users: ${e.message}")
        }
    }
}