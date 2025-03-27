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
        postId: Int?,
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
        userId: Int?,
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
                        region = ?,
                        demographic_segment = ?,
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
        userId: Int?,
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
        postId: Int?,
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

    @Transactional
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
                    "providerName" to getProviderName(sub.providerId)
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
                COUNT(DISTINCT m.id) as total_clicks,
                MAX(t.end_timestamp) as last_clicked
            FROM more_information m
            JOIN info_timestamps t ON m.id = t.info_id
            WHERE m.user_id = ? AND m.type IN ('movie', 'tv')
        """, userDto.userId)

            // Get post-specific clicks with counts
            val postClicksMap = mutableMapOf<String, Int>()

            jdbcTemplate.query("""
            SELECT 
                m.tmdb_id, 
                COUNT(DISTINCT m.id) as click_count
            FROM more_information m
            WHERE m.user_id = ? AND m.type IN ('movie', 'tv')
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
            infoButtonClicks["lastClicked"] = (infoButtonData["last_clicked"]?.toString()) ?: ""
            infoButtonClicks["postClicks"] = postClicksMap

            // Add to metadata with both keys for compatibility
            metadata["infoButtonClicks"] = infoButtonClicks
            metadata["mediaClicks"] = infoButtonClicks
        } catch (e: Exception) {
            logger.warn("Error retrieving info button data for user ${userDto.userId}: ${e.message}")

            // Create default map with explicit types
            val defaultInfoButtonClicks = HashMap<String, Any>()
            defaultInfoButtonClicks["total"] = 0
            defaultInfoButtonClicks["lastClicked"] = ""
            defaultInfoButtonClicks["postClicks"] = emptyMap<String, Int>()

            // Add with both keys for compatibility
            metadata["infoButtonClicks"] = defaultInfoButtonClicks
            metadata["mediaClicks"] = defaultInfoButtonClicks
        }

        // Create comprehensive interest weights from multiple sources
        val interestWeights = try {
            val genreWeights = mutableMapOf<String, Double>()

            // STEP 1: Get explicit user genre preferences from user_genres table
            try {
                val explicitPreferences = jdbcTemplate.query("""
                SELECT g.genre_id, g.genre_name, ug.priority
                FROM user_genres ug
                JOIN genres g ON ug.genre_id = g.genre_id
                WHERE ug.user_id = ?
                ORDER BY ug.priority DESC
            """, { rs, _ ->
                    Triple(
                        rs.getInt("genre_id"),
                        rs.getString("genre_name"),
                        rs.getInt("priority")
                    )
                }, userDto.userId)

                if (explicitPreferences.isNotEmpty()) {
                    // Normalize priorities to weights between 0.5-1.0
                    // Higher priority (usually 1-10) = higher weight
                    val maxPriority = explicitPreferences.maxByOrNull { it.third }?.third ?: 10

                    explicitPreferences.forEach { (genreId, genreName, priority) ->
                        // Convert priority to weight (0.5-1.0 range)
                        val weight = 0.5 + (priority.toDouble() / maxPriority) * 0.5
                        genreWeights[genreName] = weight
                    }

                    logger.debug("Added ${explicitPreferences.size} explicit genre preferences")
                }
            } catch (e: Exception) {
                logger.warn("Error retrieving explicit genre preferences: ${e.message}")
            }

            // STEP 2: Get implicit preferences from interaction data
            try {
                // Get genres of content the user has interacted with
                val interactionPreferences = jdbcTemplate.query("""
                SELECT g.genre_name,
                       COUNT(*) as interaction_count,
                       SUM(CASE WHEN upi.like_state THEN 1 ELSE 0 END) as like_count,
                       SUM(CASE WHEN upi.save_state THEN 1 ELSE 0 END) as save_count
                FROM user_post_interactions upi
                JOIN posts p ON upi.post_id = p.post_id
                JOIN post_genres pg ON p.post_id = pg.post_id
                JOIN genres g ON pg.genre_id = g.genre_id
                WHERE upi.user_id = ?
                GROUP BY g.genre_name
                ORDER BY interaction_count DESC
            """, { rs, _ ->
                    val genreName = rs.getString("genre_name")
                    val interactionCount = rs.getInt("interaction_count")
                    val likeCount = rs.getInt("like_count")
                    val saveCount = rs.getInt("save_count")

                    // Calculate a weight based on interactions
                    // Likes and saves count more than just views
                    val weight = (interactionCount + (likeCount * 2) + (saveCount * 3)).toDouble()

                    Pair(genreName, weight)
                }, userDto.userId)

                if (interactionPreferences.isNotEmpty()) {
                    // Normalize the weights to 0.2-0.9 range
                    val maxWeight = interactionPreferences.maxByOrNull { it.second }?.second ?: 1.0

                    interactionPreferences.forEach { (genreName, rawWeight) ->
                        // Convert to normalized weight (0.2-0.9 range)
                        val normalizedWeight = 0.2 + (rawWeight / maxWeight) * 0.7

                        // Combine with explicit preference if it exists, otherwise just use this weight
                        if (genreName in genreWeights) {
                            // Weighted average: 60% explicit preference, 40% interaction data
                            genreWeights[genreName] = genreWeights[genreName]!! * 0.6 + normalizedWeight * 0.4
                        } else {
                            genreWeights[genreName] = normalizedWeight
                        }
                    }

                    logger.debug("Added ${interactionPreferences.size} implicit genre preferences")
                }
            } catch (e: Exception) {
                logger.warn("Error retrieving interaction-based preferences: ${e.message}")
            }

            // STEP 3: Get genre avoidance preferences (negative weights)
            try {
                val avoidedGenres = jdbcTemplate.query("""
                SELECT g.genre_name
                FROM user_avoid_genres uag
                JOIN genres g ON uag.genre_id = g.genre_id
                WHERE uag.user_id = ?
            """, { rs, _ ->
                    rs.getString("genre_name")
                }, userDto.userId)

                // Set avoided genres to very low weight (0.1)
                avoidedGenres.forEach { genreName ->
                    genreWeights[genreName] = 0.1
                }

                if (avoidedGenres.isNotEmpty()) {
                    logger.debug("Added ${avoidedGenres.size} avoided genres")
                }
            } catch (e: Exception) {
                logger.warn("Error retrieving genre avoidance preferences: ${e.message}")
            }

            // If we still have no weights, use some defaults based on popular genres
            if (genreWeights.isEmpty()) {
                try {
                    // Get the most popular genres overall
                    val popularGenres = jdbcTemplate.query("""
                    SELECT g.genre_name, COUNT(*) as post_count
                    FROM post_genres pg
                    JOIN genres g ON pg.genre_id = g.genre_id
                    GROUP BY g.genre_name
                    ORDER BY post_count DESC
                    LIMIT 5
                """, { rs, _ ->
                        Pair(rs.getString("genre_name"), rs.getInt("post_count"))
                    })

                    if (popularGenres.isNotEmpty()) {
                        // Assign moderate weights to popular genres
                        val maxCount = popularGenres.maxByOrNull { it.second }?.second?.toDouble() ?: 1.0
                        popularGenres.forEach { (genreName, count) ->
                            genreWeights[genreName] = 0.4 + (count.toDouble() / maxCount) * 0.3
                        }

                        logger.debug("Added ${popularGenres.size} default popular genres")
                    } else {
                        // Last resort - just use a dummy default
                        genreWeights["default"] = 1.0
                    }
                } catch (e: Exception) {
                    logger.warn("Error retrieving popular genres: ${e.message}")
                    genreWeights["default"] = 1.0
                }
            }

            // Return the final weights map
            genreWeights
        } catch (e: Exception) {
            logger.warn("Error computing interest weights: ${e.message}")
            mapOf("default" to 1.0)
        }

        // Add interest weights to metadata
        metadata["interestWeights"] = interestWeights

        // Update language preferences directly
        try {
            // First get languages from liked posts
            val likedLanguages = jdbcTemplate.queryForList("""
            SELECT DISTINCT p.original_language
            FROM user_post_interactions upi
            JOIN posts p ON upi.post_id = p.post_id
            WHERE upi.user_id = ? 
            AND upi.like_state = true
            AND p.original_language IS NOT NULL
        """, String::class.java, userDto.userId)

            // Then get languages from saved posts
            val savedLanguages = jdbcTemplate.queryForList("""
            SELECT DISTINCT p.original_language
            FROM user_post_interactions upi
            JOIN posts p ON upi.post_id = p.post_id
            WHERE upi.user_id = ? 
            AND upi.save_state = true
            AND p.original_language IS NOT NULL
        """, String::class.java, userDto.userId)

            // Combine both lists
            val allLanguages = likedLanguages + savedLanguages

            if (allLanguages.isNotEmpty()) {
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
            """, weightsJson, userDto.userId)

                // Check if row was updated
                val rowsAffected = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM user_vector_metadata WHERE user_id = ?",
                    Int::class.java,
                    userDto.userId
                ) ?: 0

                // If no rows updated, insert a new row
                if (rowsAffected == 0) {
                    jdbcTemplate.update("""
                    INSERT INTO user_vector_metadata (user_id, language_weights, created_at, updated_at)
                    VALUES (?, ?::jsonb, NOW(), NOW())
                """, userDto.userId, weightsJson)
                }

                // Also update the more_information field for backward compatibility
                updateMoreInfoField(userDto.userId, weights)

                logger.info("Updated language preferences for user ${userDto.userId} with ${languageWeights.size} languages")

                // Add language weights to metadata
                metadata["languageWeights"] = weights
            } else {
                logger.info("No language data found for user ${userDto.userId}")
            }
        } catch (e: Exception) {
            logger.warn("Error updating language preferences for user ${userDto.userId}: ${e.message}")
            // Continue execution - this is not critical
        }

        // Now, persist the vector weights to the database
        try {
            // Convert interest weights to JSON string
            val interestWeightsJson = objectMapper.writeValueAsString(interestWeights)

            // Call updateUserVectorWeights to persist the data
            updateUserVectorWeights(
                userId = userDto.userId,
                interestWeights = interestWeightsJson,
                region = userDto.region,
                demographicSegment = null  // Add if you have demographic data
            )

            // Also store the full metadata
            storeUserMetadata(userDto.userId, comment, metadata)

            logger.info("Updated user vector weights for user ${userDto.userId} with region: ${userDto.region} and ${interestWeights.size} genre preferences")
        } catch (e: Exception) {
            logger.warn("Error persisting user vector weights: ${e.message}")
            // Continue execution - the metadata will still be returned
        }

        return metadata
    }

    /**
     * Generate post metadata including categorical features and info
     * Also updates the post vector weights to ensure data persistence.
     */
    @Transactional
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
        var postType = "movie" // Default type
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

            // Extract the type for later use
            postType = postDetails["type"]?.toString() ?: "movie"
        } catch (e: Exception) {
            logger.warn("Error retrieving post details for post ${postDto.postId}: ${e.message}")
        }

        // Get info button interactions
        try {
            // Query the more_information table to get aggregate info button data for this post
            val infoButtonData = jdbcTemplate.queryForMap("""
            SELECT 
                COUNT(DISTINCT m.id) as total_clicks,
                COUNT(DISTINCT m.user_id) as unique_users,
                MAX(t.end_timestamp) as last_clicked
            FROM more_information m
            JOIN info_timestamps t ON m.id = t.info_id
            WHERE m.tmdb_id = ? AND m.type IN ('movie', 'tv')
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
            infoButtonClicks["lastClicked"] = (infoButtonData["last_clicked"]?.toString()) ?: ""
            infoButtonClicks["userIds"] = userIds

            // Add to metadata with both keys for backward compatibility
            metadata["infoButtonClicks"] = infoButtonClicks
            metadata["mediaClicks"] = infoButtonClicks
        } catch (e: Exception) {
            logger.warn("Error retrieving info button data for post ${postDto.postId}: ${e.message}")

            // Create default map with explicit structure
            val defaultInfoButtonClicks = HashMap<String, Any>()
            defaultInfoButtonClicks["count"] = 0
            defaultInfoButtonClicks["uniqueUserCount"] = 0
            defaultInfoButtonClicks["lastClicked"] = ""
            defaultInfoButtonClicks["userIds"] = emptyList<Int>()

            // Add with both keys for backward compatibility
            metadata["infoButtonClicks"] = defaultInfoButtonClicks
            metadata["mediaClicks"] = defaultInfoButtonClicks
        }

        // Add demographics data - who watches this content
        var regionWeightsMap = mutableMapOf<String, Double>()
        try {
            val viewerDemographics = jdbcTemplate.query("""
            SELECT 
                u.region,
                COUNT(DISTINCT m.id) as view_count
            FROM 
                more_information m
            JOIN 
                users u ON m.user_id = u.user_id
            WHERE 
                m.tmdb_id = ? AND m.type IN ('movie', 'tv')
            GROUP BY 
                u.region
            ORDER BY 
                view_count DESC
        """, { rs, rowNum ->
                Pair(rs.getString("region"), rs.getInt("view_count"))
            }, tmdbId)

            if (viewerDemographics.isNotEmpty()) {
                // Convert count-based demographics to normalized weights (0.0-1.0)
                val demographicsMap = mutableMapOf<String, Int>()
                val totalCount = viewerDemographics.sumBy { it.second }

                viewerDemographics.forEach { (region, count) ->
                    demographicsMap[region] = count

                    // Calculate normalized weight based on proportion of total views
                    val normalizedWeight = count.toDouble() / totalCount
                    regionWeightsMap[region] = normalizedWeight
                }

                metadata["viewerDemographics"] = demographicsMap
                metadata["regionWeights"] = regionWeightsMap
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

        // Create genre weights map with improved weighting system
        val genreWeights = try {
            // First, get the basic genre assignments for this post
            val genres = jdbcTemplate.query("""
            SELECT g.genre_id, g.genre_name
            FROM post_genres pg
            JOIN genres g ON pg.genre_id = g.genre_id
            WHERE pg.post_id = ?
        """, { rs, _ ->
                Pair(
                    rs.getInt("genre_id"),
                    rs.getString("genre_name")
                )
            }, postDto.postId)

            if (genres.isEmpty()) {
                // No genres found, use default
                mapOf("default" to 1.0)
            } else {
                // For each genre, try to get engagement metrics to adjust weights
                val weightMap = mutableMapOf<String, Double>()

                // Get total interactions for normalization
                val totalInteractions = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM user_post_interactions WHERE post_id = ?
            """, Int::class.java, postDto.postId) ?: 0

                genres.forEach { (genreId, genreName) ->
                    // Start with base weight of 1.0
                    var weight = 1.0

                    // Try to enhance with engagement data if available
                    try {
                        val engagementData = jdbcTemplate.queryForMap("""
                        SELECT 
                            COUNT(*) as total_interactions,
                            SUM(CASE WHEN upi.like_state THEN 1 ELSE 0 END) as likes
                        FROM user_post_interactions upi
                        JOIN user_genres ug ON upi.user_id = ug.user_id AND ug.genre_id = ?
                        WHERE upi.post_id = ?
                    """, genreId, postDto.postId)

                        val genreInteractions = (engagementData["total_interactions"] as Number).toInt()
                        val genreLikes = (engagementData["likes"] as Number).toInt()

                        // Only adjust weight if we have meaningful interaction data
                        if (genreInteractions > 0 && totalInteractions > 0) {
                            // Boost weight based on like ratio among users who prefer this genre
                            val likeRatio = if (genreInteractions > 0) {
                                genreLikes.toDouble() / genreInteractions
                            } else 0.0

                            // Apply boost (between 0.8 and 1.2 based on like ratio)
                            weight = 0.8 + (likeRatio * 0.4)
                        }
                    } catch (e: Exception) {
                        // If engagement calculation fails, keep default weight
                        logger.debug("Could not calculate engagement weight for genre $genreName: ${e.message}")
                    }

                    weightMap[genreName] = weight
                }

                weightMap
            }
        } catch (e: Exception) {
            logger.warn("Error computing genre weights for post ${postDto.postId}: ${e.message}")
            mapOf("default" to 1.0)
        }

        // Add genre weights to metadata
        metadata["genreWeights"] = genreWeights

        // Now, persist the vector weights to the database
        try {
            // Convert weights to JSON strings
            val genreWeightsJson = objectMapper.writeValueAsString(genreWeights)

            // Convert demographics to JSON if available
            val demographicWeightsJson = if (metadata.containsKey("viewerDemographics")) {
                objectMapper.writeValueAsString(metadata["viewerDemographics"])
            } else {
                null
            }

            // Convert region weights to JSON if available
            val regionWeightsJson = if (regionWeightsMap.isNotEmpty()) {
                objectMapper.writeValueAsString(regionWeightsMap)
            } else {
                null
            }

            // Call updatePostVectorWeights to persist the data
            updatePostVectorWeights(
                postId = postDto.postId,
                tmdbId = tmdbId,
                type = postType,
                genreWeights = genreWeightsJson,
                demographicWeights = demographicWeightsJson,
                regionWeights = regionWeightsJson
            )

            // Also store the full metadata
            storePostMetadata(postDto.postId, comment, metadata)

            logger.info("Updated post vector weights for post ${postDto.postId} with ${genreWeights.size} genres and ${regionWeightsMap.size} regions")
        } catch (e: Exception) {
            logger.warn("Error persisting post vector weights: ${e.message}")
            // Continue execution - the metadata will still be returned
        }

        return metadata
    }

    /**
     * Get metadata for user vector
     */
    @Transactional(readOnly = true)
    fun getUserMetadata(userId: Int?): MetadataResult? {
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
                    language_weights,
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
            // First, find the corresponding TMDB ID and type for this post if not provided
            var actualTmdbId = tmdbId
            var contentType = "movie"  // Default to movie

            if (tmdbId <= 0 || postId > 0) {
                val postData = jdbcTemplate.queryForMap(
                    "SELECT tmdb_id, type FROM posts WHERE post_id = ?",
                    postId
                )

                actualTmdbId = (postData["tmdb_id"] as Number).toInt()
                contentType = postData["type"] as String
            }

            // Record the info button click in the more_information table using the content type
            val now = LocalDateTime.now().toString()
            jdbcTemplate.update("""
                INSERT INTO more_information (
                    tmdb_id, type, user_id
                ) VALUES (?, ?, ?)
            """,
                actualTmdbId,
                contentType,  // Use movie or tv instead of 'info_button'
                userId)

            // Get the generated ID
            val infoId = jdbcTemplate.queryForObject(
                "SELECT currval(pg_get_serial_sequence('more_information', 'id'))",
                Long::class.java
            )

            // Add timestamp to info_timestamps table
            jdbcTemplate.update("""
                INSERT INTO info_timestamps (
                    info_id, session_index, start_timestamp, end_timestamp
                ) VALUES (?, ?, ?, ?)
            """,
                infoId,
                0,  // First session
                now,
                now)

            // Also update vector metadata
            updateInfoButtonMetadata(userId, postId, actualTmdbId)

            logger.info("Recorded info button click: User $userId on Post $postId (TMDB ID: $actualTmdbId, Type: $contentType)")
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
            // Get fresh info button data from the more_information table for the user
            val userInfoData = jdbcTemplate.queryForMap("""
            SELECT 
                COUNT(DISTINCT m.id) as total_clicks,
                MAX(t.end_timestamp) as last_clicked
            FROM more_information m
            JOIN info_timestamps t ON m.id = t.info_id
            WHERE m.user_id = ? AND m.type IN ('movie', 'tv')
        """, userId)

            val postClicksForUser = mutableListOf<Pair<Int, Int>>()

            jdbcTemplate.query("""
            SELECT 
                m.tmdb_id,
                COUNT(DISTINCT m.id) as click_count
            FROM more_information m
            WHERE m.user_id = ? AND m.type IN ('movie', 'tv')
            GROUP BY m.tmdb_id
            ORDER BY click_count DESC
        """, { rs ->
                val id = rs.getInt("tmdb_id")
                val count = rs.getInt("click_count")
                postClicksForUser.add(Pair(id, count))
            }, userId)

            // Update user metadata
            val userMetadata = getUserMetadata(userId)
            val userMoreInfo = userMetadata?.moreInformation?.toMutableMap() ?: mutableMapOf()

            // Use both keys for backward compatibility
            val clicksData = mapOf(
                "total" to (userInfoData["total_clicks"] as Number).toInt(),
                "lastClicked" to userInfoData["last_clicked"],
                "postClicks" to postClicksForUser.associate { it.first.toString() to it.second }
            )

            // Store with both keys to ensure compatibility
            userMoreInfo["infoButtonClicks"] = clicksData
            userMoreInfo["mediaClicks"] = clicksData

            storeUserMetadata(userId, userMetadata?.comment, userMoreInfo)

            // Get the post type from the posts table
            val contentType = try {
                jdbcTemplate.queryForObject(
                    "SELECT type FROM posts WHERE post_id = ?",
                    String::class.java,
                    postId
                ) ?: "movie" // Default to movie if null
            } catch (e: Exception) {
                logger.warn("Could not determine content type for post $postId: ${e.message}")
                "movie" // Default to movie on error
            }

            // Get fresh info button data for this post using the content type
            try {
                val postInfoData = jdbcTemplate.queryForMap("""
                SELECT 
                    COUNT(DISTINCT m.id) as total_clicks,
                    COUNT(DISTINCT m.user_id) as unique_users,
                    MAX(t.end_timestamp) as last_clicked
                FROM more_information m
                JOIN info_timestamps t ON m.id = t.info_id
                WHERE m.tmdb_id = ? AND m.type = ?
            """, tmdbId, contentType)

                val userIds = jdbcTemplate.queryForList("""
                SELECT DISTINCT user_id
                FROM more_information
                WHERE tmdb_id = ? AND type = ?
                LIMIT 100
            """, Int::class.java, tmdbId, contentType)

                // Update post metadata
                val postMetadata = getPostMetadata(postId)
                val postMoreInfo = postMetadata?.moreInformation?.toMutableMap() ?: mutableMapOf()

                // Create clicks data
                val postClicksData = mapOf(
                    "count" to (postInfoData["total_clicks"] as Number).toInt(),
                    "uniqueUserCount" to (postInfoData["unique_users"] as Number).toInt(),
                    "lastClicked" to postInfoData["last_clicked"],
                    "userIds" to userIds
                )

                // Store with both keys for compatibility
                postMoreInfo["infoButtonClicks"] = postClicksData
                postMoreInfo["mediaClicks"] = postClicksData

                storePostMetadata(postId, postMetadata?.comment, postMoreInfo)
            } catch (e: Exception) {
                logger.warn("Error retrieving media data for post $postId: ${e.message}")
                // This is isolated in its own try-catch to prevent it from affecting other operations
            }
        } catch (e: Exception) {
            logger.warn("Error updating media metadata: ${e.message}")
            // Continue execution - this is not critical enough to fail the whole transaction
        }
    }

    /**
     * Update more_information JSONB field with language preferences
     * This is copied from OriginLanguagePreferences to provide compatibility
     */
    private fun updateMoreInfoField(userId: Int?, weights: Map<String, Any>) {
        try {
            val metadataResult = getUserMetadata(userId)

            if (metadataResult != null) {
                val moreInfo = metadataResult.moreInformation?.toMutableMap() ?: mutableMapOf()

                // Add language preferences to more_information
                moreInfo["languagePreferences"] = weights

                // Store updated metadata
                storeUserMetadata(userId, metadataResult.comment, moreInfo)
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

    private fun getProviderName(providerId: Int): String {
        return try {
            jdbcTemplate.queryForObject(
                "SELECT provider_name FROM subscription_providers WHERE provider_id = ?",
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