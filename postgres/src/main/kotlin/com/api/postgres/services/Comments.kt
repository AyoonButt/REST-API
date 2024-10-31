package com.api.postgres.services


import com.api.postgres.models.CommentEntity
import com.api.postgres.repositories.CommentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class Comments(
    private val commentRepository: CommentRepository
) {

    // Function to load comments from the database for a given postId
    @Transactional(readOnly = true)
    suspend fun loadComments(postId: Int): List<CommentEntity> {
        return withContext(Dispatchers.IO) {
            commentRepository.findByPostPostId(postId)
        }
    }

    // Function to insert a new comment into the database
    @Transactional
    suspend fun insertComment(newComment: CommentEntity) {
        withContext(Dispatchers.IO) {
            commentRepository.save(newComment)
        }
    }

    @Transactional
    fun addReplyToComment(commentId: Int, reply: CommentEntity): CommentEntity {
        val parentComment = commentRepository.findById(commentId)
            .orElseThrow { Exception("Parent comment not found") }

        reply.parentComment = parentComment
        return commentRepository.save(reply)
    }

    @Transactional(readOnly = true)
    fun findAllReplies(parentCommentId: Int, limit: Int = 10, offset: Int = 0): List<CommentEntity> {
        val pageable: Pageable = PageRequest.of(offset / limit, limit)
        return commentRepository.findAllReplies(parentCommentId, pageable).content // Fetch the content of the page
    }

    @Transactional(readOnly = true)
    fun findReplies(parentCommentId: Int, userId: Int, limit: Int = 10, offset: Int = 0): List<CommentEntity> {
        val pageable: Pageable = PageRequest.of(offset / limit, limit)
        return commentRepository.findReplies(parentCommentId, userId, pageable).content // Fetch the content of the page
    }


    @Transactional(readOnly = true)
    fun getCommentById(commentId: Int): CommentEntity? {
        return commentRepository.findById(commentId).orElse(null)
    }




}
