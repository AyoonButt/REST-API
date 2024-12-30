package com.api.postgres.controllers

import com.api.postgres.PostDto
import com.api.postgres.models.PostEntity
import com.api.postgres.services.Posts
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
@RestController
@RequestMapping("/api/posts")
class PostsController(
    private val postsService: Posts
) {
    private val logger: Logger = LoggerFactory.getLogger(PostsController::class.java)

    @PostMapping("/batch/{mediaType}/{providerId}")
    suspend fun addPosts(
        @PathVariable mediaType: String,
        @PathVariable providerId: Int,
        @RequestBody posts: List<PostDto>
    ): ResponseEntity<String> {
        logger.info("Adding ${posts.size} $mediaType posts for provider $providerId")
        return try {
            postsService.addPostsToDatabase(mediaType, posts, providerId)
            ResponseEntity.status(HttpStatus.CREATED)
                .body("Successfully added ${posts.size} posts")
        } catch (e: Exception) {
            logger.error("Error adding posts: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to add posts: ${e.message}")
        }
    }

    @GetMapping
    suspend fun getPosts(
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): ResponseEntity<List<PostDto>> {
        logger.info("Fetching posts with limit: $limit, offset: $offset")
        val posts = postsService.fetchPostsFromDatabase(limit, offset)
        return if (posts.isNotEmpty()) {
            ResponseEntity.ok(posts)
        } else {
            ResponseEntity.status(HttpStatus.NO_CONTENT).build()
        }
    }

    @PutMapping("/{postId}/like")
    suspend fun incrementLikes(@PathVariable postId: Int): ResponseEntity<String> {
        logger.info("Incrementing likes for post: $postId")
        return try {
            postsService.updateLikeCount(postId)
            ResponseEntity.ok("Like count updated successfully")
        } catch (e: Exception) {
            logger.error("Error updating like count: ${e.message}", e)
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Failed to update like count: ${e.message}")
        }
    }

    @PutMapping("/{postId}/trailer-like")
    suspend fun incrementTrailerLikes(@PathVariable postId: Int): ResponseEntity<String> {
        logger.info("Incrementing trailer likes for post: $postId")
        return try {
            postsService.updateTrailerLikeCount(postId)
            ResponseEntity.ok("Trailer like count updated successfully")
        } catch (e: Exception) {
            logger.error("Error updating trailer like count: ${e.message}", e)
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Failed to update trailer like count: ${e.message}")
        }
    }

    @GetMapping("/videos")
    suspend fun getVideos(
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): ResponseEntity<List<Map<String, Any>>> {
        logger.info("Fetching videos with limit: $limit, offset: $offset")
        val videos = postsService.fetchVideosFromDatabase(limit, offset)
            .map { (key, id) -> mapOf("videoKey" to key, "postId" to id) }
        return if (videos.isNotEmpty()) {
            ResponseEntity.ok(videos)
        } else {
            ResponseEntity.status(HttpStatus.NO_CONTENT).build()
        }
    }

    @GetMapping("/{postId}")
    suspend fun getPost(@PathVariable postId: Int): ResponseEntity<PostDto> {
        logger.info("Fetching post: $postId")
        val post = postsService.getPostById(postId)
        return post?.let {
            ResponseEntity.ok(it)
        } ?: ResponseEntity.status(HttpStatus.NOT_FOUND).build()
    }

    @GetMapping("/tmdb/{tmdbId}")
    suspend fun getPostIdByTmdbId(@PathVariable tmdbId: Int): ResponseEntity<Map<String, Int>> {
        logger.info("Fetching post ID for TMDB ID: $tmdbId")
        val postId = postsService.getPostIdByTmdbId(tmdbId)
        return postId?.let {
            ResponseEntity.ok(mapOf("postId" to it))
        } ?: ResponseEntity.status(HttpStatus.NOT_FOUND).build()
    }
}