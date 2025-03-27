package com.api.postgres.recommendations

import com.api.postgres.CastDto
import com.api.postgres.CrewDto
import com.api.postgres.PostDto
import com.api.postgres.UserDto
import com.api.postgres.UserSubscriptionDto
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.Executors

@Component
class VectorUpdateScheduler(
    private val vectorInitializationService: VectorInitialization,
    private val vectorTableService: VectorTableInitializer,
    private val behaviorProfiler: BehaviorProfiler,
    private val metadataService: MetadataService,
    private val languagePreferenceService: OriginLanguagePreferences,
    private val jdbcTemplate: JdbcTemplate
) {
    private val logger = LoggerFactory.getLogger(VectorUpdateScheduler::class.java)

    // Thread pool for parallel processing
    private val executor = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
    )

    /**
     * Initialize required schema and components on startup
     */
    @PostConstruct
    fun initialize() {
        try {
            logger.info("Initializing vector update scheduler")

            vectorTableService.initializeTables()

            // Ensure metadata tables exist
            metadataService.initializeMetadataTables()

            // Ensure language weights column exists
            languagePreferenceService.initializeSchema()

            logger.info("Vector update scheduler initialized successfully")
        } catch (e: Exception) {
            logger.error("Error initializing vector update scheduler: ${e.message}")
        }
    }

    /**
     * Daily update of vectors (runs at 2 AM)
     */
    @Scheduled(cron = "0 0 2 * * *")
    fun updateVectors() {
        try {
            logger.info("Starting scheduled vector update")

            // Update user vectors
            updateUserVectors()

            // Update post vectors
            updatePostVectors()

            logger.info("Scheduled vector update complete")
        } catch (e: Exception) {
            logger.error("Error during scheduled vector update: ${e.message}")
        }
    }

    /**
     * Update vectors for recently active users
     */
    private fun updateUserVectors() {
        // Get users with recent activity (in both post and trailer interactions)
        val recentlyActiveUserIds = jdbcTemplate.queryForList("""
            SELECT DISTINCT user_id FROM (
                SELECT user_id FROM user_post_interactions
                WHERE start_timestamp > NOW() - INTERVAL '1 day'
                UNION
                SELECT user_id FROM user_trailer_interactions
                WHERE start_timestamp > NOW() - INTERVAL '1 day'
            ) AS recent_activities
        """, Int::class.java)

        logger.info("Updating vectors for ${recentlyActiveUserIds.size} recently active users")

        // Submit tasks to thread pool for parallel processing
        recentlyActiveUserIds.forEach { userId ->
            executor.submit {
                try {
                    // Update user vector
                    vectorInitializationService.initializeUserVector(userId)

                    // Update user behavior profile
                    behaviorProfiler.generateUserProfile(userId)

                    // Update language preferences
                    languagePreferenceService.updateUserLanguagePreferences(userId)

                    // Generate and store updated metadata
                    updateUserMetadata(userId)

                    logger.debug("Updated vector, profile, and metadata for user $userId")
                } catch (e: Exception) {
                    logger.error("Error updating data for user $userId: ${e.message}")
                }
            }
        }
    }

    /**
     * Update vectors for recent posts
     */
    private fun updatePostVectors() {
        // Get recently added or updated posts
        val recentPostIds = jdbcTemplate.queryForList("""
            SELECT post_id FROM posts
            WHERE created_at > NOW() - INTERVAL '1 day'
            OR updated_at > NOW() - INTERVAL '1 day'
        """, Int::class.java)

        logger.info("Updating vectors for ${recentPostIds.size} recent posts")

        // Submit tasks to thread pool for parallel processing
        recentPostIds.forEach { postId ->
            executor.submit {
                try {
                    // Update post vector
                    vectorInitializationService.initializePostVector(postId)

                    // Update post metadata
                    updatePostMetadata(postId)

                    logger.debug("Updated vector and metadata for post $postId")
                } catch (e: Exception) {
                    logger.error("Error updating data for post $postId: ${e.message}")
                }
            }
        }
    }

    /**
     * Update metadata for a user
     */
    private fun updateUserMetadata(userId: Int) {
        try {
            // Get user data
            val userDto = getUserData(userId)
            if (userDto != null) {
                // Get user subscriptions
                val subscriptions = getUserSubscriptions(userId)

                // Generate metadata
                val metadata = metadataService.generateUserMetadata(
                    userDto = userDto,
                    subscriptions = subscriptions,
                    comment = "Updated by scheduler on ${java.time.LocalDateTime.now()}"
                )

                // Store metadata
                metadataService.storeUserMetadata(
                    userId = userId,
                    comment = "Auto-generated metadata",
                    moreInformation = metadata
                )
            }
        } catch (e: Exception) {
            logger.error("Error updating metadata for user $userId: ${e.message}")
        }
    }

    /**
     * Update metadata for a post
     */
    private fun updatePostMetadata(postId: Int) {
        try {
            // Get post data
            val postDto = getPostData(postId)
            if (postDto != null) {
                // Get TMDB ID
                val tmdbId = postDto.tmdbId ?: jdbcTemplate.queryForObject(
                    "SELECT tmdb_id FROM posts WHERE post_id = ?",
                    Int::class.java,
                    postId
                ) ?: 0

                // Get cast and crew
                val cast = getCastData(tmdbId)
                val crew = getCrewData(tmdbId)

                // Generate metadata
                val metadata = metadataService.generatePostMetadata(
                    postDto = postDto,
                    tmdbId = tmdbId,
                    comment = "Updated by scheduler on ${java.time.LocalDateTime.now()}",
                    cast = cast,
                    crew = crew
                )

                // Store metadata
                metadataService.storePostMetadata(
                    postId = postId,
                    comment = "Auto-generated metadata",
                    moreInformation = metadata
                )
            }
        } catch (e: Exception) {
            logger.error("Error updating metadata for post $postId: ${e.message}")
        }
    }

    /**
     * Helper to get user data
     */
    private fun getUserData(userId: Int): UserDto? {
        return try {
            jdbcTemplate.queryForObject("""
            SELECT 
                user_id as userId,
                language,
                region,
                min_movie as minMovie,
                max_movie as maxMovie,
                min_tv as minTV,
                max_tv as maxTV,
                oldest_date as oldestDate,
                recent_date as recentDate,
                email,
                name,
                username,
                created_at as createdAt,
                recent_login as recentLogin,
                pswd as password
            FROM users
            WHERE user_id = ?
        """, { rs, _ ->
                UserDto(
                    userId = rs.getInt("userId"),
                    language = rs.getString("language"),
                    region = rs.getString("region"),
                    minMovie = rs.getObject("minMovie") as Int?,
                    maxMovie = rs.getObject("maxMovie") as Int?,
                    minTV = rs.getObject("minTV") as Int?,
                    maxTV = rs.getObject("maxTV") as Int?,
                    oldestDate = rs.getString("oldestDate"),
                    recentDate = rs.getString("recentDate"),
                    email = rs.getString("email"),
                    name = rs.getString("name"),
                    username = rs.getString("username"),
                    password = rs.getString("password"),
                    createdAt = rs.getString("createdAt"),
                    recentLogin = rs.getString("recentLogin")
                )
            }, userId)
        } catch (e: Exception) {
            logger.error("Error retrieving user data for $userId: ${e.message}")
            null
        }
    }

    /**
     * Helper to get user subscriptions
     */
    private fun getUserSubscriptions(userId: Int): List<UserSubscriptionDto> {
        return try {
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
            logger.error("Error retrieving subscriptions for user $userId: ${e.message}")
            emptyList()
        }
    }

    /**
     * Helper to get post data
     */
    private fun getPostData(postId: Int): PostDto? {
        return try {
            jdbcTemplate.queryForObject("""
                SELECT 
                    post_id as postId,
                    tmdb_id as tmdbId,
                    type,
                    original_language as originalLanguage,
                    subscription,
                    title,
                    original_title as originalTitle,
                    overview,
                    popularity,
                    vote_average as voteAverage,
                    vote_count as voteCount,
                    release_date as releaseDate,
                    poster_path as posterPath,
                    video_key as videoKey,
                    genre_ids as genreIds,
                    post_like_count as postLikeCount,
                    trailer_like_count as trailerLikeCount
                FROM posts
                WHERE post_id = ?
            """, { rs, _ ->
                PostDto(
                    postId = rs.getInt("postId"),
                    tmdbId = rs.getInt("tmdbId"),
                    type = rs.getString("type"),
                    originalLanguage = rs.getString("originalLanguage"),
                    subscription = rs.getString("subscription"),
                    title = rs.getString("title"),
                    originalTitle = rs.getString("originalTitle"),
                    overview = rs.getString("overview"),
                    popularity = rs.getDouble("popularity"),
                    voteAverage = rs.getDouble("voteAverage"),
                    voteCount = rs.getInt("voteCount"),
                    releaseDate = rs.getString("releaseDate"),
                    posterPath = rs.getString("posterPath"),
                    videoKey = rs.getString("videoKey"),
                    genreIds = rs.getString("genreIds"),
                    postLikeCount = rs.getInt("postLikeCount"),
                    trailerLikeCount = rs.getInt("trailerLikeCount")
                )
            }, postId)
        } catch (e: Exception) {
            logger.error("Error retrieving post data for $postId: ${e.message}")
            null
        }
    }

    /**
     * Helper to get cast data
     */
    private fun getCastData(tmdbId: Int): List<CastDto> {
        return try {
            jdbcTemplate.query("""
                SELECT 
                    id,
                    person_id as personId,
                    name,
                    character,
                    order_index as orderIndex,
                    popularity,
                    profile_path as profilePath,
                    gender,
                    known_for_department as knownForDepartment,
                    episode_count as episodeCount
                FROM cast_members
                WHERE tmdb_id = ?
                ORDER BY order_index
            """, { rs, _ ->
                CastDto(
                    personId = rs.getInt("personId"),
                    name = rs.getString("name"),
                    character = rs.getString("character"),
                    orderIndex = rs.getInt("orderIndex"),
                    popularity = rs.getDouble("popularity"),
                    profilePath = rs.getString("profilePath"),
                    gender = rs.getInt("gender"),
                    knownForDepartment = rs.getString("knownForDepartment"),
                    episodeCount = rs.getObject("episodeCount") as Int
                )
            }, tmdbId)
        } catch (e: Exception) {
            logger.error("Error retrieving cast data for TMDB ID $tmdbId: ${e.message}")
            emptyList()
        }
    }

    /**
     * Helper to get crew data
     */
    private fun getCrewData(tmdbId: Int): List<CrewDto> {
        return try {
            jdbcTemplate.query("""
                SELECT 
                    id,
                    person_id as personId,
                    name,
                    department,
                    job,
                    popularity,
                    profile_path as profilePath,
                    gender,
                    known_for_department as knownForDepartment,
                    episode_count as episodeCount
                FROM crew
                WHERE tmdb_id = ?
            """, { rs, _ ->
                CrewDto(
                    personId = rs.getInt("personId"),
                    name = rs.getString("name"),
                    department = rs.getString("department"),
                    job = rs.getString("job"),
                    popularity = rs.getDouble("popularity"),
                    profilePath = rs.getString("profilePath"),
                    gender = rs.getInt("gender"),
                    knownForDepartment = rs.getString("knownForDepartment"),
                    episodeCount = rs.getObject("episodeCount") as Int
                )
            }, tmdbId)
        } catch (e: Exception) {
            logger.error("Error retrieving crew data for TMDB ID $tmdbId: ${e.message}")
            emptyList()
        }
    }

    /**
     * Weekly batch update of metadata and preferences (runs at 3 AM on Sunday)
     */
    @Scheduled(cron = "0 0 3 * * 0")
    fun weeklyMetadataUpdate() {
        try {
            logger.info("Starting weekly metadata and preferences update")

            // Update language preferences for all users
            languagePreferenceService.updateAllUsersLanguagePreferences()

            // Cleanup old behavior profiles
            cleanupOldProfiles()

            logger.info("Weekly metadata and preferences update complete")
        } catch (e: Exception) {
            logger.error("Error during weekly metadata update: ${e.message}")
        }
    }

    /**
     * Cleanup of old behavior profiles
     */
    private fun cleanupOldProfiles() {
        try {
            logger.info("Starting cleanup of old user behavior profiles")

            // Remove profiles that haven't been updated in over 90 days
            val count = jdbcTemplate.update("""
                DELETE FROM user_behavior_profiles
                WHERE updated_at < NOW() - INTERVAL '90 days'
            """)

            logger.info("Removed $count old user behavior profiles")
        } catch (e: Exception) {
            logger.error("Error during behavior profile cleanup: ${e.message}")
        }
    }
}