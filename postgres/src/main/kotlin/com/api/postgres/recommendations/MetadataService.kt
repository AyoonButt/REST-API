package com.api.postgres.recommendations

import com.api.postgres.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.core.type.TypeReference
import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Service for managing metadata associated with user and post vectors.
 * Handles both the vector metadata tables and additional metadata operations.
 */
@Service
class MetadataService(
    private val jdbcTemplate: JdbcTemplate
) {
    private val logger = LoggerFactory.getLogger(MetadataService::class.java)
    private val objectMapper = ObjectMapper()

    /**
     * Initialize vector metadata tables
     */
    fun initializeMetadataTables() {
        try {
            logger.info("Creating vector metadata tables if needed")

            // Create post vector metadata table
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS post_vector_metadata (
                    post_id INT PRIMARY KEY,
                    tmdb_id INT NOT NULL,
                    type VARCHAR(50) NOT NULL,
                    genre_weights JSONB,
                    demographic_weights JSONB,
                    region_weights JSONB,
                    comment TEXT,
                    more_information JSONB,
                    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
            """)

            // Create user vector metadata table
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_vector_metadata (
                    user_id INT PRIMARY KEY,
                    interest_weights JSONB,
                    region VARCHAR(100),
                    demographic_segment VARCHAR(100),
                    comment TEXT,
                    more_information JSONB,
                    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
            """)

            // Create indices for efficient querying
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS post_metadata_tmdb_idx ON post_vector_metadata(tmdb_id, type)")
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS user_metadata_region_idx ON user_vector_metadata(region)")
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS post_metadata_updated_idx ON post_vector_metadata(updated_at)")
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS user_metadata_updated_idx ON user_vector_metadata(updated_at)")

            logger.info("Vector metadata tables created successfully")
        } catch (e: Exception) {
            logger.error("Error creating vector metadata tables: ${e.message}")
            throw e
        }
    }

    /**
     * Update post vector metadata weights
     */
    fun updatePostVectorWeights(
        postId: Int,
        tmdbId: Int,
        type: String,
        genreWeights: String,
        demographicWeights: String? = null,
        regionWeights: String? = null
    ) {
        try {
            jdbcTemplate.update("""
                INSERT INTO post_vector_metadata 
                    (post_id, tmdb_id, type, genre_weights, demographic_weights, region_weights, updated_at)
                VALUES (?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, NOW())
                ON CONFLICT (post_id)
                DO UPDATE SET 
                    tmdb_id = ?,
                    type = ?,
                    genre_weights = ?::jsonb,
                    demographic_weights = ?::jsonb,
                    region_weights = ?::jsonb,
                    updated_at = NOW()
            """,
                postId, tmdbId, type, genreWeights, demographicWeights, regionWeights,
                tmdbId, type, genreWeights, demographicWeights, regionWeights
            )
            logger.info("Updated vector weights for post ID: $postId")
        } catch (e: Exception) {
            logger.error("Error updating vector weights for post ID $postId: ${e.message}")
            throw e
        }
    }

    /**
     * Update user vector weights
     */
    fun updateUserVectorWeights(
        userId: Int,
        interestWeights: String,
        region: String? = null,
        demographicSegment: String? = null
    ) {
        try {
            jdbcTemplate.update("""
                INSERT INTO user_vector_metadata 
                    (user_id, interest_weights, region, demographic_segment, updated_at)
                VALUES (?, ?::jsonb, ?, ?, NOW())
                ON CONFLICT (user_id)
                DO UPDATE SET 
                    interest_weights = ?::jsonb,
                    region = COALESCE(?, region),
                    demographic_segment = COALESCE(?, demographic_segment),
                    updated_at = NOW()
            """,
                userId, interestWeights, region, demographicSegment,
                interestWeights, region, demographicSegment
            )
            logger.info("Updated vector weights for user ID: $userId")
        } catch (e: Exception) {
            logger.error("Error updating vector weights for user ID $userId: ${e.message}")
            throw e
        }
    }

    /**
     * Store metadata for a user vector
     */
    @Transactional
    fun storeUserMetadata(
        userId: Int,
        comment: String?,
        moreInformation: Map<String, Any>?
    ) {
        try {
            // Convert moreInformation map to JSON string if provided
            val moreInfoJson = if (moreInformation != null && moreInformation.isNotEmpty()) {
                objectMapper.writeValueAsString(moreInformation)
            } else {
                null
            }

            // Check if metadata entry already exists
            val exists = (jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_vector_metadata WHERE user_id = ?",
                Int::class.java,
                userId
            ) ?: 0) > 0

            if (exists) {
                // Update existing entry
                jdbcTemplate.update("""
                    UPDATE user_vector_metadata 
                    SET comment = ?, more_information = ?::jsonb, updated_at = NOW()
                    WHERE user_id = ?
                """,
                    comment,
                    moreInfoJson,
                    userId)
            } else {
                // Insert new entry
                jdbcTemplate.update("""
                    INSERT INTO user_vector_metadata (user_id, comment, more_information, created_at, updated_at)
                    VALUES (?, ?, ?::jsonb, NOW(), NOW())
                """,
                    userId,
                    comment,
                    moreInfoJson)
            }

            logger.info("Metadata stored for user vector $userId")
        } catch (e: Exception) {
            logger.error("Error storing user vector metadata: ${e.message}")
            throw e
        }
    }

    /**
     * Store metadata for a post vector
     */
    @Transactional
    fun storePostMetadata(
        postId: Int,
        comment: String?,
        moreInformation: Map<String, Any>?
    ) {
        try {
            // Convert moreInformation map to JSON string if provided
            val moreInfoJson = if (moreInformation != null && moreInformation.isNotEmpty()) {
                objectMapper.writeValueAsString(moreInformation)
            } else {
                null
            }

            // Check if metadata entry already exists
            val exists = (jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM post_vector_metadata WHERE post_id = ?",
                Int::class.java,
                postId
            ) ?: 0) > 0

            if (exists) {
                // Update existing entry
                jdbcTemplate.update("""
                    UPDATE post_vector_metadata 
                    SET comment = ?, more_information = ?::jsonb, updated_at = NOW()
                    WHERE post_id = ?
                """,
                    comment,
                    moreInfoJson,
                    postId)
            } else {
                // Insert new entry with minimum required fields
                val tmdbId = jdbcTemplate.queryForObject(
                    "SELECT tmdb_id FROM posts WHERE post_id = ?",
                    Int::class.java,
                    postId
                ) ?: throw IllegalArgumentException("Post not found with ID $postId")

                val type = jdbcTemplate.queryForObject(
                    "SELECT type FROM posts WHERE post_id = ?",
                    String::class.java,
                    postId
                ) ?: "unknown"

                jdbcTemplate.update("""
                    INSERT INTO post_vector_metadata 
                    (post_id, tmdb_id, type, comment, more_information, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?::jsonb, NOW(), NOW())
                """,
                    postId,
                    tmdbId,
                    type,
                    comment,
                    moreInfoJson)
            }

            logger.info("Metadata stored for post vector $postId")
        } catch (e: Exception) {
            logger.error("Error storing post vector metadata: ${e.message}")
            throw e
        }
    }

    /**
     * Generate user metadata including categorical features and info from the more_information table
     */
    @Transactional(readOnly = true)
    fun generateUserMetadata(
        userDto: UserDto,
        subscriptions: List<UserSubscriptionDto>? = null,
        comment: String? = null
    ): Map<String, Any> {
        val metadata = mutableMapOf<String, Any>()

        // Add basic information
        metadata["vectorCreatedAt"] = LocalDateTime.now().toString()
        if (comment != null) {
            metadata["comment"] = comment
        }

        // Add categorical data
        val categoricalData = HashMap<String, Any>()

        // Add language preference
        categoricalData["language"] = userDto.language

        // Add region preference
        categoricalData["region"] = userDto.region

        // Add subscription providers information if available
        if (subscriptions != null && subscriptions.isNotEmpty()) {
            val subscriptionProviders = subscriptions.map { sub ->
                mapOf(
                    "providerId" to sub.providerId,
                    "priority" to sub.priority,
                    "name" to getProviderName(sub.providerId)
                )
            }
            categoricalData["subscriptionProviders"] = subscriptionProviders
        }

        metadata["categoricalFeatures"] = categoricalData

        // Add runtime and date preferences
        val preferencesData = HashMap<String, Any>()
        preferencesData["movieRuntime"] = mapOf(
            "min" to (userDto.minMovie ?: 0),
            "max" to (userDto.maxMovie ?: 180)
        )
        preferencesData["tvRuntime"] = mapOf(
            "min" to (userDto.minTV ?: 0),
            "max" to (userDto.maxTV ?: 60)
        )
        preferencesData["dateRange"] = mapOf(
            "oldest" to userDto.oldestDate,
            "recent" to userDto.recentDate
        )
        metadata["preferences"] = preferencesData

        // Get user's info button interactions
        try {
            // Query the more_information table to get aggregate info button data
            val infoButtonData = jdbcTemplate.queryForMap("""
                SELECT 
                    COUNT(*) as total_clicks,
                    MAX(end_timestamp) as last_clicked
                FROM more_information
                WHERE user_id = ? AND type = 'info_button'
            """, userDto.userId)

            // Get post-specific clicks with counts
            val postClicksMap = mutableMapOf<String, Int>()

            jdbcTemplate.query("""
                SELECT 
                    m.tmdb_id, 
                    COUNT(*) as click_count
                FROM more_information m
                WHERE m.user_id = ? AND m.type = 'info_button'
                GROUP BY m.tmdb_id
                ORDER BY click_count DESC
            """, { rs ->
                val tmdbId = rs.getInt("tmdb_id")
                val clickCount = rs.getInt("click_count")
                postClicksMap[tmdbId.toString()] = clickCount
            }, userDto.userId)

            // Create the map with explicit types to avoid type inference issues
            val infoButtonClicks = HashMap<String, Any>()
            infoButtonClicks["total"] = (infoButtonData["total_clicks"] as? Number)?.toInt() ?: 0
            infoButtonClicks["lastClicked"] = infoButtonData["last_clicked"] as Any
            infoButtonClicks["postClicks"] = postClicksMap

            // Add to metadata
            metadata["infoButtonClicks"] = infoButtonClicks
        } catch (e: Exception) {
            logger.warn("Error retrieving info button data for user ${userDto.userId}: ${e.message}")

            // Create default map with explicit types
            val defaultInfoButtonClicks = HashMap<String, Any>()
            defaultInfoButtonClicks["total"] = 0
            defaultInfoButtonClicks["lastClicked"] = Any()
            defaultInfoButtonClicks["postClicks"] = emptyMap<String, Int>()

            metadata["infoButtonClicks"] = defaultInfoButtonClicks
        }

        return metadata
    }

    /**
     * Generate post metadata including categorical features and info
     */
    @Transactional(readOnly = true)
    fun generatePostMetadata(
        postDto: PostDto,
        tmdbId: Int,
        comment: String? = null,
        cast: List<CastDto>? = null,
        crew: List<CrewDto>? = null
    ): Map<String, Any> {
        val metadata = mutableMapOf<String, Any>()

        // Add basic information
        metadata["vectorCreatedAt"] = LocalDateTime.now().toString()
        if (comment != null) {
            metadata["comment"] = comment
        }

        // Add categorical features
        val categoricalData = HashMap<String, Any>()

        // Add language information
        categoricalData["language"] = postDto.originalLanguage ?: "en"

        // Add subscription provider
        categoricalData["subscriptionProvider"] = postDto.subscription

        metadata["categoricalFeatures"] = categoricalData

        // Get post details from posts table
        try {
            val postDetails = jdbcTemplate.queryForMap("""
                SELECT 
                    title, 
                    type,
                    release_date as releaseDate,
                    vote_average as voteAverage
                FROM posts
                WHERE post_id = ?
            """, postDto.postId)

            // Safely add each entry to avoid type issues
            postDetails.forEach { (key, value) ->
                metadata[key] = value
            }
        } catch (e: Exception) {
            logger.warn("Error retrieving post details for post ${postDto.postId}: ${e.message}")
        }

        // Get info button interactions
        try {
            // Query the more_information table to get aggregate info button data for this post
            val infoButtonData = jdbcTemplate.queryForMap("""
                SELECT 
                    COUNT(*) as total_clicks,
                    COUNT(DISTINCT user_id) as unique_users,
                    MAX(end_timestamp) as last_clicked
                FROM more_information
                WHERE tmdb_id = ? AND type = 'info_button'
            """, tmdbId)

            // Get user IDs who clicked
            val userIds = jdbcTemplate.queryForList("""
                SELECT DISTINCT user_id
                FROM more_information
                WHERE tmdb_id = ? AND type IN ('movie', 'tv')
                LIMIT 100
            """, Int::class.java, tmdbId)

            // Create map manually to avoid Pair/type issues
            val infoButtonClicks = HashMap<String, Any>()
            infoButtonClicks["count"] = (infoButtonData["total_clicks"] as? Number)?.toInt() ?: 0
            infoButtonClicks["uniqueUserCount"] = (infoButtonData["unique_users"] as? Number)?.toInt() ?: 0
            infoButtonClicks["lastClicked"] = (infoButtonData["last_clicked"] ?: null) as Any
            infoButtonClicks["userIds"] = userIds

            // Add to metadata
            metadata["infoButtonClicks"] = infoButtonClicks
        } catch (e: Exception) {
            logger.warn("Error retrieving info button data for post ${postDto.postId}: ${e.message}")

            // Create default map with explicit structure
            val defaultInfoButtonClicks = HashMap<String, Any>()
            defaultInfoButtonClicks["count"] = 0
            defaultInfoButtonClicks["uniqueUserCount"] = 0
            defaultInfoButtonClicks["lastClicked"] = Any()
            defaultInfoButtonClicks["userIds"] = emptyList<Int>()

            metadata["infoButtonClicks"] = defaultInfoButtonClicks
        }

        // Add demographics data - who watches this content
        try {
            val viewerDemographics = jdbcTemplate.query("""
                SELECT 
                    u.region,
                    COUNT(*) as view_count
                FROM 
                    more_information mi
                JOIN 
                    users u ON mi.user_id = u.user_id
                WHERE 
                    mi.tmdb_id = ? AND mi.type IN ('movie', 'tv')
                GROUP BY 
                    u.region
                ORDER BY 
                    view_count DESC
            """, { rs, rowNum ->
                Pair(rs.getString("region"), rs.getInt("view_count"))
            }, tmdbId)

            if (viewerDemographics.isNotEmpty()) {
                val demographicsMap = mutableMapOf<String, Int>()
                viewerDemographics.forEach { (region, count) ->
                    demographicsMap[region] = count
                }

                metadata["viewerDemographics"] = demographicsMap
            }
        } catch (e: Exception) {
            logger.warn("Error retrieving demographic data for post ${postDto.postId}: ${e.message}")
        }

        // Process cast information
        if (cast != null && cast.isNotEmpty()) {
            // Store top cast members (max 10)
            val topCast = cast
                .sortedBy { it.orderIndex }
                .take(10)
                .map { castMember ->
                    // Create map manually for each cast member
                    val castMap = HashMap<String, Any>()
                    castMap["id"] = castMember.personId
                    castMap["name"] = castMember.name
                    castMap["character"] = castMember.character
                    castMap["popularity"] = castMember.popularity
                    castMap["profilePath"] = castMember.profilePath
                    castMap
                }

            metadata["cast"] = topCast
        }

        // Process crew information
        if (crew != null && crew.isNotEmpty()) {
            // Extract key crew roles (director, writer, producer)
            val keyCrewByRole = HashMap<String, List<Map<String, Any>>>()

            // Build role groups
            val relevantRoles = listOf("director", "writer", "producer", "creator", "executive producer", "animator")
            for (role in relevantRoles) {
                val crewInRole = crew
                    .filter { it.job.toLowerCase() == role }
                    .map { crewMember ->
                        // Create map manually for each crew member
                        val crewMap = HashMap<String, Any>()
                        crewMap["id"] = crewMember.personId
                        crewMap["name"] = crewMember.name
                        crewMap["department"] = crewMember.department
                        crewMap["profilePath"] = crewMember.profilePath
                        crewMap
                    }

                if (crewInRole.isNotEmpty()) {
                    keyCrewByRole[role] = crewInRole
                }
            }

            if (keyCrewByRole.isNotEmpty()) {
                metadata["crew"] = keyCrewByRole
            }
        }

        // Add country information
        try {
            val countries = jdbcTemplate.queryForList("""
                SELECT origin_country 
                FROM post_countries 
                WHERE post_id = ?
            """, String::class.java, postDto.postId)

            if (countries.isNotEmpty()) {
                metadata["originCountries"] = countries
            }
        } catch (e: Exception) {
            logger.warn("Error retrieving country data for post ${postDto.postId}: ${e.message}")
        }

        return metadata
    }

    /**
     * Get metadata for user vector
     */
    @Transactional(readOnly = true)
    fun getUserMetadata(userId: Int): MetadataResult? {
        try {
            val rowMapper = RowMapper<MetadataResult> { rs, _ ->
                val comment = rs.getString("comment")
                val moreInfoJson = rs.getString("more_info_json")

                val moreInfo = if (moreInfoJson != null) {
                    try {
                        objectMapper.readValue(
                            moreInfoJson,
                            object : TypeReference<Map<String, Any>>() {}
                        )
                    } catch (e: Exception) {
                        mapOf<String, Any>()
                    }
                } else {
                    null
                }

                MetadataResult(comment, moreInfo)
            }

            return jdbcTemplate.queryForObject("""
                SELECT comment, more_information::text AS more_info_json
                FROM user_vector_metadata
                WHERE user_id = ?
            """, rowMapper, userId)
        } catch (e: EmptyResultDataAccessException) {
            return null
        } catch (e: Exception) {
            logger.error("Error retrieving user vector metadata: ${e.message}")
            return null
        }
    }

    /**
     * Get metadata for post vector
     */
    @Transactional(readOnly = true)
    fun getPostMetadata(postId: Int): MetadataResult? {
        try {
            val rowMapper = RowMapper<MetadataResult> { rs, _ ->
                val comment = rs.getString("comment")
                val moreInfoJson = rs.getString("more_info_json")

                val moreInfo = if (moreInfoJson != null) {
                    try {
                        objectMapper.readValue(
                            moreInfoJson,
                            object : TypeReference<Map<String, Any>>() {}
                        )
                    } catch (e: Exception) {
                        mapOf<String, Any>()
                    }
                } else {
                    null
                }

                MetadataResult(comment, moreInfo)
            }

            return jdbcTemplate.queryForObject("""
                SELECT comment, more_information::text AS more_info_json
                FROM post_vector_metadata
                WHERE post_id = ?
            """, rowMapper, postId)
        } catch (e: EmptyResultDataAccessException) {
            return null
        } catch (e: Exception) {
            logger.error("Error retrieving post vector metadata: ${e.message}")
            return null
        }
    }

    /**
     * Get post vector metadata with vector weights
     */
    @Transactional
    fun getPostVectorMetadata(postId: Int): PostVectorMetadataDto? {
        try {
            return jdbcTemplate.queryForObject("""
                SELECT 
                    post_id,
                    tmdb_id,
                    type,
                    genre_weights,
                    demographic_weights,
                    region_weights,
                    updated_at
                FROM 
                    post_vector_metadata
                WHERE 
                    post_id = ?
            """, { rs, _ ->
                PostVectorMetadataDto(
                    postId = rs.getInt("post_id"),
                    tmdbId = rs.getInt("tmdb_id"),
                    type = rs.getString("type"),
                    genreWeights = rs.getString("genre_weights"),
                    demographicWeights = rs.getString("demographic_weights"),
                    regionWeights = rs.getString("region_weights"),
                    updatedAt = rs.getTimestamp("updated_at").toLocalDateTime()
                )
            }, postId)
        } catch (e: Exception) {
            logger.error("Error retrieving vector metadata for post ID $postId: ${e.message}")
            return null
        }
    }

    /**
     * Get user vector metadata with weights
     */
    @Transactional
    fun getUserVectorMetadata(userId: Int): UserVectorMetadataDto? {
        try {
            return jdbcTemplate.queryForObject("""
                SELECT 
                    user_id,
                    interest_weights,
                    region,
                    demographic_segment,
                    language_weights
                    updated_at
                FROM 
                    user_vector_metadata
                WHERE 
                    user_id = ?
            """, { rs, _ ->
                UserVectorMetadataDto(
                    userId = rs.getInt("user_id"),
                    interestWeights = rs.getString("interest_weights"),
                    region = rs.getString("region"),
                    demographicSegment = rs.getString("demographic_segment"),
                    languageWeights = rs.getString("language_weights"),
                    updatedAt = rs.getTimestamp("updated_at").toLocalDateTime()
                )
            }, userId)
        } catch (e: Exception) {
            logger.error("Error retrieving vector metadata for user ID $userId: ${e.message}")
            return null
        }
    }

    /**
     * Using the more_information table, record that a user clicked the info button for a post
     */
    @Transactional
    fun recordInfoButtonClick(userId: Int, postId: Int, tmdbId: Int) {
        try {
            // First, find the corresponding TMDB ID for this post if not provided
            val actualTmdbId = if (tmdbId <= 0) {
                jdbcTemplate.queryForObject(
                    "SELECT tmdb_id FROM posts WHERE post_id = ?",
                    Int::class.java,
                    postId
                ) ?: throw IllegalArgumentException("No TMDB ID found for post $postId")
            } else {
                tmdbId
            }

            // Record the info button click in the more_information table
            val now = LocalDateTime.now().toString()
            jdbcTemplate.update("""
                INSERT INTO more_information (
                    tmdb_id, type, start_timestamp, end_timestamp, user_id
                ) VALUES (?, ?, ?, ?, ?)
            """,
                actualTmdbId,
                "info_button",
                now,
                now,
                userId)

            // Also update vector metadata to maintain backward compatibility
            updateInfoButtonMetadata(userId, postId, actualTmdbId)

            logger.info("Recorded info button click: User $userId on Post $postId (TMDB ID: $actualTmdbId)")
        } catch (e: Exception) {
            logger.error("Error recording info button click: ${e.message}")
            throw e
        }
    }

    /**
     * Update metadata with info button click information
     */
    private fun updateInfoButtonMetadata(userId: Int, postId: Int, tmdbId: Int) {
        try {
            // Get fresh info button data from the more_information table
            val userInfoData = jdbcTemplate.queryForMap("""
                SELECT 
                    COUNT(*) as total_clicks,
                    MAX(end_timestamp) as last_clicked
                FROM more_information
                WHERE user_id = ? AND type = 'info_button'
            """, userId)

            val postClicksForUser = mutableListOf<Pair<Int, Int>>()

            jdbcTemplate.query("""
                SELECT 
                    m.tmdb_id, 
                    COUNT(*) as click_count
                FROM more_information m
                WHERE m.user_id = ? AND m.type = 'info_button'
                GROUP BY m.tmdb_id
            """, { rs ->
                val id = rs.getInt("tmdb_id")
                val count = rs.getInt("click_count")
                postClicksForUser.add(Pair(id, count))
            }, userId)

            // Update user metadata
            val userMetadata = getUserMetadata(userId)
            val userMoreInfo = userMetadata?.moreInformation?.toMutableMap() ?: mutableMapOf()

            userMoreInfo["infoButtonClicks"] = mapOf(
                "total" to (userInfoData["total_clicks"] as Number).toInt(),
                "lastClicked" to userInfoData["last_clicked"],
                "postClicks" to postClicksForUser.associate { it.first.toString() to it.second }
            )

            storeUserMetadata(userId, userMetadata?.comment, userMoreInfo)

            // Get fresh info button data for this post
            val postInfoData = jdbcTemplate.queryForMap("""
                SELECT 
                    COUNT(*) as total_clicks,
                    COUNT(DISTINCT user_id) as unique_users,
                    MAX(end_timestamp) as last_clicked
                FROM more_information
                WHERE tmdb_id = ? AND type = 'info_button'
            """, tmdbId)

            val userIds = jdbcTemplate.queryForList("""
                SELECT DISTINCT user_id
                FROM more_information
                WHERE tmdb_id = ? AND type = 'info_button'
                LIMIT 100
            """, Int::class.java, tmdbId)

            // Update post metadata
            val postMetadata = getPostMetadata(postId)
            val postMoreInfo = postMetadata?.moreInformation?.toMutableMap() ?: mutableMapOf()

            postMoreInfo["infoButtonClicks"] = mapOf(
                "count" to (postInfoData["total_clicks"] as Number).toInt(),
                "uniqueUserCount" to (postInfoData["unique_users"] as Number).toInt(),
                "lastClicked" to postInfoData["last_clicked"],
                "userIds" to userIds
            )

            storePostMetadata(postId, postMetadata?.comment, postMoreInfo)

        } catch (e: Exception) {
            logger.warn("Error updating info button metadata: ${e.message}")
            // Continue execution - this is not critical enough to fail the whole transaction
        }
    }

    /**
     * Helper method to get provider name from ID
     */
    private fun getProviderName(providerId: Int): String {
        return try {
            jdbcTemplate.queryForObject(
                "SELECT name FROM subscription_providers WHERE provider_id = ?",
                String::class.java,
                providerId
            )
        } catch (e: Exception) {
            logger.warn("Error retrieving provider name for ID $providerId: ${e.message}")
            "Unknown"
        }
    }

    // Data classes for metadata results

    data class MetadataResult(
        val comment: String?,
        val moreInformation: Map<String, Any>?
    )

    /**
     * DTO for post vector metadata
     */
    data class PostVectorMetadataDto(
        val postId: Int,
        val tmdbId: Int,
        val type: String,
        val genreWeights: String?,
        val demographicWeights: String?,
        val regionWeights: String?,
        val updatedAt: java.time.LocalDateTime
    )

    /**
     * DTO for user vector metadata
     */
    data class UserVectorMetadataDto(
        val userId: Int,
        val interestWeights: String?,
        val languageWeights: String?,
        val region: String?,
        val demographicSegment: String?,
        val updatedAt: java.time.LocalDateTime
    )
}