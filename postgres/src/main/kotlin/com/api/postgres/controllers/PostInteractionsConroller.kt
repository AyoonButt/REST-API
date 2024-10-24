package com.api.postgres.controllers

import com.api.postgres.models.UserPostInteraction
import com.api.postgres.services.PostInteractions
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/interactions")
class PostInteractionsController(
    private val postInteractionsService: PostInteractions
) {

    // Endpoint to save interaction data for a post
    @PostMapping("/save")
    fun saveInteractionData(@RequestBody interactionData: UserPostInteraction): ResponseEntity<String> {
        postInteractionsService.saveInteractionData(interactionData)
        return ResponseEntity.status(HttpStatus.CREATED).body("Interaction data saved successfully")
    }

    // Endpoint to update the interaction timestamp for a post
    @PutMapping("/updateTimestamp")
    fun updateInteractionTimestamp(
        @RequestParam postId: Int,
        @RequestParam userId: Int,
        @RequestParam timestamp: Long,
    ): ResponseEntity<String> {
        postInteractionsService.updateInteractionTimestamp(postId, userId, timestamp)
        return ResponseEntity.ok("Timestamp updated successfully for postId: $postId")
    }

    // Endpoint to get all post interactions for a user
    @GetMapping("/user/{userId}")
    fun getPostInteractionsByUser(@PathVariable userId: Int): ResponseEntity<List<UserPostInteraction>> {
        val interactions = postInteractionsService.getPostInteractionsByUser(userId)
        return if (interactions.isNotEmpty()) {
            ResponseEntity.ok(interactions)
        } else {
            ResponseEntity.status(HttpStatus.NO_CONTENT).build()
        }
    }

    // Endpoint to get a single post interaction for a user
    @GetMapping("/user/{userId}/post/{postId}")
    fun getPostInteraction(
        @PathVariable userId: Int,
        @PathVariable postId: Int
    ): ResponseEntity<UserPostInteraction> {
        val interaction = postInteractionsService.getPostInteraction(userId, postId)
        return interaction?.let {
            ResponseEntity.ok(it)
        } ?: ResponseEntity.status(HttpStatus.NOT_FOUND).build()
    }

    // Endpoint to get all liked posts for a user
    @GetMapping("/liked/user/{userId}")
    fun getLikedPosts(@PathVariable userId: Int): ResponseEntity<List<Int>> {
        val likedPosts = postInteractionsService.getLikedPosts(userId)
        return if (likedPosts.isNotEmpty()) {
            ResponseEntity.ok(likedPosts)
        } else {
            ResponseEntity.status(HttpStatus.NO_CONTENT).build()
        }
    }

    // Endpoint to get all saved posts for a user
    @GetMapping("/saved/user/{userId}")
    fun getSavedPosts(@PathVariable userId: Int): ResponseEntity<List<Int>> {
        val savedPosts = postInteractionsService.getSavedPosts(userId)
        return if (savedPosts.isNotEmpty()) {
            ResponseEntity.ok(savedPosts)
        } else {
            ResponseEntity.status(HttpStatus.NO_CONTENT).build()
        }
    }

    // Endpoint to get all posts where a comment was made by a user
    @GetMapping("/commented/user/{userId}")
    fun getCommentMadePosts(@PathVariable userId: Int): ResponseEntity<List<Int>> {
        val commentPosts = postInteractionsService.getCommentMadePosts(userId)
        return if (commentPosts.isNotEmpty()) {
            ResponseEntity.ok(commentPosts)
        } else {
            ResponseEntity.status(HttpStatus.NO_CONTENT).build()
        }
    }
}
