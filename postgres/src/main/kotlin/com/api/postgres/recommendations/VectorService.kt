package com.api.postgres.recommendations

import com.api.postgres.PostDto
import com.api.postgres.UserDto
import com.api.postgres.UserGenreDto
import com.api.postgres.UserPostInteractionDto
import com.api.postgres.TrailerInteractionDto
import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.SQLException
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@Service
class VectorService(private val jdbcTemplate: JdbcTemplate) {
    private val logger = LoggerFactory.getLogger(VectorService::class.java)

    // Vector dimensions
    private val USER_VECTOR_DIM = 64
    private val POST_VECTOR_DIM = 64

    /**
     * Create user vector with proper normalization
     * Focusing only on quantifiable, continuous features
     */
    fun createUserVector(
        userDto: UserDto,
        genres: List<UserGenreDto>,
        interactions: List<UserPostInteractionDto>,
        trailerInteractions: List<TrailerInteractionDto> = emptyList()
    ): FloatArray {
        val vector = mutableListOf<Float>()

        // Add runtime preferences (truly numerical)
        vector.add(normalizeRuntimePreference(userDto.minMovie, userDto.maxMovie))
        vector.add(normalizeRuntimePreference(userDto.minTV, userDto.maxTV))

        // Add date preferences (truly numerical)
        vector.add(normalizeDatePreference(userDto.oldestDate, userDto.recentDate))

        // Add explicit genre preferences (from user_genres)
        addGenreFeatures(vector, genres)

        // Add content preferences based on actual interactions
        if (interactions.isNotEmpty()) {
            addContentPreferenceFeatures(vector, userDto.userId ?: 0)
        }

        // Add post interaction features
        addPostInteractionFeatures(vector, interactions)

        // Add trailer interaction features
        addTrailerInteractionFeatures(vector, trailerInteractions)

        // Ensure vector has correct dimension
        val paddedVector = padVector(vector.toFloatArray(), USER_VECTOR_DIM)

        // Normalize vector
        return normalizeVector(paddedVector)
    }

    /**
     * Create post vector with proper normalization
     * Focusing only on quantifiable, continuous features
     */
    fun createPostVector(postDto: PostDto, contentType: String = "posts"): FloatArray {
        val vector = mutableListOf<Float>()

        // Add post type feature (binary - movie or TV)
        vector.add(if (postDto.type == "movie") 1f else 0f)

        // Add release date feature (truly numerical)
        vector.add(normalizeReleaseDate(postDto.releaseDate))

        // Add popularity features (truly numerical)
        vector.add(normalizeVoteAverage(postDto.voteAverage))
        vector.add(normalizeVoteCount(postDto.voteCount))
        vector.add(normalizePopularity(postDto.popularity))

        // Add genre features
        addPostGenreFeatures(vector, postDto)

        // Add interaction metrics based on content type
        if (postDto.postId != null) {
            if (contentType == "trailers") {
                // For trailers, prioritize trailer interactions by adding them first
                addTrailerInteractionMetrics(vector, postDto.postId)
                // Add regular interaction metrics as supplementary data
                addInteractionMetrics(vector, postDto.postId)
            } else {
                // For regular posts, prioritize standard interactions
                addInteractionMetrics(vector, postDto.postId)
                // Add trailer interactions as supplementary data if available
                addTrailerInteractionMetrics(vector, postDto.postId)
            }
        }

        // Ensure vector has correct dimension
        val paddedVector = padVector(vector.toFloatArray(), POST_VECTOR_DIM)

        // Normalize vector
        return normalizeVector(paddedVector)
    }

    /**
     * Store user vector using pgvector
     */
    fun storeUserVector(userId: Int, vector: FloatArray) {
        try {
            // Convert vector to string representation for pgvector
            val vectorStr = vector.joinToString(",", "[", "]")

            jdbcTemplate.update(
                """
                INSERT INTO user_vectors (user_id, vector, dimension, updated_at)
                VALUES (?, ?::vector, ?, NOW())
                ON CONFLICT (user_id) DO UPDATE
                SET vector = ?::vector, dimension = ?, updated_at = NOW()
            """, userId, vectorStr, vector.size, vectorStr, vector.size
            )

            logger.info("Vector stored for user $userId")
        } catch (e: SQLException) {
            logger.error("Error storing user vector: ${e.message}")
            throw e
        }
    }

    /**
     * Store post vector using pgvector
     */
    fun storePostVector(postId: Int, vector: FloatArray) {
        try {
            // Convert vector to string representation for pgvector
            val vectorStr = vector.joinToString(",", "[", "]")

            jdbcTemplate.update(
                """
                INSERT INTO post_vectors (post_id, vector, dimension, updated_at)
                VALUES (?, ?::vector, ?, NOW())
                ON CONFLICT (post_id) DO UPDATE
                SET vector = ?::vector, dimension = ?, updated_at = NOW()
            """, postId, vectorStr, vector.size, vectorStr, vector.size
            )

            logger.info("Vector stored for post $postId")
        } catch (e: SQLException) {
            logger.error("Error storing post vector: ${e.message}")
            throw e
        }
    }

    /**
     * Get user vector
     */
    @Transactional(readOnly = true)
    fun getUserVector(userId: Int): FloatArray {
        try {
            val vectorStr = jdbcTemplate.queryForObject(
                "SELECT vector::text FROM user_vectors WHERE user_id = ?",
                String::class.java,
                userId
            )

            return parseVectorString(vectorStr)
        } catch (e: EmptyResultDataAccessException) {
            return createDefaultVector()
        } catch (e: Exception) {
            logger.error("Error retrieving user vector: ${e.message}")
            throw e
        }
    }

    /**
     * Create a default vector for users without an existing vector
     * This ensures getUserVector never returns null
     */
    private fun createDefaultVector(): FloatArray {
        // Create a vector with the correct dimension and random values
        // Alternatively, you could use zeros or some other default
        val dimension = 32 // Use whatever dimension your vectors have
        return FloatArray(dimension) { 0f }
    }

    /**
     * Get vectors for multiple posts
     */
    fun getPostVectors(postIds: List<Int>): Map<Int, FloatArray> {
        if (postIds.isEmpty()) {
            return emptyMap()
        }

        try {
            // Create placeholders for SQL IN clause
            val placeholders = postIds.joinToString(",") { "?" }

            // Query to get vectors for all requested post IDs
            val query = """
            SELECT post_id, vector::text 
            FROM post_vectors 
            WHERE post_id IN ($placeholders)
        """

            // Store results in a mutable map
            val resultMap = mutableMapOf<Int, FloatArray>()

            // Execute query with post IDs as parameters
            jdbcTemplate.query(query, { rs ->
                val id = rs.getInt("post_id")
                val vectorStr = rs.getString("vector")
                val parsedVector = parseVectorString(vectorStr)
                resultMap[id] = parsedVector
            }, *postIds.map { it as Any }.toTypedArray())

            return resultMap
        } catch (e: Exception) {
            logger.error("Error retrieving post vectors: ${e.message}")
            throw e
        }
    }

    /**
     * Get post vector for a single post (keeping for backward compatibility)
     */
    fun getPostVector(postId: Int): FloatArray? {
        return getPostVectors(listOf(postId))[postId]
    }

    /**
     * Find similar posts using vector similarity directly in the database
     */
    fun findSimilarPosts(
        userVector: FloatArray?,
        limit: Int = 10,
        excludePostIds: List<Int> = emptyList(),
        contentType: String? = null
    ): List<Int> {
        try {
            val vectorStr = userVector?.joinToString(",", "[", "]")

            val excludeClause = if (excludePostIds.isNotEmpty()) {
                "AND pv.post_id NOT IN (${excludePostIds.joinToString(",")})"
            } else ""

            val typeClause = if (contentType != null) {
                "AND p.type = ?"
            } else ""

            val query = """
                SELECT pv.post_id 
                FROM post_vectors pv
                JOIN posts p ON pv.post_id = p.post_id
                WHERE 1=1 
                $excludeClause
                $typeClause
                ORDER BY pv.vector <=> ?::vector
                LIMIT ?
            """

            return if (contentType != null) {
                jdbcTemplate.queryForList(
                    query,
                    Int::class.java,
                    contentType,
                    vectorStr,
                    limit
                )
            } else {
                jdbcTemplate.queryForList(
                    query,
                    Int::class.java,
                    vectorStr,
                    limit
                )
            }
        } catch (e: Exception) {
            logger.error("Error finding similar posts: ${e.message}")
            throw e
        }
    }

    /**
     * Parse vector string from database
     */
    private fun parseVectorString(vectorStr: String): FloatArray {
        return try {
            // Remove brackets and split by commas
            vectorStr
                .trim('[', ']')
                .split(",")
                .map { it.trim().toFloat() }
                .toFloatArray()
        } catch (e: Exception) {
            logger.error("Error parsing vector string: ${e.message}")
            FloatArray(0)
        }
    }

    /**
     * Normalize vector to unit length (L2 normalization)
     */
    fun normalizeVector(vector: FloatArray): FloatArray {
        val magnitude = sqrt(vector.sumOf { it * it.toDouble() }).toFloat()

        // Avoid division by zero
        if (magnitude < 1e-8) {
            return vector
        }

        // Normalize each element
        return FloatArray(vector.size) { i -> vector[i] / magnitude }
    }

    /**
     * Pad or truncate vector to desired dimension
     */
    private fun padVector(vector: FloatArray, targetDim: Int): FloatArray {
        return when {
            vector.size == targetDim -> vector
            vector.size < targetDim -> {
                // Pad with zeros
                val padded = FloatArray(targetDim)
                vector.copyInto(padded)
                padded
            }

            else -> {
                // Truncate
                vector.copyOf(targetDim)
            }
        }
    }

    /**
     * Add genre features based on explicitly set user preferences
     */
    private fun addGenreFeatures(vector: MutableList<Float>, genres: List<UserGenreDto>) {
        // Get total number of genres
        val totalGenres = getTotalGenres()

        // Create one-hot encoding with weights based on priority
        val genreVector = FloatArray(totalGenres) { 0f }

        genres.forEach { genre ->
            val index = genre.genreId - 1 // Assuming genre IDs start at 1
            if (index in genreVector.indices) {
                genreVector[index] = normalizeGenrePriority(genre.priority)
            }
        }

        // Add genre vector to main vector
        vector.addAll(genreVector.toList())
    }

    /**
     * Add content preference features based on actual user interactions
     */
    private fun addContentPreferenceFeatures(vector: MutableList<Float>, userId: Int) {
        try {
            // Get genres of posts the user interacted with
            val interactedGenres = mutableListOf<Quadruple<Int, String, Int, Int>>()

            jdbcTemplate.query("""
                SELECT g.genre_id, g.name, COUNT(*) as interaction_count,
                       SUM(CASE WHEN upi.like_state THEN 1 ELSE 0 END) as liked_count
                FROM user_post_interactions upi
                JOIN posts p ON upi.post_id = p.post_id
                JOIN post_games pg ON p.post_id = pg.post_id
                JOIN genres g ON pg.genre_id = g.genre_id
                WHERE upi.user_id = ?
                GROUP BY g.genre_id, g.name
                ORDER BY interaction_count DESC
            """, { rs ->
                val genreId = rs.getInt("genre_id")
                val name = rs.getString("name")
                val interactionCount = rs.getInt("interaction_count")
                val likedCount = rs.getInt("liked_count")

                interactedGenres.add(
                    Quadruple(
                        genreId,
                        name,
                        interactionCount,
                        likedCount
                    )
                )
            }, userId
            )

            if (interactedGenres.isEmpty()) {
                // No interacted genres found, add default values
                val defaultGenreVector = FloatArray(getTotalGenres()) { 0.5f }
                vector.addAll(defaultGenreVector.toList())
                return
            }

            // Calculate normalized weights for each genre
            val totalInteractions = interactedGenres.sumOf { it.third }.toFloat().coerceAtLeast(1f)

            // Create weighted vector that emphasizes liked genres
            val genreVector = FloatArray(getTotalGenres()) { 0f }

            interactedGenres.forEach { (genreId, _, interactionCount, likedCount) ->
                val index = genreId - 1
                if (index in genreVector.indices) {
                    // Base weight from interaction count
                    val interactionWeight = interactionCount / totalInteractions

                    // Additional weight from like ratio
                    val likeRatio = if (interactionCount > 0) likedCount.toFloat() / interactionCount else 0f

                    // Combined weight with emphasis on liked genres
                    genreVector[index] = (interactionWeight * (1f + likeRatio)).coerceIn(0f, 1f)
                }
            }

            // Add weighted genre preference vector to main vector
            vector.addAll(genreVector.toList())

            // Add temporal interaction patterns (numerical, continuous data)
            addTemporalInteractionPatterns(vector, userId)

        } catch (e: Exception) {
            logger.error("Error adding content preference features: ${e.message}")
            // Add default values in case of error
            val defaultGenreVector = FloatArray(getTotalGenres()) { 0.5f }
            vector.addAll(defaultGenreVector.toList())
        }
    }

    /**
     * Add temporal interaction patterns to the user vector
     */
    private fun addTemporalInteractionPatterns(vector: MutableList<Float>, userId: Int) {
        try {
            // Get time-of-day preferences
            val timeOfDayPreferences = jdbcTemplate.queryForMap(
                """
                SELECT
                    SUM(CASE WHEN EXTRACT(HOUR FROM TO_TIMESTAMP(start_timestamp, 'YYYY-MM-DD HH24:MI:SS')) BETWEEN 5 AND 11 THEN 1 ELSE 0 END)::float / COUNT(*) as morning_ratio,
                    SUM(CASE WHEN EXTRACT(HOUR FROM TO_TIMESTAMP(start_timestamp, 'YYYY-MM-DD HH24:MI:SS')) BETWEEN 12 AND 17 THEN 1 ELSE 0 END)::float / COUNT(*) as afternoon_ratio,
                    SUM(CASE WHEN EXTRACT(HOUR FROM TO_TIMESTAMP(start_timestamp, 'YYYY-MM-DD HH24:MI:SS')) BETWEEN 18 AND 23 THEN 1 ELSE 0 END)::float / COUNT(*) as evening_ratio,
                    SUM(CASE WHEN EXTRACT(HOUR FROM TO_TIMESTAMP(start_timestamp, 'YYYY-MM-DD HH24:MI:SS')) BETWEEN 0 AND 4 THEN 1 ELSE 0 END)::float / COUNT(*) as night_ratio
                FROM user_post_interactions
                WHERE user_id = ?
            """, userId
            )

            // Add time-of-day preferences
            vector.add(timeOfDayPreferences["morning_ratio"] as Float? ?: 0.25f)
            vector.add(timeOfDayPreferences["afternoon_ratio"] as Float? ?: 0.25f)
            vector.add(timeOfDayPreferences["evening_ratio"] as Float? ?: 0.25f)
            vector.add(timeOfDayPreferences["night_ratio"] as Float? ?: 0.25f)

        } catch (e: Exception) {
            logger.error("Error adding temporal interaction patterns: ${e.message}")
            // Add default values
            vector.add(0.25f) // morning
            vector.add(0.25f) // afternoon
            vector.add(0.25f) // evening
            vector.add(0.25f) // night
        }
    }

    /**
     * Add post interaction features to vector
     */
    private fun addPostInteractionFeatures(vector: MutableList<Float>, interactions: List<UserPostInteractionDto>) {
        if (interactions.isEmpty()) {
            // Default values if no interactions
            vector.add(0.5f) // Like ratio
            vector.add(0.5f) // Save ratio
            vector.add(0.2f) // Comment ratio
            vector.add(0.5f) // Engagement duration
            return
        }

        // Calculate interaction ratios
        val likeRatio = interactions.count { it.likeState } / interactions.size.toFloat()
        val saveRatio = interactions.count { it.saveState } / interactions.size.toFloat()
        val commentRatio = interactions.count { it.commentButtonPressed } / interactions.size.toFloat()

        // Calculate average engagement duration
        val avgDuration = interactions
            .map { calculateDuration(it.startTimestamp, it.endTimestamp) }
            .average()
            .toFloat()

        // Add to vector
        vector.add(likeRatio)
        vector.add(saveRatio)
        vector.add(commentRatio)
        vector.add(normalizeEngagementDuration(avgDuration))
    }

    /**
     * Add trailer interaction features to vector
     */
    private fun addTrailerInteractionFeatures(vector: MutableList<Float>, interactions: List<TrailerInteractionDto>) {
        if (interactions.isEmpty()) {
            // Default values if no interactions
            vector.add(0.5f) // Like ratio
            vector.add(0.5f) // Save ratio
            vector.add(0.2f) // Comment ratio
            vector.add(0.5f) // Mute ratio
            vector.add(0.5f) // Replay count
            vector.add(0.5f) // Engagement duration
            return
        }

        // Calculate interaction ratios
        val likeRatio = interactions.count { it.likeState } / interactions.size.toFloat()
        val saveRatio = interactions.count { it.saveState } / interactions.size.toFloat()
        val commentRatio = interactions.count { it.commentButtonPressed } / interactions.size.toFloat()
        val muteRatio = interactions.count { it.isMuted } / interactions.size.toFloat()

        // Calculate average replay count
        val avgReplayCount = interactions
            .map { it.replayCount.toFloat() }
            .average()
            .toFloat()

        // Calculate average engagement duration
        val avgDuration = interactions
            .map { calculateDuration(it.startTimestamp, it.endTimestamp) }
            .average()
            .toFloat()

        // Add to vector
        vector.add(likeRatio)
        vector.add(saveRatio)
        vector.add(commentRatio)
        vector.add(muteRatio)
        vector.add(normalizeReplayCount(avgReplayCount))
        vector.add(normalizeEngagementDuration(avgDuration))
    }

    /**
     * Add interaction metrics for a specific post
     */
    private fun addInteractionMetrics(vector: MutableList<Float>, postId: Int) {
        try {
            // Get interaction metrics for this post
            val metrics = jdbcTemplate.queryForMap(
                """
                SELECT 
                    AVG(EXTRACT(EPOCH FROM (TO_TIMESTAMP(end_timestamp,'YYYY-MM-DD HH24:MI:SS') - 
                                           TO_TIMESTAMP(start_timestamp,'YYYY-MM-DD HH24:MI:SS')))) as avg_duration,
                    COUNT(*) as total_views,
                    SUM(CASE WHEN like_state THEN 1 ELSE 0 END)::float / NULLIF(COUNT(*), 0) as like_ratio,
                    SUM(CASE WHEN save_state THEN 1 ELSE 0 END)::float / NULLIF(COUNT(*), 0) as save_ratio
                FROM user_post_interactions
                WHERE post_id = ?
            """, postId
            )

            // Add normalized metrics to vector
            vector.add(normalizeWatchDuration(metrics["avg_duration"] as Double? ?: 0.0))
            vector.add(normalizeViewCount(metrics["total_views"] as Long? ?: 0))
            vector.add(metrics["like_ratio"] as Float? ?: 0.5f)
            vector.add(metrics["save_ratio"] as Float? ?: 0.5f)

        } catch (e: Exception) {
            logger.error("Error adding interaction metrics for post $postId: ${e.message}")
            // Add default values in case of error
            vector.add(0.5f) // watch duration
            vector.add(0.5f) // view count
            vector.add(0.5f) // like ratio
            vector.add(0.5f) // save ratio
        }
    }

    private fun addTrailerInteractionMetrics(vector: MutableList<Float>, postId: Int) {
        try {
            // Get trailer interaction metrics for this post
            val metrics = jdbcTemplate.queryForMap(
                """
            SELECT 
                AVG(EXTRACT(EPOCH FROM (TO_TIMESTAMP(end_timestamp,'YYYY-MM-DD HH24:MI:SS') - 
                                       TO_TIMESTAMP(start_timestamp,'YYYY-MM-DD HH24:MI:SS')))) as avg_duration,
                COUNT(*) as total_views,
                AVG(replay_count) as avg_replays,
                SUM(CASE WHEN like_state THEN 1 ELSE 0 END)::float / NULLIF(COUNT(*), 0) as like_ratio,
                SUM(CASE WHEN save_state THEN 1 ELSE 0 END)::float / NULLIF(COUNT(*), 0) as save_ratio,
                SUM(CASE WHEN is_muted THEN 0 ELSE 1 END)::float / NULLIF(COUNT(*), 0) as unmuted_ratio
            FROM user_trailer_interactions
            WHERE post_id = ?
        """, postId
            )

            // Add normalized metrics to vector
            vector.add(normalizeWatchDuration(metrics["avg_duration"] as Double? ?: 0.0))
            vector.add(normalizeViewCount(metrics["total_views"] as Long? ?: 0))
            vector.add(normalizeReplayCount(metrics["avg_replays"] as Double? ?: 0.0))
            vector.add(metrics["like_ratio"] as Float? ?: 0.5f)
            vector.add(metrics["save_ratio"] as Float? ?: 0.5f)
            vector.add(metrics["unmuted_ratio"] as Float? ?: 0.5f)

        } catch (e: Exception) {
            logger.error("Error adding trailer interaction metrics for post $postId: ${e.message}")
            // Add default values in case of error
            vector.add(0.5f) // watch duration
            vector.add(0.5f) // view count
            vector.add(0.5f) // replay count
            vector.add(0.5f) // like ratio
            vector.add(0.5f) // save ratio
            vector.add(0.5f) // unmuted ratio
        }
    }

    private fun normalizeReplayCount(replayCount: Double): Float {
        // Transform replay count to a 0-1 scale using a logarithmic scale
        // Assuming 5+ replays indicates very high engagement
        return min(1.0f, (log10(replayCount.toFloat() + 1) / log10(6.0f)))
    }

    /**
     * Overloaded version to handle Float inputs directly
     */
    private fun normalizeReplayCount(replayCount: Float): Float {
        // Use the Double version for consistency
        return normalizeReplayCount(replayCount.toDouble())
    }


    /**
     * Add post genre features to vector
     */
    private fun addPostGenreFeatures(vector: MutableList<Float>, postDto: PostDto) {
        // Get total number of genres
        val totalGenres = getTotalGenres()

        // Create one-hot encoding for genres
        val genreVector = FloatArray(totalGenres) { 0f }

        try {
            // Parse genre IDs from comma-separated string
            val genreIds = postDto.genreIds.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { it.toInt() }

            genreIds.forEach { genreId ->
                val index = genreId - 1 // Assuming genre IDs start at 1
                if (index in genreVector.indices) {
                    genreVector[index] = 1f
                }
            }
        } catch (e: Exception) {
            logger.warn("Error parsing genre IDs for post ${postDto.postId}: ${e.message}")
        }

        // Add genre vector to main vector
        vector.addAll(genreVector.toList())
    }

    /**
     * Calculate duration between two timestamps
     */
    private fun calculateDuration(start: String, end: String): Float {
        try {
            val startTime = java.sql.Timestamp.valueOf(start).time
            val endTime = java.sql.Timestamp.valueOf(end).time
            return max(0, endTime - startTime).toFloat() / 1000f // Duration in seconds
        } catch (e: Exception) {
            return 0f
        }
    }

    /**
     * Get total number of genres in the system
     */
    private fun getTotalGenres(): Int {
        return try {
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM genres",
                Int::class.java
            ) ?: 20 // Default fallback if query fails
        } catch (e: Exception) {
            logger.warn("Error getting total genres: ${e.message}")
            20 // Default value
        }
    }

    // Normalization helper functions for continuous numerical features

    private fun normalizeRuntimePreference(min: Int?, max: Int?): Float {
        // Normalize runtime preference to 0-1 range
        return when {
            min != null && max != null -> {
                ((min + max) / 2f / 240f).coerceIn(0f, 1f) // Normalize to 0-1 assuming 240 minutes as max
            }

            min != null -> (min / 240f).coerceIn(0f, 1f)
            max != null -> (max / 240f).coerceIn(0f, 1f)
            else -> 0.5f // Default to mid-point
        }
    }

    private fun normalizeDatePreference(oldestDate: String, recentDate: String): Float {
        try {
            // Extract years from dates
            val oldestYear = oldestDate.substring(0, 4).toInt()
            val recentYear = recentDate.substring(0, 4).toInt()

            // Calculate average year preference
            val averageYear = (oldestYear + recentYear) / 2

            // Normalize to 0-1 range (1900-2030)
            return ((averageYear - 1900).toFloat() / 130f).coerceIn(0f, 1f)
        } catch (e: Exception) {
            return 0.5f // Default to mid-point
        }
    }

    private fun normalizeReleaseDate(releaseDate: String?): Float {
        if (releaseDate.isNullOrBlank()) return 0.5f

        try {
            val year = releaseDate.substring(0, 4).toInt()
            // Normalize to 0-1 range (1900-2030)
            return ((year - 1900).toFloat() / 130f).coerceIn(0f, 1f)
        } catch (e: Exception) {
            return 0.5f
        }
    }

    private fun normalizeVoteAverage(voteAverage: Double): Float {
        // TMDB vote average is 0-10, normalize to 0-1
        return (voteAverage / 10f).toFloat().coerceIn(0f, 1f)
    }

    private fun normalizeVoteCount(voteCount: Int): Float {
        // Log-scale normalization for vote count
        if (voteCount <= 0) return 0f
        val logCount = ln(voteCount.toFloat() + 1)
        // Max out at ~22K votes (ln(22K) ≈ 10)
        return (logCount / 10f).coerceIn(0f, 1f)
    }

    private fun normalizePopularity(popularity: Double): Float {
        // TMDB popularity is unbounded, use sigmoid-like normalization
        return (1f / (1f + kotlin.math.exp(-0.1f * popularity.toFloat() + 2f))).coerceIn(0f, 1f)
    }

    private fun normalizeGenrePriority(priority: Int): Float {
        // Normalize genre priority to 0-1 range
        return (priority / 10f).coerceIn(0f, 1f)
    }

    private fun normalizeEngagementDuration(durationInSeconds: Float): Float {
        // Normalize engagement duration (cap at 30 minutes = 1800 seconds)
        return (durationInSeconds / 1800f).coerceIn(0f, 1f)
    }

    private fun normalizeWatchDuration(avgDuration: Double): Float {
        // Normalize average watch duration (cap at 2 hours = 7200 seconds)
        return (avgDuration / 7200.0).toFloat().coerceIn(0f, 1f)
    }

    private fun normalizeViewCount(viewCount: Long): Float {
        // Log-scale normalization for view count
        if (viewCount <= 0) return 0f
        val logCount = ln(viewCount.toFloat() + 1)
        // Max out at ~22K views (ln(22K) ≈ 10)
        return (logCount / 10f).coerceIn(0f, 1f)
    }

/**
 * Helper class for storing four values together
 */
data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

}