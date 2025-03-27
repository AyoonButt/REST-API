package com.api.postgres.recommendations

import com.api.postgres.UserDto
import com.api.postgres.UserGenreDto
import com.api.postgres.UserPostInteractionDto
import com.api.postgres.UserSubscriptionDto
import com.api.postgres.TrailerInteractionDto
import org.slf4j.LoggerFactory
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.sql.ResultSet

/**
 * Service function to regenerate user vector after preference changes
 */
@Service
class UserVectorService(
    private val jdbcTemplate: JdbcTemplate,
    private val vectorService: VectorService
) {
    private val logger = LoggerFactory.getLogger(UserVectorService::class.java)

    /**
     * Regenerate and store user vector after preferences change
     */
    fun regenerateUserVectorAfterPreferenceChange(userId: Int) {
        try {
            // 1. Fetch updated user data with new preferences
            val userData = fetchUserData(userId)

            // 2. Generate new vector with updated preferences
            val newVector = vectorService.createUserVector(
                userData.userDto,
                userData.genres,
                userData.postInteractions,
                userData.trailerInteractions
            )

            // 3. Store updated vector
            vectorService.storeUserVector(userId, newVector)

            logger.info("User vector regenerated for user $userId after preference update")
        } catch (e: Exception) {
            logger.error("Failed to regenerate vector for user $userId: ${e.message}")
            throw e
        }
    }

    /**
     * Fetch all user data needed for vector generation
     */
    private fun fetchUserData(userId: Int): UserData {
        // Get user preferences
        val userDto = jdbcTemplate.queryForObject(
            "SELECT * FROM users WHERE user_id = ?",
            { rs, _ -> mapToUserDto(rs) },
            userId
        ) ?: throw NotFoundException()

        // Get updated subscription preferences
        val subscriptions = jdbcTemplate.query("""
            SELECT us.user_id, us.provider_id, sp.provider_name, us.priority
            FROM user_subscriptions us
            JOIN subscription_providers sp ON us.provider_id = sp.provider_id
            WHERE us.user_id = ?
        """, { rs, _ -> mapToSubscriptionDto(rs) }, userId)

        // Get updated genre preferences
        val genres = jdbcTemplate.query("""
            SELECT ug.user_id, ug.genre_id, g.genre_name, ug.priority
            FROM user_genres ug
            JOIN genres g ON ug.genre_id = g.genre_id
            WHERE ug.user_id = ?
        """, { rs, _ -> mapToGenreDto(rs) }, userId)

        // Get recent post interactions
        val postInteractions = jdbcTemplate.query("""
            SELECT * FROM user_post_interactions
            WHERE user_id = ?
            ORDER BY start_timestamp DESC
            LIMIT 100
        """, { rs, _ -> mapToPostInteractionDto(rs) }, userId)

        // Get recent trailer interactions
        val trailerInteractions = jdbcTemplate.query("""
            SELECT * FROM user_trailer_interactions
            WHERE user_id = ?
            ORDER BY start_timestamp DESC
            LIMIT 100
        """, { rs, _ -> mapToTrailerInteractionDto(rs) }, userId)

        return UserData(userDto, subscriptions, genres, postInteractions, trailerInteractions)
    }

    // Data class to hold all user information
    data class UserData(
        val userDto: UserDto,
        val subscriptions: List<UserSubscriptionDto>,
        val genres: List<UserGenreDto>,
        val postInteractions: List<UserPostInteractionDto>,
        val trailerInteractions: List<TrailerInteractionDto>
    )

    // Mapping methods (implement these)
    private fun mapToUserDto(rs: ResultSet): UserDto {
        // Implementation for mapping ResultSet to UserDto
        return UserDto(
            userId = rs.getInt("user_id"),
            name = rs.getString("name"),
            username = rs.getString("username"),
            password = "", // Don't include actual password
            email = rs.getString("email"),
            language = rs.getString("language"),
            region = rs.getString("region"),
            minMovie = rs.getObject("min_movie") as Int?,
            maxMovie = rs.getObject("max_movie") as Int?,
            minTV = rs.getObject("min_tv") as Int?,
            maxTV = rs.getObject("max_tv") as Int?,
            oldestDate = rs.getString("oldest_date"),
            recentDate = rs.getString("recent_date"),
            createdAt = rs.getString("created_at"),
            recentLogin = rs.getString("recent_login")
        )
    }

    private fun mapToSubscriptionDto(rs: ResultSet): UserSubscriptionDto {
        return UserSubscriptionDto(
            userId = rs.getInt("user_id"),
            providerId = rs.getInt("provider_id"),
            providerName = rs.getString("provider_name"),
            priority = rs.getInt("priority")
        )
    }

    private fun mapToGenreDto(rs: ResultSet): UserGenreDto {
        return UserGenreDto(
            userId = rs.getInt("user_id"),
            genreId = rs.getInt("genre_id"),
            genreName = rs.getString("genre_name"),
            priority = rs.getInt("priority")
        )
    }

    private fun mapToPostInteractionDto(rs: ResultSet): UserPostInteractionDto {
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

    private fun mapToTrailerInteractionDto(rs: ResultSet): TrailerInteractionDto {
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
}