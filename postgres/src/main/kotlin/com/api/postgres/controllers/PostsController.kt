package com.api.postgres.controllers

import com.api.postgres.models.PostEntity
import com.api.postgres.services.Posts
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import kotlinx.coroutines.runBlocking

@RestController
@RequestMapping("/api/posts")
class PostsController(
    private val postsService: Posts
) {

    // Endpoint to add posts to the database
    @PostMapping("/add")
    fun addPosts(
        @RequestParam mediaType: String,
        @RequestParam providerId: Int,
        @RequestBody dataList: List<PostEntity>
    ): ResponseEntity<String> {
        return runBlocking {
            postsService.addPostsToDatabase(mediaType, dataList, providerId)
            ResponseEntity.ok("Posts added successfully")
        }
    }

    // Endpoint to fetch posts with pagination
    @GetMapping("/list")
    fun getPaginatedPosts(
        @RequestParam limit: Int,
        @RequestParam offset: Int
    ): ResponseEntity<List<PostEntity>> {
        return runBlocking {
            val posts = postsService.getPaginatedPostsAPI(limit, offset)
            ResponseEntity.ok(posts)
        }
    }

    // Endpoint to update like count for a post
    @PutMapping("/like/{postId}")
    fun updateLikeCount(@PathVariable postId: Int): ResponseEntity<String> {
        postsService.updateLikeCount(postId)
        return ResponseEntity.ok("Like count updated successfully")
    }

    // Endpoint to fetch videos from the database
    @GetMapping("/videos")
    fun getVideos(
        @RequestParam limit: Int,
        @RequestParam offset: Int
    ): ResponseEntity<List<Pair<String, Int>>> {
        val videos = postsService.fetchVideosFromDatabase(limit, offset)
        return ResponseEntity.ok(videos)
    }
}
