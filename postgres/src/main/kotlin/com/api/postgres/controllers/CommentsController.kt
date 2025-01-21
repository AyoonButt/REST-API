package com.api.postgres.controllers

import com.api.postgres.ApiResponse
import com.api.postgres.CommentDto
import com.api.postgres.CommentResponse
import com.api.postgres.ReplyCountDto
import com.api.postgres.services.Comments
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
@RestController
@RequestMapping("/api/comments")
class CommentsController(
    private val commentsService: Comments,
) {
    private val logger: Logger = LoggerFactory.getLogger(CommentsController::class.java)

    @GetMapping("/post/{postId}")
    suspend fun getCommentsByPost(@PathVariable postId: Int): ResponseEntity<List<CommentDto>> {
        logger.info("Fetching comments for postId: $postId")
        val comments = commentsService.loadComments(postId)
        return if (comments.isNotEmpty()) {
            logger.info("Found ${comments.size} comments for postId: $postId")
            ResponseEntity.ok(comments)
        } else {
            logger.warn("No comments found for postId: $postId")
            ResponseEntity.status(HttpStatus.NO_CONTENT).build()
        }
    }

    @PostMapping("/insert")
    suspend fun addComment(@RequestBody newComment: CommentDto): ResponseEntity<CommentResponse> {
        logger.info("Received request to add a new comment: $newComment")
        return try {
            val commentId = commentsService.insertComment(newComment)
            logger.info("Comment added successfully with ID: $commentId")
            ResponseEntity.status(HttpStatus.CREATED)
                .body(
                    CommentResponse(
                        success = true,
                        message = "Comment added successfully",
                        commentId = commentId // Include the generated commentId
                    )
                )
        } catch (ex: Exception) {
            logger.error("Error adding comment: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                    CommentResponse(
                        success = false,
                        message = "Failed to add comment",
                        commentId = 0

                    )
                )
        }
    }




    @GetMapping("/{commentId}/replies")
    suspend fun getReplies(
        @PathVariable commentId: Int,
        @RequestParam userId: Int,
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): ResponseEntity<List<CommentDto>> {
        logger.info("Fetching replies for commentId: $commentId, userId: $userId, limit: $limit, offset: $offset")
        val replies = commentsService.findReplies(commentId, userId, limit, offset)
        logger.info("Found ${replies.size} replies for commentId: $commentId")
        return ResponseEntity.ok(replies)
    }

    @GetMapping("/{commentId}/all-replies")
    suspend fun getAllReplies(
        @PathVariable commentId: Int,
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): ResponseEntity<List<CommentDto>> {
        logger.info("Fetching all replies for commentId: $commentId with limit: $limit and offset: $offset")
        val replies = commentsService.findAllReplies(commentId, limit, offset)
        logger.info("Found ${replies.size} all-replies for commentId: $commentId")
        return ResponseEntity.ok(replies)
    }

    @GetMapping("/{commentId}/parent-username")
    suspend fun getParentCommentUsername(@PathVariable commentId: Int): ResponseEntity<ApiResponse> {
        logger.info("Fetching parent comment username for commentId: $commentId")

        return try {
            val username = commentsService.getParentCommentUsername(commentId)
                ?: throw Exception("Parent comment or username not found")

            logger.info("Found parent username: $username for commentId: $commentId")
            ResponseEntity.ok(ApiResponse(
                success = true,
                message = username
            ))
        } catch (e: Exception) {
            logger.error("Error fetching parent username for commentId: $commentId: ${e.message}", e)
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse(
                    success = false,
                    message = e.message ?: "Unknown error"
                ))
        }
    }

    @GetMapping("/reply-counts")
    suspend fun getReplyCountsForComments(
        @RequestParam parentIds: List<Int>
    ): ResponseEntity<List<ReplyCountDto>> {
        logger.info("Getting reply counts for parent IDs: $parentIds")

        return try {
            val replyCounts = commentsService.getReplyCountsForComments(parentIds)
            logger.info("Successfully retrieved ${replyCounts.size} reply counts")
            ResponseEntity.ok(replyCounts)
        } catch (e: Exception) {
            logger.error("Error getting reply counts for parent IDs: $parentIds", e)
            throw e
        }
    }

}



