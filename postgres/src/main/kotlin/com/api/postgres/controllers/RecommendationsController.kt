package com.api.postgres.controllers

import com.api.postgres.FilterRecommendationRequest
import com.api.postgres.PostDto
import com.api.postgres.ScoredPost
import com.api.postgres.recommendations.BehaviorProfiler
import com.api.postgres.recommendations.MetadataService
import com.api.postgres.recommendations.RecommendationService
import com.api.postgres.recommendations.VectorService
import org.apache.coyote.BadRequestException
import org.slf4j.LoggerFactory
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/recommendations")
class RecommendationsController(
    private val recommendationService: RecommendationService,
    private val vectorService: VectorService,
    private val behaviorProfiler: BehaviorProfiler,
    private val metadataService: MetadataService
) {
    private val logger = LoggerFactory.getLogger(RecommendationsController::class.java)

    /**
     * Get recommendations for a user
     */
    @GetMapping("/{userId}")
    suspend fun getRecommendations(
        @PathVariable userId: Int,
        @RequestParam(required = false) contentType: String?,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): ResponseEntity<List<PostDto>> {
        try {
            logger.info("Request for recommendations: userId=$userId, contentType=$contentType, limit=$limit, offset=$offset")

            val recommendations = recommendationService.getRecommendations(userId, contentType, limit, offset)
            // Extract just the content from the Page object
            return ResponseEntity.ok(recommendations)

        } catch (e: NotFoundException) {
            logger.warn("User not found: ${e.message}")
            return ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("Error getting recommendations: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    /**
     * Get candidate vectors for ML model training or testing
     */
    @GetMapping("/{userId}/candidates")
    suspend fun getCandidateVectors(
        @PathVariable userId: Int,
        @RequestParam(defaultValue = "100") limit: Int,
        @RequestParam(defaultValue = "false") loosenFiltering: Boolean,
        @RequestParam(defaultValue = "0") loosenAttempt: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): ResponseEntity<Map<Int, FloatArray>> {
        try {
            logger.info("Fetching candidate vectors: userId=$userId, limit=$limit, offset=$offset")
            val vectors = recommendationService.getCandidateVectors(
                userId, limit, loosenFiltering, loosenAttempt, offset
            )

            if (vectors.isEmpty()) {
                return ResponseEntity.noContent().build()
            }

            return ResponseEntity.ok(vectors)
        } catch (e: Exception) {
            logger.error("Error getting candidate vectors: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    /**
     * Get user vector
     */
    @GetMapping("/users/{userId}/vector")
    fun getUserVector(@PathVariable userId: Int): ResponseEntity<FloatArray> {
        try {
            logger.info("Fetching user vector: userId=$userId")
            val vector = vectorService.getUserVector(userId)
            return ResponseEntity.ok(vector)
        } catch (e: Exception) {
            logger.error("Error getting user vector: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/users/{userId}/profile")
    fun getUserBehaviorProfile(@PathVariable userId: Int): ResponseEntity<Any> {
        try {
            logger.info("Fetching user behavior profile: userId=$userId")
            val profile = behaviorProfiler.getUserProfile(userId)
            return ResponseEntity.ok(profile)
        } catch (e: Exception) {
            logger.error("Error getting user behavior profile: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    /**
     * Get user vector metadata
     */
    @GetMapping("/users/{userId}/metadata")
    fun getUserVectorMetadata(@PathVariable userId: Int): ResponseEntity<Any> {
        try {
            logger.info("Fetching user vector metadata: userId=$userId")
            val metadata = metadataService.getUserVectorMetadata(userId)
            if (metadata != null) {
                return ResponseEntity.ok(metadata)
            }
            return ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("Error getting user vector metadata: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    /**
     * Get post vector metadata
     */
    @GetMapping("/posts/{postId}/metadata")
    fun getPostVectorMetadata(@PathVariable postId: Int): ResponseEntity<Any> {
        try {
            logger.info("Fetching post vector metadata: postId=$postId")
            val metadata = metadataService.getPostVectorMetadata(postId)
            if (metadata != null) {
                return ResponseEntity.ok(metadata)
            }
            return ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("Error getting post vector metadata: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PostMapping("/posts/filter")
    suspend fun filterPostRecommendations(
        @RequestBody request: FilterRecommendationRequest
    ): List<ScoredPost> {
        return try {
            logger.info("Received request to filter post recommendations for user ${request.userId}")

            // Extract postIds and scores from scoredPosts
            val postIds = request.scoredPosts.map { it.postId }
            val scores = request.scoredPosts.map { it.score }

            // Call service to filter recommendations
            recommendationService.getFilteredPostRecommendations(
                userId = request.userId,
                postIds = postIds,
                scores = scores,
                limit = request.limit
            )
        } catch (e: Exception) {
            logger.error("Error filtering post recommendations: ${e.message}", e)
            throw e
        }
    }

    @PostMapping("/trailers/filter")
    suspend fun filterTrailerRecommendations(
        @RequestBody request: FilterRecommendationRequest
    ): List<ScoredPost> {
        return try {
            logger.info("Received request to filter trailer recommendations for user ${request.userId}")

            // Extract postIds and scores from scoredPosts
            val postIds = request.scoredPosts.map { it.postId }
            val scores = request.scoredPosts.map { it.score }

            // Call service to filter recommendations
            recommendationService.getFilteredTrailerRecommendations(
                userId = request.userId,
                postIds = postIds,
                scores = scores,
                limit = request.limit
            )
        } catch (e: Exception) {
            logger.error("Error filtering trailer recommendations: ${e.message}", e)
            throw e
        }
    }

}