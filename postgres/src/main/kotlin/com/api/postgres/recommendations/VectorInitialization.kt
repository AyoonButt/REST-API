package com.api.postgres.recommendations

import com.api.postgres.MediaDto
import com.api.postgres.PostDto
import com.api.postgres.UserDto
import com.api.postgres.UserGenreDto
import com.api.postgres.UserPostInteractionDto
import com.api.postgres.UserSubscriptionDto
import com.api.postgres.TrailerInteractionDto
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.sql.ResultSet

@Service
class VectorInitialization(
    private val jdbcTemplate: JdbcTemplate,
    private val vectorService: VectorService
) {
    private val logger = LoggerFactory.getLogger(VectorInitialization::class.java)

    /**
     * Initialize vectors for all users and posts
     */
    fun initializeAllVectors() {
        initializeUserVectors()
        initializePostVectors()
    }

    fun initializeUserVectors() {
        val userIds = jdbcTemplate.queryForList("SELECT user_id FROM users", Int::class.java)
        logger.info("Initializing vectors for ${userIds.size} users")

        userIds.forEach { userId ->
            try {
                // Check if vector already exists
                val exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM user_vectors WHERE user_id = ?",
                    Int::class.java,
                    userId
                ) ?: 0

                if (exists == 0) {
                    initializeUserVector(userId)
                }
            } catch (e: Exception) {
                logger.error("Error initializing vector for user $userId: ${e.message}")
            }
        }
    }

    fun initializePostVectors() {
        val postIds = jdbcTemplate.queryForList("SELECT post_id FROM posts", Int::class.java)
        logger.info("Initializing vectors for ${postIds.size} posts")

        postIds.forEach { postId ->
            try {
                // Check if vector already exists
                val exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM post_vectors WHERE post_id = ?",
                    Int::class.java,
                    postId
                ) ?: 0

                if (exists == 0) {
                    initializePostVector(postId)
                }
            } catch (e: Exception) {
                logger.error("Error initializing vector for post $postId: ${e.message}")
            }
        }
    }

    fun initializeUserVector(userId: Int) {
        try {
            // Get user data
            val userData = getUserData(userId)

            // Create initial vector
            val vector = vectorService.createUserVector(
                userData.userDto,
                userData.genres,
                userData.interactions,
                userData.trailerInteractions
            )

            // Store vector
            vectorService.storeUserVector(userId, vector)

            logger.info("Initialized vector for user $userId")
        } catch (e: Exception) {
            logger.error("Error initializing vector for user $userId: ${e.message}")
            throw e
        }
    }

    fun initializePostVector(postId: Int) {
        try {
            // Get post data
            val postData = getPostData(postId)

            // Create initial vector
            val vector = vectorService.createPostVector(postData.postDto)

            // Store vector
            vectorService.storePostVector(postId, vector)

            logger.info("Initialized vector for post $postId")
        } catch (e: Exception) {
            logger.error("Error initializing vector for post $postId: ${e.message}")
            throw e
        }
    }

    /**
     * Get user data needed for vector creation
     */
    private fun getUserData(userId: Int): UserData {
        // Get user basic info
        val userDto = jdbcTemplate.queryForObject(
            "SELECT * FROM users WHERE user_id = ?",
            { rs, _ -> mapToUserDto(rs) },
            userId
        ) ?: throw Exception("User not found")

        // Get genres
        val genres = jdbcTemplate.query("""
            SELECT ug.user_id, ug.genre_id, g.name as genre_name, ug.priority
            FROM user_genres ug
            JOIN genres g ON ug.genre_id = g.genre_id
            WHERE ug.user_id = ?
        """, { rs, _ -> mapToGenreDto(rs) }, userId)

        // Get post interactions
        val postInteractions = jdbcTemplate.query("""
            SELECT * FROM user_post_interactions
            WHERE user_id = ?
            ORDER BY start_timestamp DESC
            LIMIT 100
        """, { rs, _ -> mapToPostInteractionDto(rs) }, userId)

        // Get trailer interactions
        val trailerInteractions = jdbcTemplate.query("""
            SELECT * FROM trailer_interactions
            WHERE user_id = ?
            ORDER BY start_timestamp DESC
            LIMIT 100
        """, { rs, _ -> mapToTrailerInteractionDto(rs) }, userId)

        return UserData(
            userDto = userDto,
            genres = genres,
            interactions = postInteractions,
            trailerInteractions = trailerInteractions
        )
    }

    /**
     * Get post data with optional media info
     */
    private fun getPostData(postId: Int): PostData {
        // Get post basic info
        val postDto = jdbcTemplate.queryForObject(
            "SELECT * FROM posts WHERE post_id = ?",
            { rs, _ -> mapToPostDto(rs) },
            postId
        ) ?: throw Exception("Post not found")

        // Try to get media data (this would be from your TMDB API service in a real implementation)
        val mediaDto = try {
            getMediaForTmdbId(postDto.tmdbId, postDto.type)
        } catch (e: Exception) {
            logger.warn("Could not get media data for post $postId: ${e.message}")
            null
        }

        return PostData(
            postDto = postDto,
            mediaDto = mediaDto
        )
    }

    /**
     * Get media data for a TMDB ID (placeholder implementation)
     */
    private fun getMediaForTmdbId(tmdbId: Int, type: String): MediaDto? {
        // In a real implementation, this would call your TMDB API service
        // This is a placeholder that returns null - implement your actual API call here
        return null
    }

    // Mapping methods (similar to UserVectorService)
    private fun mapToUserDto(rs: ResultSet): UserDto {
        // Implementation
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

    private fun mapToPostDto(rs: ResultSet): PostDto {
        return PostDto(
            postId = rs.getInt("post_id"),
            tmdbId = rs.getInt("tmdb_id"),
            type = rs.getString("type"),
            title = rs.getString("title"),
            subscription = rs.getString("subscription"),
            releaseDate = rs.getString("release_date"),
            overview = rs.getString("overview"),
            posterPath = rs.getString("poster_path"),
            voteAverage = rs.getDouble("vote_average"),
            voteCount = rs.getInt("vote_count"),
            originalLanguage = rs.getString("original_language"),
            originalTitle = rs.getString("original_title"),
            popularity = rs.getDouble("popularity"),
            genreIds = rs.getString("genre_ids"),
            postLikeCount = rs.getInt("post_like_count"),
            trailerLikeCount = rs.getInt("trailer_like_count"),
            videoKey = rs.getString("video_key")
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

    data class UserData(
        val userDto: UserDto,
        val genres: List<UserGenreDto>,
        val interactions: List<UserPostInteractionDto>,
        val trailerInteractions: List<TrailerInteractionDto>
    )

    data class PostData(
        val postDto: PostDto,
        val mediaDto: MediaDto?
    )
}