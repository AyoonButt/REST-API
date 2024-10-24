package com.api.postgres.controllers


import com.api.postgres.models.PostEntity
import com.api.postgres.models.UserTrailerInteraction
import com.api.postgres.services.TrailerInteractions
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/trailer-interactions")
class TrailerInteractionsController(private val trailerInteractions: TrailerInteractions) {

    // Endpoint to update interaction timestamp for a specific postId
    @PutMapping("/{postId}/timestamp")
    fun updateInteractionTimestamp(
        @PathVariable postId: Int,
        @RequestParam timestamp: Long
    ): ResponseEntity<String> = runBlocking {
        trailerInteractions.updateInteractionTimestamp(postId, timestamp)
        ResponseEntity.ok("Timestamp updated successfully for postId: $postId")
    }

    // Endpoint to save new interaction data
    @PostMapping("/save")
    fun saveInteractionData(
        @RequestBody interactionData: UserTrailerInteraction
    ): ResponseEntity<String> = runBlocking {
        trailerInteractions.saveInteractionData(interactionData)
        ResponseEntity.ok("Interaction data saved successfully.")
    }

    // Endpoint to get trailer interactions by a specific user
    @GetMapping("/user/{userId}")
    fun getTrailerInteractionsByUser(@PathVariable userId: Int): ResponseEntity<List<UserTrailerInteraction>> = runBlocking {
        val interactions = trailerInteractions.getTrailerInteractionsByUser(userId)
        ResponseEntity.ok(interactions)
    }

    // Endpoint to get a specific trailer interaction by userId and postId
    @GetMapping("/user/{userId}/post/{postId}")
    fun getTrailerInteraction(
        @PathVariable userId: Int,
        @PathVariable postId: Int
    ): ResponseEntity<UserTrailerInteraction?> = runBlocking {
        val interaction = trailerInteractions.getTrailerInteraction(userId, postId)
        ResponseEntity.ok(interaction)
    }

    // Endpoint to get liked trailers by userId
    @GetMapping("/user/{userId}/liked")
    fun getLikedTrailers(@PathVariable userId: Int): ResponseEntity<List<PostEntity>> = runBlocking {
        val likedTrailers = trailerInteractions.getLikedTrailers(userId)
        ResponseEntity.ok(likedTrailers)
    }

    // Endpoint to get saved trailers by userId
    @GetMapping("/user/{userId}/saved")
    fun getSavedTrailers(@PathVariable userId: Int): ResponseEntity<List<PostEntity>> = runBlocking {
        val savedTrailers = trailerInteractions.getSavedTrailers(userId)
        ResponseEntity.ok(savedTrailers)
    }

    // Endpoint to get trailers where comments were made by userId
    @GetMapping("/user/{userId}/commented")
    fun getCommentMadeTrailers(@PathVariable userId: Int): ResponseEntity<List<PostEntity>> = runBlocking {
        val commentedTrailers = trailerInteractions.getCommentMadeTrailers(userId)
        ResponseEntity.ok(commentedTrailers)
    }
}
