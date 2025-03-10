package com.api.postgres.recommendations


import com.api.postgres.UserPostInteractionDto
import com.api.postgres.TrailerInteractionDto
import com.api.postgres.UserBehaviorProfile
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.max

/**
 * BehaviorProfiler analyzes user interactions to create behavior profiles
 * that inform the recommendation system about user preferences.
 */
@Service
class BehaviorProfiler(@Autowired private val jdbcTemplate: JdbcTemplate) {
    private val logger = LoggerFactory.getLogger(BehaviorProfiler::class.java)

    /**
     * Generate or update a user's behavior profile based on their interactions
     */
    fun generateUserProfile(userId: Int): UserBehaviorProfile {
        logger.info("Generating behavior profile for user $userId")

        try {
            // Get post interactions
            val postInteractions = getPostInteractions(userId)

            // Get trailer interactions
            val trailerInteractions = getTrailerInteractions(userId)

            // Calculate behavioral metrics
            val metrics = calculateBehaviorMetrics(postInteractions, trailerInteractions)

            // Determine dominant behavior type
            val dominantType = determineDominantType(metrics)

            // Create behavior profile
            val profile = UserBehaviorProfile(
                userId = userId,
                metrics = metrics,
                dominantType = dominantType,
                updatedAt = Timestamp.from(Instant.now())
            )

            // Store profile in database
            storeUserProfile(profile)

            return profile
        } catch (e: Exception) {
            logger.error("Error generating behavior profile for user $userId: ${e.message}")
            throw e
        }
    }

    /**
     * Get user's behavior profile from database, or generate if not exists
     */

    @Transactional
    fun getUserProfile(userId: Int): UserBehaviorProfile {
        try {
            // Try to get existing profile
            val existingProfile = jdbcTemplate.queryForObject("""
                SELECT user_id, profile, dominant_type, updated_at
                FROM user_behavior_profiles
                WHERE user_id = ?
            """, { rs, _ -> mapToProfile(rs) }, userId)

            if (existingProfile != null) {
                // Check if profile is stale (older than 1 day)
                val profileAge = ChronoUnit.DAYS.between(
                    existingProfile.updatedAt.toInstant(),
                    Instant.now()
                )

                if (profileAge >= 1) {
                    // Profile is stale, regenerate it
                    logger.info("User profile for $userId is stale, regenerating")
                    return generateUserProfile(userId)
                }

                return existingProfile
            }

            // No existing profile, generate a new one
            return generateUserProfile(userId)
        } catch (e: Exception) {
            logger.info("No existing profile found for user $userId, generating new one")
            return generateUserProfile(userId)
        }
    }

    /**
     * Get post interactions for a user
     */
    private fun getPostInteractions(userId: Int): List<UserPostInteractionDto> {
        return jdbcTemplate.query("""
            SELECT * FROM user_post_interactions
            WHERE user_id = ?
            ORDER BY start_timestamp DESC
        """, { rs, _ -> mapToPostInteraction(rs) }, userId)
    }

    /**
     * Get trailer interactions for a user
     */
    private fun getTrailerInteractions(userId: Int): List<TrailerInteractionDto> {
        return jdbcTemplate.query("""
            SELECT * FROM trailer_interactions
            WHERE user_id = ?
            ORDER BY start_timestamp DESC
        """, { rs, _ -> mapToTrailerInteraction(rs) }, userId)
    }

    /**
     * Calculate behavioral metrics from interactions
     */
    private fun calculateBehaviorMetrics(
        postInteractions: List<UserPostInteractionDto>,
        trailerInteractions: List<TrailerInteractionDto>
    ): Map<String, Float> {
        val metrics = mutableMapOf<String, Float>()

        // Calculate metrics from post interactions
        if (postInteractions.isNotEmpty()) {
            // Interaction preferences
            val totalPostInteractions = postInteractions.size.toFloat()
            val likeRatio = postInteractions.count { it.likeState } / totalPostInteractions
            val saveRatio = postInteractions.count { it.saveState } / totalPostInteractions
            val commentRatio = postInteractions.count { it.commentButtonPressed } / totalPostInteractions

            metrics["post_like_ratio"] = likeRatio
            metrics["post_save_ratio"] = saveRatio
            metrics["post_comment_ratio"] = commentRatio

            // Engagement time metrics
            val avgPostEngagementDuration = postInteractions.map {
                calculateDuration(it.startTimestamp, it.endTimestamp)
            }.average().toFloat()

            metrics["avg_post_engagement_duration"] = avgPostEngagementDuration
        } else {
            // Default values if no post interactions
            metrics["post_like_ratio"] = 0.5f
            metrics["post_save_ratio"] = 0.5f
            metrics["post_comment_ratio"] = 0.2f
            metrics["avg_post_engagement_duration"] = 60f // Assume 60 seconds
        }

        // Calculate metrics from trailer interactions
        if (trailerInteractions.isNotEmpty()) {
            // Interaction preferences
            val totalTrailerInteractions = trailerInteractions.size.toFloat()
            val trailerLikeRatio = trailerInteractions.count { it.likeState } / totalTrailerInteractions
            val trailerSaveRatio = trailerInteractions.count { it.saveState } / totalTrailerInteractions
            val trailerCommentRatio = trailerInteractions.count { it.commentButtonPressed } / totalTrailerInteractions

            metrics["trailer_like_ratio"] = trailerLikeRatio
            metrics["trailer_save_ratio"] = trailerSaveRatio
            metrics["trailer_comment_ratio"] = trailerCommentRatio

            // Trailer-specific metrics
            val avgReplayCount = trailerInteractions.map { it.replayCount.toFloat() }.average().toFloat()
            val muteRatio = trailerInteractions.count { it.isMuted } / totalTrailerInteractions

            metrics["avg_trailer_replay_count"] = avgReplayCount
            metrics["trailer_mute_ratio"] = muteRatio

            // Engagement time metrics
            val avgTrailerEngagementDuration = trailerInteractions.map {
                calculateDuration(it.startTimestamp, it.endTimestamp)
            }.average().toFloat()

            metrics["avg_trailer_engagement_duration"] = avgTrailerEngagementDuration
        } else {
            // Default values if no trailer interactions
            metrics["trailer_like_ratio"] = 0.5f
            metrics["trailer_save_ratio"] = 0.5f
            metrics["trailer_comment_ratio"] = 0.2f
            metrics["avg_trailer_replay_count"] = 1.0f
            metrics["trailer_mute_ratio"] = 0.2f
            metrics["avg_trailer_engagement_duration"] = 30f // Assume 30 seconds
        }

        // Calculate overall preferences
        val postInteractionCount = postInteractions.size.toFloat()
        val trailerInteractionCount = trailerInteractions.size.toFloat()
        val totalInteractions = postInteractionCount + trailerInteractionCount

        if (totalInteractions > 0) {
            metrics["post_preference_ratio"] = postInteractionCount / totalInteractions
            metrics["trailer_preference_ratio"] = trailerInteractionCount / totalInteractions
        } else {
            metrics["post_preference_ratio"] = 0.5f
            metrics["trailer_preference_ratio"] = 0.5f
        }

        return metrics
    }

    /**
     * Determine the dominant behavior type based on metrics
     */
    private fun determineDominantType(metrics: Map<String, Float>): String {
        // Define behavior dimensions and their scores
        val dimensions = mapOf(
            "content_saver" to (metrics["post_save_ratio"] ?: 0f) + (metrics["trailer_save_ratio"] ?: 0f),
            "content_liker" to (metrics["post_like_ratio"] ?: 0f) + (metrics["trailer_like_ratio"] ?: 0f),
            "commenter" to (metrics["post_comment_ratio"] ?: 0f) + (metrics["trailer_comment_ratio"] ?: 0f),
            "trailer_focused" to (metrics["trailer_preference_ratio"] ?: 0f) * 2,
            "content_focused" to (metrics["post_preference_ratio"] ?: 0f) * 2
        )

        // Find dimension with highest score
        return dimensions.maxByOrNull { it.value }?.key ?: "content_focused"
    }

    /**
     * Store user profile in database
     */
    private fun storeUserProfile(profile: UserBehaviorProfile) {
        // Convert metrics to JSON
        val metricsJson = profile.metrics.entries.joinToString(",", "{", "}") {
            "\"${it.key}\": ${it.value}"
        }

        jdbcTemplate.update("""
            INSERT INTO user_behavior_profiles (user_id, profile, dominant_type, updated_at)
            VALUES (?, ?::json, ?, ?)
            ON CONFLICT (user_id) DO UPDATE
            SET profile = ?::json, dominant_type = ?, updated_at = ?
        """,
            profile.userId,
            metricsJson,
            profile.dominantType,
            profile.updatedAt,
            metricsJson,
            profile.dominantType,
            profile.updatedAt)
    }

    /**
     * Map ResultSet to UserBehaviorProfile
     */
    private fun mapToProfile(rs: ResultSet): UserBehaviorProfile {
        val userId = rs.getInt("user_id")
        val dominantType = rs.getString("dominant_type")
        val updatedAt = rs.getTimestamp("updated_at")

        // Parse JSON profile
        val profileJson = rs.getString("profile")
        val metrics = parseProfileJson(profileJson)

        return UserBehaviorProfile(userId, metrics, dominantType, updatedAt)
    }

    /**
     * Parse profile JSON to Map
     */
    private fun parseProfileJson(json: String): Map<String, Float> {
        // Simple JSON parsing (in production, use a proper JSON library)
        val metrics = mutableMapOf<String, Float>()

        val cleanJson = json.trim('{', '}')
        val pairs = cleanJson.split(",")

        for (pair in pairs) {
            val keyValue = pair.split(":")
            if (keyValue.size == 2) {
                val key = keyValue[0].trim('"', ' ')
                val value = keyValue[1].trim().toFloatOrNull() ?: 0f
                metrics[key] = value
            }
        }

        return metrics
    }

    /**
     * Map ResultSet to UserPostInteractionDto
     */
    private fun mapToPostInteraction(rs: ResultSet): UserPostInteractionDto {
        return UserPostInteractionDto(
            interactionId = rs.getInt("interaction_id"),
            userId = rs.getInt("user_id"),
            postId = rs.getInt("post_id"),
            startTimestamp = rs.getString("start_timestamp"),
            endTimestamp = rs.getString("end_timestamp"),
            likeState = rs.getBoolean("like_state"),
            saveState = rs.getBoolean("save_state"),
            commentButtonPressed = rs.getBoolean("comment_button_pressed")
        )
    }

    /**
     * Map ResultSet to TrailerInteractionDto
     */
    private fun mapToTrailerInteraction(rs: ResultSet): TrailerInteractionDto {
        return TrailerInteractionDto(
            interactionId = rs.getInt("interaction_id"),
            userId = rs.getInt("user_id"),
            postId = rs.getInt("post_id"),
            startTimestamp = rs.getString("start_timestamp"),
            endTimestamp = rs.getString("end_timestamp"),
            replayCount = rs.getInt("replay_count"),
            isMuted = rs.getBoolean("is_muted"),
            likeState = rs.getBoolean("like_state"),
            saveState = rs.getBoolean("save_state"),
            commentButtonPressed = rs.getBoolean("comment_button_pressed")
        )
    }

    /**
     * Calculate duration between two timestamps
     */
    private fun calculateDuration(start: String, end: String): Float {
        try {
            val startTime = Timestamp.valueOf(start).time
            val endTime = Timestamp.valueOf(end).time
            return max(0, endTime - startTime).toFloat() / 1000f // Duration in seconds
        } catch (e: Exception) {
            return 0f
        }
    }
}

/**
 * Data class representing a user's behavior profile
 */
