package com.api.postgres.controllers

import com.api.postgres.services.Posts
import com.api.postgres.models.Post
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/posts")
class PostsController(private val postsService: Posts) {

    // Endpoint to fetch paginated posts from the database
    @GetMapping("/paginated")
    fun getPaginatedPosts(
        @RequestParam("limit", defaultValue = "10") limit: Int,
        @RequestParam("offset", defaultValue = "0") offset: Int
    ): ResponseEntity<List<Post>> = runBlocking {
        val posts = postsService.getPaginatedPostsAPI(limit, offset)
        ResponseEntity.ok(posts)
    }

    // Endpoint to fetch posts from the external API and store them in the database
    @PostMapping("/fetch")
    fun fetchAndSavePosts(
        @RequestParam("mediaType") mediaType: String,
        @RequestParam("providerId") providerId: Int
    ): ResponseEntity<String> = runBlocking {
        val fetchedPosts = postsService.fetchPostsFromAPI(mediaType, providerId)
        if (fetchedPosts.isNotEmpty()) {
            postsService.addPostsToDatabase(mediaType, fetchedPosts, providerId)
            ResponseEntity.ok("Posts successfully fetched and saved.")
        } else {
            ResponseEntity.status(500).body("Failed to fetch posts.")
        }
    }

    // Endpoint to update the like count of a post
    @PutMapping("/like/{postId}")
    fun updateLikeCount(@PathVariable postId: Int): ResponseEntity<String> {
        return try {
            postsService.updateLikeCount(postId)
            ResponseEntity.ok("Like count updated successfully.")
        } catch (e: Exception) {
            ResponseEntity.status(404).body("Post not found.")
        }
    }

    // Endpoint to fetch the best video key for a specific post based on priority
    @GetMapping("/{postId}/best-video")
    fun getBestVideoKey(@PathVariable postId: Int): ResponseEntity<String?> = runBlocking {
        val videos = postsService.fetchVideosFromDatabase(1, 0) // Fetching videos for the post
        val bestVideoKey = postsService.selectBestVideoKey(videos.map { Video(it.first, true, "Trailer", "2022-01-01") }) // Dummy example for now
        ResponseEntity.ok(bestVideoKey)
    }
}
