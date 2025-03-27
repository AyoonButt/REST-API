package com.api.postgres.recommendations

import com.api.postgres.MLRecommendationRequest
import com.api.postgres.MLRecommendationResponse
import com.api.postgres.PostDto
import com.api.postgres.ScoredPost
import com.api.postgres.repositories.PostRepository
import com.api.postgres.repositories.UserPostInteractionRepository
import com.api.postgres.repositories.UserTrailerInteractionRepository
import com.api.postgres.services.Posts
import com.api.postgres.services.UsersService
import org.slf4j.LoggerFactory
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.time.Instant
import kotlin.math.sqrt

@Service
class RecommendationService(
    private val vectorService: VectorService,
    private val userService: UsersService,
    private val postsService: Posts,
    private val mlModelClient: MLModelClient,
    private val jdbcTemplate: JdbcTemplate,
    private val userPostInteractionRepository: UserPostInteractionRepository,
    private val userTrailerInteractionRepository: UserTrailerInteractionRepository
) {
    private val logger = LoggerFactory.getLogger(RecommendationService::class.java)

    /**
     * Get personalized recommendations with duplicate prevention handled by the ML model
     */

    @Transactional(readOnly = true)
    suspend fun getCandidateVectors(userId: Int, limit: Int = 100, loosenFiltering: Boolean = false, loosenAttempt: Int = 0, offset: Int = 0): Map<Int, FloatArray> {
        // Get user preferences
        val preferences = userService.getUserPreferences(userId) ?: return emptyMap()

        // Get filtered post IDs
        val postIds = postsService.getFilteredPostIds(preferences, loosenFiltering, loosenAttempt, limit, offset)

        // Return empty map if no candidates were found
        if (postIds.isEmpty()) return emptyMap()

        // Get vectors for the filtered posts
        return vectorService.getPostVectors(postIds)
    }

    @Transactional(readOnly = true)
    fun getFilteredPostRecommendations(
        userId: Int,
        postIds: List<String>,
        scores: List<Double>,
        limit: Int = 20
    ): List<ScoredPost> {
        try {
            logger.info("Filtering post recommendations for user $userId with ${postIds.size} candidates")

            // Convert string IDs to integers
            val postIdsInt = postIds.map { it.toInt() }

            // Get posts that the user has already interacted with
            val interactedPostIds = if (postIdsInt.isNotEmpty()) {
                userPostInteractionRepository.findInteractedPostIds(userId, postIdsInt).toSet()
            } else {
                emptySet()
            }

            logger.info("User $userId has ${interactedPostIds.size} interacted posts from candidate set")

            // Filter out interacted posts and combine with scores
            val filteredPosts = postIdsInt.zip(scores)
                .filter { (postId, _) -> postId !in interactedPostIds }
                .sortedByDescending { (_, score) -> score }
                .take(limit)
                .map { (postId, score) -> ScoredPost(postId.toString(), score) }

            logger.info("Returning ${filteredPosts.size} filtered post recommendations for user $userId")

            // Return list of scored posts
            return filteredPosts
        } catch (e: Exception) {
            logger.error("Error filtering post recommendations: ${e.message}", e)
            throw e
        }
    }

    @Transactional(readOnly = true)
    fun getFilteredTrailerRecommendations(
        userId: Int,
        postIds: List<String>,
        scores: List<Double>,
        limit: Int = 20
    ): List<ScoredPost> {
        try {
            logger.info("Filtering trailer recommendations for user $userId with ${postIds.size} candidates")

            // Convert string IDs to integers
            val postIdsInt = postIds.map { it.toInt() }

            // Get trailers that the user has already interacted with
            val interactedTrailerIds = if (postIdsInt.isNotEmpty()) {
                userTrailerInteractionRepository.findInteractedPostIds(userId, postIdsInt).toSet()
            } else {
                emptySet()
            }

            logger.info("User $userId has ${interactedTrailerIds.size} interacted trailers from candidate set")

            // Filter out interacted trailers and combine with scores
            val filteredTrailers = postIdsInt.zip(scores)
                .filter { (postId, _) -> postId !in interactedTrailerIds }
                .sortedByDescending { (_, score) -> score }
                .take(limit)
                .map { (postId, score) -> ScoredPost(postId.toString(), score) }

            logger.info("Returning ${filteredTrailers.size} filtered trailer recommendations for user $userId")

            // Return list of scored posts
            return filteredTrailers
        } catch (e: Exception) {
            logger.error("Error filtering trailer recommendations: ${e.message}", e)
            throw e
        }
    }

    /**
     * Get recommendations for users, handling both regular posts and trailers
     */
    @Transactional(readOnly = true)
    suspend fun getRecommendations(
        userId: Int,
        contentType: String? = null,  // "posts" or "trailers" or null for all
        limit: Int = 20,
        offset: Int = 0
    ): List<PostDto> {
        try {
            logger.info("Getting recommendations for user $userId, type: $contentType, limit: $limit, offset: $offset")
            // Check if user exists
            checkUserExists(userId)

            // Create simplified ML model request
            val request = MLRecommendationRequest(
                userId = userId,
                contentType = contentType,
                limit = limit,
                offset = offset
            )

            // Get recommendations from ML model
            val response = mlModelClient.getRecommendations(request)

            // If ML model returns no recommendations, use appropriate fallback
            if (response == null || response.postIds.isEmpty()) {
                logger.info("No recommendations from ML model, using fallback")

                // Use content-specific fallback based on request type
                return if (contentType == "trailers") {
                    getTrailerFallbackRecommendations(userId, emptyList(), limit, offset)
                } else {
                    getFallbackRecommendations(userId, contentType, emptyList(), limit, offset)
                }
            }

            // Get full post data for the recommended posts
            return postsService.getPostDtosForInteractionIds(response.postIds)

        } catch (e: NotFoundException) {
            logger.warn("User not found: ${e.message}")
            throw e
        } catch (e: Exception) {
            logger.error("Error getting recommendations: ${e.message}")
            throw e
        }
    }

    /**
     * Fallback method using vector similarity search
     */
    suspend fun getFallbackRecommendations(
        userId: Int,
        contentType: String?,
        excludePostIds: List<Int>,
        limit: Int,
        offset: Int
    ): List<PostDto> {
        try {
            // Get user vector
            val userVector = vectorService.getUserVector(userId)

            // Get user subscriptions
            val subscriptions = getUserSubscriptions(userId)

            // Get subscription provider IDs
            val subscriptionProviderIds = if (subscriptions.isNotEmpty()) {
                jdbcTemplate.queryForList("""
                SELECT provider_id FROM subscription_providers
                WHERE provider_name IN (${subscriptions.joinToString { "'$it'" }})
            """, Int::class.java)
            } else {
                emptyList()
            }

            // Create subscription filter for SQL
            val subscriptionFilter = if (subscriptionProviderIds.isNotEmpty()) {
                """
            AND EXISTS (
                SELECT 1 FROM subscription_providers sp
                WHERE sp.provider_name = p.subscription
                AND sp.provider_id IN (${subscriptionProviderIds.joinToString()})
            )
            """
            } else {
                ""
            }

            // Create content type filter
            val typeFilter = if (contentType != null) "AND p.type = '$contentType'" else ""

            // Create exclusion filter
            val excludeFilter = if (excludePostIds.isNotEmpty()) {
                "AND p.post_id NOT IN (${excludePostIds.joinToString()})"
            } else {
                ""
            }

            // Find similar posts using vector similarity
            val similarPostIds = vectorService.findSimilarPosts(
                userVector = userVector,
                limit = limit,
                excludePostIds = excludePostIds,
                contentType = contentType
            )

            // Get full post data
            return postsService.getPostDtosForInteractionIds(similarPostIds)

        } catch (e: Exception) {
            logger.error("Error getting fallback recommendations: ${e.message}")
            throw e
        }
    }

    /**
     * Fallback method for trailer recommendations
     */
    suspend fun getTrailerFallbackRecommendations(
        userId: Int,
        excludePostIds: List<Int>,
        limit: Int,
        offset: Int
    ): List<PostDto> {
        try {
            // Get user vector
            val userVector = vectorService.getUserVector(userId)

            // Create exclusion filter
            val excludeFilter = if (excludePostIds.isNotEmpty()) {
                "AND p.post_id NOT IN (${excludePostIds.joinToString()})"
            } else {
                ""
            }

            // Get posts with trailers
            val trailerPostIds = jdbcTemplate.queryForList("""
            SELECT p.post_id FROM posts p
            WHERE p.video_key IS NOT NULL AND p.video_key != '' $excludeFilter
        """, Int::class.java)

            if (trailerPostIds.isEmpty()) {
                return emptyList()
            }

            // Get post vectors
            val postVectors = vectorService.getPostVectors(trailerPostIds)

            // Calculate similarity scores
            val scoredPosts = trailerPostIds.mapNotNull { postId ->
                val postVector = postVectors[postId] ?: return@mapNotNull null
                val score = calculateCosineSimilarity(userVector, postVector)
                Pair(postId, score)
            }

            // Sort by similarity score
            val sortedPostIds = scoredPosts
                .sortedByDescending { it.second }
                .map { it.first }

            // Apply pagination
            val endIdx = minOf(offset + limit, sortedPostIds.size)

            val pagedPostIds = if (offset < sortedPostIds.size) {
                sortedPostIds.subList(offset, endIdx)
            } else {
                emptyList()
            }

            // Get full post data
            return return postsService.getPostDtosForInteractionIds(pagedPostIds)

        } catch (e: Exception) {
            logger.error("Error getting trailer fallback recommendations: ${e.message}")
            throw e
        }
    }

    /**
     * Check if user exists
     */
    private fun checkUserExists(userId: Int) {
        val exists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE user_id = ?",
            Int::class.java,
            userId
        ) ?: 0

        if (exists == 0) {
            throw NotFoundException()
        }
    }


    /**
     * Get user's subscription providers
     */
    private fun getUserSubscriptions(userId: Int): List<String> {
        return jdbcTemplate.queryForList("""
            SELECT sp.provider_name
            FROM subscription_providers sp
            JOIN user_subscriptions us ON sp.provider_id = us.provider_id
            WHERE us.user_id = ?
        """, String::class.java, userId)
    }

    /**
     * Get full post data for a list of post IDs
     */
    private fun getPostsByIds(postIds: List<Int>): List<PostDto> {
        if (postIds.isEmpty()) return emptyList()

        val placeholders = postIds.joinToString(",") { "?" }

        return jdbcTemplate.query("""
            SELECT * FROM posts 
            WHERE post_id IN ($placeholders)
            ORDER BY FIELD(post_id, $placeholders) -- Maintain order from postIds
        """, { rs, _ -> mapToPostDto(rs) }, *(postIds + postIds).toTypedArray())
    }

    /**
     * Calculate cosine similarity between two vectors
     */
    private fun calculateCosineSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
        val minLength = minOf(vec1.size, vec2.size)

        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f

        for (i in 0 until minLength) {
            dotProduct += vec1[i] * vec2[i]
            norm1 += vec1[i] * vec1[i]
            norm2 += vec2[i] * vec2[i]
        }

        val denominator = sqrt(norm1) * sqrt(norm2)
        return if (denominator > 0) dotProduct / denominator else 0f
    }

    /**
     * Map result set to PostDto
     */
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
}