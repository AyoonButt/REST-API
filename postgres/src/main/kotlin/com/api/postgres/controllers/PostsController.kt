package com.api.postgres.controllers

import com.api.postgres.models.PostEntity
import com.api.postgres.services.Posts
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus

@RestController
@RequestMapping("/api/posts")
class PostsController(
    private val postsService: Posts
) {

    @GetMapping("/{postId}")
    fun fetchPostEntityById(@PathVariable postId: Int): ResponseEntity<PostEntity?> {
        val postEntity = postsService.fetchPostEntityById(postId)
        return if (postEntity != null) {
            ResponseEntity.ok(postEntity)
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
        }
    }

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


    @PutMapping("/like/trailers/{postId}")
    fun updateTrailerLikeCount(@PathVariable postId: Int): ResponseEntity<String> {
        postsService.updateTrailerLikeCount(postId)
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

    @GetMapping("/postId")
    fun getPostIdByTmdbId(@RequestParam("tmdbId") tmdbId: Int): ResponseEntity<Int?> {
        val postId = postsService.getPostIdByTmdbId(tmdbId)
        return (if (postId != null) {
            ResponseEntity.ok(postId)
        } else {
            ResponseEntity.notFound().build()
        })
    }


}
