package com.api.postgres.controllers

import com.api.postgres.models.UserPostInteraction
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
    fun saveInteractionData(@RequestBody interactionData: UserPostInteraction): ResponseEntity<String> {
        logger.info("Saving interaction data for postId: ${interactionData.post} and userId: ${interactionData.user}")
        postInteractionsService.saveInteractionData(interactionData)
        logger.info("Interaction data saved successfully for postId: ${interactionData.post} and userId: ${interactionData.user}")
        return ResponseEntity.status(HttpStatus.CREATED).body("Interaction data saved successfully")
    }

    // Endpoint to update the interaction timestamp for a post
    @PutMapping("/updateTimestamp")
    fun updateInteractionTimestamp(
        @RequestParam postId: Int,
        @RequestParam userId: Int,
        @RequestParam timestamp: Long,
    ): ResponseEntity<String> {
        logger.info("Updating interaction timestamp for postId: $postId, userId: $userId with timestamp: $timestamp")
        postInteractionsService.updateInteractionTimestamp(postId, userId, timestamp)
        logger.info("Timestamp updated successfully for postId: $postId, userId: $userId")
        return ResponseEntity.ok("Timestamp updated successfully for postId: $postId")
    }

    // Endpoint to get all post interactions for a user
    @GetMapping("/user/{userId}")
    fun getPostInteractionsByUser(@PathVariable userId: Int): ResponseEntity<List<UserPostInteraction>> {
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
    fun getPostInteraction(
        @PathVariable userId: Int,
        @PathVariable postId: Int
    ): ResponseEntity<UserPostInteraction> {
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
    fun getLikedPosts(@PathVariable userId: Int): ResponseEntity<List<Int>> {
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
    fun getSavedPosts(@PathVariable userId: Int): ResponseEntity<List<Int>> {
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

    // Endpoint to get all posts where a comment was made by a user
    @GetMapping("/commented/user/{userId}")
    fun getCommentMadePosts(@PathVariable userId: Int): ResponseEntity<List<Int>> {
        logger.info("Fetching posts with comments made by userId: $userId")
        val commentPosts = postInteractionsService.getCommentMadePosts(userId)
        return if (commentPosts.isNotEmpty()) {
            logger.info("Found ${commentPosts.size} posts with comments made by userId: $userId")
            ResponseEntity.ok(commentPosts)
        } else {
            logger.warn("No posts with comments found for userId: $userId")
            ResponseEntity.status(HttpStatus.NO_CONTENT).build()
        }
    }
}
