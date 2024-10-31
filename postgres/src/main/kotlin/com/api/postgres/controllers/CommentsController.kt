package com.api.postgres.controllers

import com.api.postgres.ReplyRequest
import com.api.postgres.models.CommentEntity
import com.api.postgres.services.Comments
import com.api.postgres.services.UsersService
import com.api.postgres.services.Posts
import kotlinx.coroutines.runBlocking
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

    // Endpoint to load comments for a specific post by postId
    @GetMapping("/post/{postId}")
    fun getCommentsByPost(@PathVariable postId: Int): ResponseEntity<List<CommentEntity>> {
        return runBlocking {
            val comments = commentsService.loadComments(postId)
            if (comments.isNotEmpty()) {
                ResponseEntity.ok(comments)
            } else {
                ResponseEntity.status(HttpStatus.NO_CONTENT).build()
            }
        }
    }

    // Endpoint to insert a new comment
    @PostMapping
    fun addComment(@RequestBody newComment: CommentEntity): ResponseEntity<String> {
        return runBlocking {
            commentsService.insertComment(newComment)
            ResponseEntity.status(HttpStatus.CREATED).body("Comment added successfully")
        }
    }

    @PostMapping("/{userId}/comments/{commentId}/replies")
    fun addReply(
        @PathVariable userId: Int,
        @PathVariable commentId: Int,
        @RequestBody replyRequest: ReplyRequest
    ): ResponseEntity<String> {
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
            ResponseEntity.ok("Reply added successfully")
        } catch (e: Exception) {
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
        val replies = commentsService.findReplies(commentId, userId, limit, offset)
        return ResponseEntity.ok(replies)
    }

    @GetMapping("/{commentId}/all-replies")
    fun getAllReplies(
        @PathVariable commentId: Int,
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): ResponseEntity<List<CommentEntity>> {
        val replies = commentsService.findAllReplies(commentId, limit, offset)
        return ResponseEntity.ok(replies)
    }
}
