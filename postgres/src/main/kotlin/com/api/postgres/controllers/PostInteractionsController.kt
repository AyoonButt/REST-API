package com.api.postgres.controllers

import com.api.postgres.ApiResponse
import com.api.postgres.InteractionStates
import com.api.postgres.UserPostInteractionDto
import com.api.postgres.services.PostInteractions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/interactions")
class PostInteractionsController(
    private val postInteractionsService: PostInteractions
) {
    private val logger: Logger = LoggerFactory.getLogger(PostInteractionsController::class.java)

    // Endpoint to save interaction data for a post
    @PostMapping("/save")
    suspend fun saveInteractionData(@RequestBody interactionData: UserPostInteractionDto): ResponseEntity<ApiResponse> {
        logger.info("Saving interaction data for postId: ${interactionData.postId} and userId: ${interactionData.userId}")
        return try {
            postInteractionsService.saveInteractionData(interactionData)
            logger.info("Interaction data saved successfully for postId: ${interactionData.postId} and userId: ${interactionData.userId}")
            ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse(success = true, message = "Interaction data saved successfully"))
        } catch (e: Exception) {
            logger.error("Failed to save interaction data: ${e.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse(success = false, message = "Failed to save interaction data"))
        }
    }


    // Endpoint to get all post interactions for a user
    @GetMapping("/user/{userId}")
    suspend fun getPostInteractionsByUser(@PathVariable userId: Int): ResponseEntity<List<UserPostInteractionDto>> {
        logger.info("Fetching post interactions for userId: $userId")
        val interactions = postInteractionsService.getPostInteractionsByUser(userId)
        return if (interactions.isNotEmpty()) {
            logger.info("Found ${interactions.size} interactions for userId: $userId")
            ResponseEntity.ok(interactions)
        } else {
            logger.warn("No interactions found for userId: $userId")
            ResponseEntity.status(HttpStatus.NO_CONTENT).build()
        }
    }

    // Endpoint to get a single post interaction for a user
    @GetMapping("/user/{userId}/post/{postId}")
    suspend fun getPostInteraction(
        @PathVariable userId: Int,
        @PathVariable postId: Int
    ): ResponseEntity<UserPostInteractionDto> {
        logger.info("Fetching interaction for userId: $userId and postId: $postId")
        val interaction = postInteractionsService.getPostInteraction(userId, postId)
        return interaction?.let {
            logger.info("Found interaction for userId: $userId and postId: $postId")
            ResponseEntity.ok(it)
        } ?: run {
            logger.warn("No interaction found for userId: $userId and postId: $postId")
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }

    // Endpoint to get all liked posts for a user
    @GetMapping("/liked/user/{userId}")
    suspend fun getLikedPosts(@PathVariable userId: Int): ResponseEntity<List<Int>> {
        logger.info("Fetching liked posts for userId: $userId")
        val likedPosts = postInteractionsService.getLikedPosts(userId)
        return if (likedPosts.isNotEmpty()) {
            logger.info("Found ${likedPosts.size} liked posts for userId: $userId")
            ResponseEntity.ok(likedPosts)
        } else {
            logger.warn("No liked posts found for userId: $userId")
            ResponseEntity.status(HttpStatus.NO_CONTENT).build()
        }
    }

    // Endpoint to get all saved posts for a user
    @GetMapping("/saved/user/{userId}")
    suspend fun getSavedPosts(@PathVariable userId: Int): ResponseEntity<List<Int>> {
        logger.info("Fetching saved posts for userId: $userId")
        val savedPosts = postInteractionsService.getSavedPosts(userId)
        return if (savedPosts.isNotEmpty()) {
            logger.info("Found ${savedPosts.size} saved posts for userId: $userId")
            ResponseEntity.ok(savedPosts)
        } else {
            logger.warn("No saved posts found for userId: $userId")
            ResponseEntity.status(HttpStatus.NO_CONTENT).build()
        }
    }


    @GetMapping("/{userId}/{postId}/states")
    suspend fun getInteractionStates(
        @PathVariable userId: Int,
        @PathVariable postId: Int
    ): ResponseEntity<InteractionStates> {
        return try {
            val states = postInteractionsService.getPostInteractionStates(userId, postId)
            ResponseEntity.ok(states)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/user/{userId}/recent-views")
    suspend fun getRecentlyViewedPosts(
        @PathVariable userId: Int,
        @RequestParam(required = false, defaultValue = "100") limit: Int
    ): ResponseEntity<List<Int>> {
        logger.info("Fetching $limit most recently viewed posts for userId: $userId")
        try {
            val recentPosts = postInteractionsService.getRecentlyViewedPostIds(userId, limit)

            return if (recentPosts.isNotEmpty()) {
                logger.info("Found ${recentPosts.size} recently viewed posts for userId: $userId")
                ResponseEntity.ok(recentPosts)
            } else {
                logger.info("No recently viewed posts found for userId: $userId")
                ResponseEntity.ok(emptyList())
            }
        } catch (e: Exception) {
            logger.error("Error fetching recently viewed posts for userId: $userId", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

}
