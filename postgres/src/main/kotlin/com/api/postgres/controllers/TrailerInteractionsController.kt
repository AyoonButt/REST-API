package com.api.postgres.controllers

import com.api.postgres.models.PostEntity
import com.api.postgres.models.UserTrailerInteraction
import com.api.postgres.services.TrailerInteractions
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/trailer-interactions")
class TrailerInteractionsController(private val trailerInteractions: TrailerInteractions) {

    private val logger: Logger = LoggerFactory.getLogger(TrailerInteractionsController::class.java)

    // Endpoint to update interaction timestamp for a specific postId
    @PutMapping("/{postId}/timestamp")
    fun updateInteractionTimestamp(
        @PathVariable postId: Int,
        @RequestParam timestamp: String
    ): ResponseEntity<String> = runBlocking {
        logger.info("Updating interaction timestamp for postId: $postId with timestamp: $timestamp")
        trailerInteractions.updateInteractionTimestamp(postId, timestamp)
        logger.info("Timestamp updated successfully for postId: $postId")
        ResponseEntity.ok("Timestamp updated successfully for postId: $postId")
    }

    // Endpoint to save new interaction data
    @PostMapping("/save")
    fun saveInteractionData(
        @RequestBody interactionData: UserTrailerInteraction
    ): ResponseEntity<String> = runBlocking {
        logger.info("Saving interaction data for userId: ${interactionData.user}, postId: ${interactionData.post}")
        trailerInteractions.saveInteractionData(interactionData)
        logger.info("Interaction data saved successfully for userId: ${interactionData.user}, postId: ${interactionData.post}")
        ResponseEntity.ok("Interaction data saved successfully.")
    }

    // Endpoint to get trailer interactions by a specific user
    @GetMapping("/user/{userId}")
    fun getTrailerInteractionsByUser(@PathVariable userId: Int): ResponseEntity<List<UserTrailerInteraction>> = runBlocking {
        logger.info("Fetching trailer interactions for userId: $userId")
        val interactions = trailerInteractions.getTrailerInteractionsByUser(userId)
        logger.info("Found ${interactions.size} interactions for userId: $userId")
        ResponseEntity.ok(interactions)
    }

    // Endpoint to get a specific trailer interaction by userId and postId
    @GetMapping("/user/{userId}/post/{postId}")
    fun getTrailerInteraction(
        @PathVariable userId: Int,
        @PathVariable postId: Int
    ): ResponseEntity<UserTrailerInteraction?> = runBlocking {
        logger.info("Fetching trailer interaction for userId: $userId and postId: $postId")
        val interaction = trailerInteractions.getTrailerInteraction(userId, postId)
        return@runBlocking if (interaction != null) {
            logger.info("Found interaction for userId: $userId and postId: $postId")
            ResponseEntity.ok(interaction)
        } else {
            logger.warn("No interaction found for userId: $userId and postId: $postId")
            ResponseEntity.notFound().build()
        }
    }

    // Endpoint to get liked trailers by userId
    @GetMapping("/user/{userId}/liked")
    fun getLikedTrailers(@PathVariable userId: Int): ResponseEntity<List<PostEntity>> = runBlocking {
        logger.info("Fetching liked trailers for userId: $userId")
        val likedTrailers = trailerInteractions.getLikedTrailers(userId)
        logger.info("Found ${likedTrailers.size} liked trailers for userId: $userId")
        ResponseEntity.ok(likedTrailers)
    }

    // Endpoint to get saved trailers by userId
    @GetMapping("/user/{userId}/saved")
    fun getSavedTrailers(@PathVariable userId: Int): ResponseEntity<List<PostEntity>> = runBlocking {
        logger.info("Fetching saved trailers for userId: $userId")
        val savedTrailers = trailerInteractions.getSavedTrailers(userId)
        logger.info("Found ${savedTrailers.size} saved trailers for userId: $userId")
        ResponseEntity.ok(savedTrailers)
    }

    // Endpoint to get trailers where comments were made by userId
    @GetMapping("/user/{userId}/commented")
    fun getCommentMadeTrailers(@PathVariable userId: Int): ResponseEntity<List<PostEntity>> = runBlocking {
        logger.info("Fetching trailers where comments were made by userId: $userId")
        val commentedTrailers = trailerInteractions.getCommentMadeTrailers(userId)
        logger.info("Found ${commentedTrailers.size} trailers where userId: $userId commented")
        ResponseEntity.ok(commentedTrailers)
    }
}
