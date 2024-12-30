package com.api.postgres.services


import com.api.postgres.CommentDto
import com.api.postgres.CommentProjection
import com.api.postgres.repositories.CommentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class Comments(
    private val commentRepository: CommentRepository
) {

    private fun CommentProjection.toDto() = CommentDto(
        commentId = commentId,
        userId = userId,
        username = username,
        postId = postId,
        content = content,
        sentiment = sentiment,
        timestamp = timestamp,
        parentCommentId = parentCommentId
    )

    private val logger: Logger = LoggerFactory.getLogger(Comments::class.java)

    @Transactional(readOnly = true)
    suspend fun loadComments(
        postId: Int,
        limit: Int = 10,
        offset: Int = 0
    ): List<CommentDto> = withContext(Dispatchers.IO) {
        try {
            commentRepository.findCommentsByPostId(postId, limit, offset)
                .map { it.toDto() }
        } catch (e: Exception) {
            logger.error("Error loading comments for post $postId: ${e.message}")
            emptyList()
        }
    }

    @Transactional
    suspend fun insertComment(comment: CommentDto) {
        withContext(Dispatchers.IO) {
            commentRepository.insertComment(
                userId = comment.userId,
                postId = comment.postId,
                content = comment.content,
                sentiment = comment.sentiment,
                timestamp = comment.timestamp
            )
        }
    }

    @Transactional
    suspend fun addReplyToComment(commentId: Int, reply: CommentDto) {
        withContext(Dispatchers.IO) {
            commentRepository.insertReply(
                parentCommentId = commentId,
                userId = reply.userId,
                postId = reply.postId,
                content = reply.content,
                sentiment = reply.sentiment,
                timestamp = reply.timestamp
            )
        }
    }

    @Transactional(readOnly = true)
    suspend fun findAllReplies(
        parentCommentId: Int,
        limit: Int = 10,
        offset: Int = 0
    ): List<CommentDto> = withContext(Dispatchers.IO) {
        try {
            commentRepository.findRepliesByParentId(parentCommentId, limit, offset)
                .map { it.toDto() }
        } catch (e: Exception) {
            logger.error("Error finding replies for parent comment $parentCommentId: ${e.message}")
            emptyList()
        }
    }

    @Transactional(readOnly = true)
    suspend fun findReplies(
        parentCommentId: Int,
        userId: Int,
        limit: Int = 10,
        offset: Int = 0
    ): List<CommentDto> = withContext(Dispatchers.IO) {
        try {
            commentRepository.findRepliesByParentIdAndUserId(
                parentCommentId,
                userId,
                limit,
                offset
            ).map { it.toDto() }
        } catch (e: Exception) {
            logger.error("Error finding replies for parent comment $parentCommentId and user $userId: ${e.message}")
            emptyList()
        }
    }

    @Transactional(readOnly = true)
    suspend fun getParentCommentUsername(commentId: Int): String? {
        return withContext(Dispatchers.IO) {
            commentRepository.findParentCommentUsername(commentId)
        }
    }
}
