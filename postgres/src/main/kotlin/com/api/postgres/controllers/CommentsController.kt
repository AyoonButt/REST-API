package com.api.postgres.controllers

import com.api.postgres.ReplyRequest
import com.api.postgres.models.CommentEntity
import com.api.postgres.services.Comments
import com.api.postgres.services.UsersService
import com.api.postgres.services.Posts
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/comments")
class CommentsController(
    private val commentsService: Comments,
    private val usersService: UsersService,    // Inject UsersService
    private val postsService: Posts      // Inject PostsService
) {
    private val logger: Logger = LoggerFactory.getLogger(CommentsController::class.java)

    // Endpoint to load comments for a specific post by postId
    @GetMapping("/post/{postId}")
    fun getCommentsByPost(@PathVariable postId: Int): ResponseEntity<List<CommentEntity>> {
        logger.info("Fetching comments for postId: $postId")
        return runBlocking {
            val comments = commentsService.loadComments(postId)
            return@runBlocking if (comments.isNotEmpty()) {
                logger.info("Found ${comments.size} comments for postId: $postId")
                ResponseEntity.ok(comments)
            } else {
                logger.warn("No comments found for postId: $postId")
                ResponseEntity.status(HttpStatus.NO_CONTENT).build()
            }
        }
    }

    // Endpoint to insert a new comment
    @PostMapping
    fun addComment(@RequestBody newComment: CommentEntity): ResponseEntity<String> {
        logger.info("Received request to add a new comment: $newComment")
        return runBlocking {
            try {
                commentsService.insertComment(newComment)
                logger.info("Comment added successfully: $newComment")
                ResponseEntity.status(HttpStatus.CREATED).body("Comment added successfully")
            } catch (ex: Exception) {
                logger.error("Error adding comment: ${ex.message}", ex)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add comment")
            }
        }
    }

    @PostMapping("/{userId}/comments/{commentId}/replies")
    fun addReply(
        @PathVariable userId: Int,
        @PathVariable commentId: Int,
        @RequestBody replyRequest: ReplyRequest
    ): ResponseEntity<String> {
        logger.info("Received reply request for commentId: $commentId by userId: $userId")
        return try {
            // Fetch the user and post entities
            val user = usersService.getUserById(userId) ?: throw Exception("User not found")
            val post = postsService.getPostById(replyRequest.postId) ?: throw Exception("Post not found")

            // Create a reply CommentEntity with the parent comment and necessary associations
            val reply = CommentEntity(
                user = user,
                post = post,
                content = replyRequest.content,
                sentiment = replyRequest.sentiment,
                parentComment = commentsService.getCommentById(commentId)  // Fetch parent comment
            )

            commentsService.addReplyToComment(commentId, reply)
            logger.info("Reply added successfully for commentId: $commentId")
            ResponseEntity.ok("Reply added successfully")
        } catch (e: Exception) {
            logger.error("Error adding reply for commentId: $commentId by userId: $userId: ${e.message}", e)
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }
    }

    @GetMapping("/{commentId}/replies")
    fun getReplies(
        @PathVariable commentId: Int,
        @RequestParam userId: Int,
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): ResponseEntity<List<CommentEntity>> {
        logger.info("Fetching replies for commentId: $commentId, userId: $userId, limit: $limit, offset: $offset")
        val replies = commentsService.findReplies(commentId, userId, limit, offset)
        logger.info("Found ${replies.size} replies for commentId: $commentId")
        return ResponseEntity.ok(replies)
    }

    @GetMapping("/{commentId}/all-replies")
    fun getAllReplies(
        @PathVariable commentId: Int,
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): ResponseEntity<List<CommentEntity>> {
        logger.info("Fetching all replies for commentId: $commentId with limit: $limit and offset: $offset")
        val replies = commentsService.findAllReplies(commentId, limit, offset)
        logger.info("Found ${replies.size} all-replies for commentId: $commentId")
        return ResponseEntity.ok(replies)
    }
}
