package com.api.postgres.controllers

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

    // Endpoint to fetch post entity by postId
    @GetMapping("/{postId}")
    fun fetchPostEntityById(@PathVariable postId: Int): ResponseEntity<PostEntity?> {
        logger.info("Fetching post with postId: $postId")
        val postEntity = postsService.fetchPostEntityById(postId)
        return if (postEntity != null) {
            logger.info("Post found for postId: $postId")
            ResponseEntity.ok(postEntity)
        } else {
            logger.warn("Post not found for postId: $postId")
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
        logger.info("Adding posts with mediaType: $mediaType, providerId: $providerId")
        return runBlocking {
            postsService.addPostsToDatabase(mediaType, dataList, providerId)
            logger.info("Successfully added ${dataList.size} posts to the database for providerId: $providerId")
            ResponseEntity.ok("Posts added successfully")
        }
    }

    // Endpoint to fetch posts with pagination
    @GetMapping("/list")
    fun getPaginatedPosts(
        @RequestParam limit: Int,
        @RequestParam offset: Int
    ): ResponseEntity<List<PostEntity>> {
        logger.info("Fetching posts with pagination - limit: $limit, offset: $offset")
        return runBlocking {
            val posts = postsService.getPaginatedPostsAPI(limit, offset)
            logger.info("Fetched ${posts.size} posts with limit: $limit and offset: $offset")
            ResponseEntity.ok(posts)
        }
    }

    // Endpoint to update like count for a post
    @PutMapping("/like/{postId}")
    fun updateLikeCount(@PathVariable postId: Int): ResponseEntity<String> {
        logger.info("Updating like count for postId: $postId")
        postsService.updateLikeCount(postId)
        logger.info("Like count updated for postId: $postId")
        return ResponseEntity.ok("Like count updated successfully")
    }

    // Endpoint to update like count for trailers
    @PutMapping("/like/trailers/{postId}")
    fun updateTrailerLikeCount(@PathVariable postId: Int): ResponseEntity<String> {
        logger.info("Updating like count for trailer postId: $postId")
        postsService.updateTrailerLikeCount(postId)
        logger.info("Trailer like count updated for postId: $postId")
        return ResponseEntity.ok("Like count updated successfully")
    }

    // Endpoint to fetch videos from the database
    @GetMapping("/videos")
    fun getVideos(
        @RequestParam limit: Int,
        @RequestParam offset: Int
    ): ResponseEntity<List<Pair<String, Int>>> {
        logger.info("Fetching videos with pagination - limit: $limit, offset: $offset")
        val videos = postsService.fetchVideosFromDatabase(limit, offset)
        logger.info("Fetched ${videos.size} videos with limit: $limit and offset: $offset")
        return ResponseEntity.ok(videos)
    }

    // Endpoint to fetch postId by tmdbId
    @GetMapping("/postId")
    fun getPostIdByTmdbId(@RequestParam("tmdbId") tmdbId: Int): ResponseEntity<Int?> {
        logger.info("Fetching postId for tmdbId: $tmdbId")
        val postId = postsService.getPostIdByTmdbId(tmdbId)
        return if (postId != null) {
            logger.info("Found postId: $postId for tmdbId: $tmdbId")
            ResponseEntity.ok(postId)
        } else {
            logger.warn("PostId not found for tmdbId: $tmdbId")
            ResponseEntity.notFound().build()
        }
    }
}
