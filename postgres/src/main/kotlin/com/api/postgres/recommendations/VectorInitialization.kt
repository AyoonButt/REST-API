package com.api.postgres.recommendations

import com.api.postgres.MediaDto
import com.api.postgres.PostDto
import com.api.postgres.UserDto
import com.api.postgres.UserGenreDto
import com.api.postgres.UserPostInteractionDto
import com.api.postgres.UserSubscriptionDto
import com.api.postgres.TrailerInteractionDto
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet

@Service
class VectorInitialization(
    private val jdbcTemplate: JdbcTemplate,
    private val vectorService: VectorService,
    private val metadataService: MetadataService
) {
    private val logger = LoggerFactory.getLogger(VectorInitialization::class.java)

    @PostConstruct
    fun initialize() {
        try {
            // Ensure metadata tables exist
            metadataService.initializeMetadataTables()

            logger.info("Vector and metadata tables initialized")
        } catch (e: Exception) {
            logger.error("Error during initialization: ${e.message}")
        }
    }

    /**
     * Initialize vectors for all users and posts
     */
    fun initializeAllVectors() {
        initializeUserVectors()
        initializePostVectors()
    }

    @Transactional
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

                    // Also generate and store user metadata
                    updateUserMetadata(userId)
                }
            } catch (e: Exception) {
                logger.error("Error initializing vector for user $userId: ${e.message}")
            }
        }
    }

    @Transactional
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

                    // Also generate and store post metadata
                    updatePostMetadata(postId)
                }
            } catch (e: Exception) {
                logger.error("Error initializing vector for post $postId: ${e.message}")
            }
        }
    }

    @Transactional
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

            // Update metadata
            updateUserMetadata(userId)

            logger.info("Initialized vector for user $userId")
        } catch (e: Exception) {
            logger.error("Error initializing vector for user $userId: ${e.message}")
            throw e
        }
    }

    @Transactional
    fun initializePostVector(postId: Int) {
        try {
            // Get post data
            val postData = getPostData(postId)

            // Create initial vector
            val vector = vectorService.createPostVector(postData.postDto)

            // Store vector
            vectorService.storePostVector(postId, vector)

            // Update metadata
            updatePostMetadata(postId)

            logger.info("Initialized vector for post $postId")
        } catch (e: Exception) {
            logger.error("Error initializing vector for post $postId: ${e.message}")
            throw e
        }
    }

    /**
     * Update user metadata
     */
    private fun updateUserMetadata(userId: Int) {
        try {
            // Get user data
            val userDto = jdbcTemplate.queryForObject(
                "SELECT * FROM users WHERE user_id = ?",
                { rs, _ -> mapToUserDto(rs) },
                userId
            ) ?: throw Exception("User not found")

            // Get user subscriptions
            val subscriptions = try {
                jdbcTemplate.query("""
                        SELECT 
                            us.user_id as userId,
                            us.provider_id as providerId,
                            sp.provider_name as providerName,  
                            us.priority
                        FROM user_subscriptions us
                        JOIN subscription_providers sp ON us.provider_id = sp.provider_id
                        WHERE us.user_id = ?
                        ORDER BY us.priority
                    """, { rs, _ ->
                    UserSubscriptionDto(
                        userId = rs.getInt("userId"),
                        providerId = rs.getInt("providerId"),
                        providerName = rs.getString("providerName"),
                        priority = rs.getInt("priority")
                    )
                }, userId)
            } catch (e: Exception) {
                logger.warn("Error getting user subscriptions: ${e.message}")
                emptyList()
            }
            // Generate metadata
            val metadata = metadataService.generateUserMetadata(
                userDto = userDto,
                subscriptions = subscriptions,
                comment = "Auto-generated by VectorInitialization"
            )

            // Store metadata
            metadataService.storeUserMetadata(userId, "Vector initialization", metadata)

            logger.info("Updated metadata for user $userId")
        } catch (e: Exception) {
            logger.error("Error updating metadata for user $userId: ${e.message}")
        }
    }

    /**
     * Update post metadata
     */
    private fun updatePostMetadata(postId: Int) {
        try {
            // Get post data
            val postDto = jdbcTemplate.queryForObject(
                "SELECT * FROM posts WHERE post_id = ?",
                { rs, _ -> mapToPostDto(rs) },
                postId
            ) ?: throw Exception("Post not found")

            // Get cast information
            val cast = try {
                val tmdbId = postDto.tmdbId
                jdbcTemplate.query("""
                    SELECT * FROM cast_members
                    WHERE tmdb_id = ?
                    ORDER BY order_index
                """, { rs, _ ->
                    com.api.postgres.CastDto(
                        personId = rs.getInt("person_id"),
                        name = rs.getString("name"),
                        character = rs.getString("character"),
                        orderIndex = rs.getInt("order_index"),
                        popularity = rs.getDouble("popularity"),
                        profilePath = rs.getString("profile_path"),
                        gender = rs.getInt("gender"),
                        knownForDepartment = rs.getString("known_for_department"),
                        episodeCount = rs.getObject("episode_count") as Int
                    )
                }, tmdbId)
            } catch (e: Exception) {
                logger.warn("Error getting cast info: ${e.message}")
                emptyList()
            }

            // Get crew information
            val crew = try {
                val tmdbId = postDto.tmdbId
                jdbcTemplate.query("""
                    SELECT * FROM crew
                    WHERE tmdb_id = ?
                """, { rs, _ ->
                    com.api.postgres.CrewDto(
                        personId = rs.getInt("person_id"),
                        name = rs.getString("name"),
                        department = rs.getString("department"),
                        job = rs.getString("job"),
                        popularity = rs.getDouble("popularity"),
                        profilePath = rs.getString("profile_path"),
                        gender = rs.getInt("gender"),
                        knownForDepartment = rs.getString("known_for_department"),
                        episodeCount = rs.getObject("episode_count") as Int
                    )
                }, tmdbId)
            } catch (e: Exception) {
                logger.warn("Error getting crew info: ${e.message}")
                emptyList()
            }

            // Generate metadata
            val metadata = metadataService.generatePostMetadata(
                postDto = postDto,
                tmdbId = postDto.tmdbId,
                comment = "Auto-generated by VectorInitialization",
                cast = cast,
                crew = crew
            )

            // Store metadata
            metadataService.storePostMetadata(postId, "Vector initialization", metadata)

            logger.info("Updated metadata for post $postId")
        } catch (e: Exception) {
            logger.error("Error updating metadata for post $postId: ${e.message}")
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

        // Get genres - handle table not existing or different schema
        val genres = try {
            jdbcTemplate.query("""
                SELECT ug.user_id, ug.genre_id, g.genre_name, ug.priority
                FROM user_genres ug
                JOIN genres g ON ug.genre_id = g.genre_id
                WHERE ug.user_id = ?
            """, { rs, _ -> mapToGenreDto(rs) }, userId)
        } catch (e: DataAccessException) {
            // If there's an error, try a different query format
            try {
                logger.info("Trying alternative query for user genres")
                jdbcTemplate.query("""
                    SELECT ug.user_id, ug.genre_id, g.genre_name, ug.priority
                    FROM user_genres ug
                    JOIN genres g ON ug.genre_id = g.genre_id
                    WHERE ug.user_id = ?
                """, { rs, _ -> mapToGenreDto(rs) }, userId)
            } catch (e2: Exception) {
                logger.warn("Could not get genres for user $userId: ${e2.message}")
                emptyList()
            }
        } catch (e: Exception) {
            logger.warn("Could not get genres for user $userId: ${e.message}")
            emptyList()
        }

        // Get post interactions
        val postInteractions = try {
            jdbcTemplate.query("""
                SELECT * FROM user_post_interactions
                WHERE user_id = ?
                ORDER BY start_timestamp DESC
                LIMIT 100
            """, { rs, _ -> mapToPostInteractionDto(rs) }, userId)
        } catch (e: Exception) {
            logger.warn("Could not get post interactions for user $userId: ${e.message}")
            emptyList()
        }

        // Get trailer interactions
        val trailerInteractions = try {
            jdbcTemplate.query("""
                SELECT * FROM user_trailer_interactions
                WHERE user_id = ?
                ORDER BY start_timestamp DESC
                LIMIT 100
            """, { rs, _ -> mapToTrailerInteractionDto(rs) }, userId)
        } catch (e: Exception) {
            logger.warn("Could not get trailer interactions for user $userId: ${e.message}")
            emptyList()
        }

        return UserData(
            userDto = userDto,
            genres = genres ?: emptyList(),
            interactions = postInteractions ?: emptyList(),
            trailerInteractions = trailerInteractions ?: emptyList()
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