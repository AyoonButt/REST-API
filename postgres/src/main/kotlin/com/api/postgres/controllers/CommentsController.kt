package com.api.postgres.controllers

import com.api.postgres.entities.CommentEntity
import com.api.postgres.services.Comments
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/comments")
class CommentsController(
    private val commentsService: Comments
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
}
