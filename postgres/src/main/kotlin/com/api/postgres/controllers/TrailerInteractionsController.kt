package com.api.postgres.controllers

import com.api.postgres.TrailerInteractionDto
import com.api.postgres.services.TrailerInteractions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/trailer-interactions")
class TrailerInteractionsController(private val trailerInteractions: TrailerInteractions) {
    private val logger: Logger = LoggerFactory.getLogger(TrailerInteractionsController::class.java)

    @PutMapping("/{postId}/timestamp")
    suspend fun updateInteractionTimestamp(
        @PathVariable postId: Int,
        @RequestParam timestamp: String
    ): ResponseEntity<String> {
        logger.info("Updating interaction timestamp for postId: $postId")
        trailerInteractions.updateInteractionTimestamp(postId, timestamp)
        return ResponseEntity.ok("Timestamp updated successfully")
    }

    @PostMapping("/save")
    suspend fun saveInteractionData(
        @RequestBody interactionData: TrailerInteractionDto
    ): ResponseEntity<String> {
        logger.info("Saving interaction data for userId: ${interactionData.userId}")
        trailerInteractions.saveInteractionData(interactionData)
        return ResponseEntity.ok("Interaction saved successfully")
    }

    @GetMapping("/user/{userId}")
    suspend fun getTrailerInteractionsByUser(
        @PathVariable userId: Int
    ): ResponseEntity<List<TrailerInteractionDto>> {
        logger.info("Fetching trailer interactions for userId: $userId")
        val interactions = trailerInteractions.getTrailerInteractionsByUser(userId)
        return if (interactions.isNotEmpty()) {
            ResponseEntity.ok(interactions)
        } else {
            ResponseEntity.noContent().build()
        }
    }

    @GetMapping("/user/{userId}/post/{postId}")
    suspend fun getTrailerInteraction(
        @PathVariable userId: Int,
        @PathVariable postId: Int
    ): ResponseEntity<TrailerInteractionDto> {
        logger.info("Fetching interaction for userId: $userId, postId: $postId")
        return trailerInteractions.getTrailerInteraction(userId, postId)?.let {
            ResponseEntity.ok(it)
        } ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/user/{userId}/liked")
    suspend fun getLikedTrailers(
        @PathVariable userId: Int
    ): ResponseEntity<List<Int>> {
        val likedTrailers = trailerInteractions.getLikedTrailers(userId)
        return if (likedTrailers.isNotEmpty()) {
            ResponseEntity.ok(likedTrailers)
        } else {
            ResponseEntity.noContent().build()
        }
    }

    @GetMapping("/user/{userId}/saved")
    suspend fun getSavedTrailers(
        @PathVariable userId: Int
    ): ResponseEntity<List<Int>> {
        val savedTrailers = trailerInteractions.getSavedTrailers(userId)
        return if (savedTrailers.isNotEmpty()) {
            ResponseEntity.ok(savedTrailers)
        } else {
            ResponseEntity.noContent().build()
        }
    }

    @GetMapping("/user/{userId}/commented")
    suspend fun getCommentMadeTrailers(
        @PathVariable userId: Int
    ): ResponseEntity<List<Int>> {
        val commentedTrailers = trailerInteractions.getCommentMadeTrailers(userId)
        return if (commentedTrailers.isNotEmpty()) {
            ResponseEntity.ok(commentedTrailers)
        } else {
            ResponseEntity.noContent().build()
        }
    }
}