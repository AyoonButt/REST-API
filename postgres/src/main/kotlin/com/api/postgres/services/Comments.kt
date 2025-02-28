package com.api.postgres.services


import com.api.postgres.CommentDto
import com.api.postgres.CommentProjection
import com.api.postgres.InfoDto
import com.api.postgres.ReplyCountDto
import com.api.postgres.repositories.CommentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.collections.map


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
        parentCommentId = parentCommentId,
        commentType = commentType
    )

    private val logger: Logger = LoggerFactory.getLogger(Comments::class.java)

    @Transactional(readOnly = true)
    suspend fun loadComments(
        postId: Int,
        limit: Int = 50,
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
    suspend fun insertComment(comment: CommentDto): Int {
        commentRepository.insertComment(
            userId = comment.userId,
            postId = comment.postId,
            content = comment.content,
            sentiment = comment.sentiment,
            timestamp = comment.timestamp,
            parentCommentId = comment.parentCommentId,
            commentType = comment.commentType
        )
        return commentRepository.getLastInsertedId()
    }

    @Transactional(readOnly = true)
    suspend fun getCommentsByUserIdAndType(
        userId: Int,
        commentType: String,
        page: Int,
        pageSize: Int
    ): List<CommentDto> = withContext(Dispatchers.IO) {
        try {
            val offset = page * pageSize
            return@withContext commentRepository.findCommentsByUserIdAndType(
                userId = userId,
                commentType = commentType,
                limit = pageSize,
                offset = offset
            ).map { it.toDto() }
        } catch (e: Exception) {
            logger.error("Error fetching comments for user $userId: ${e.message}")
            emptyList()
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
    suspend fun findRootCommentData(commentId: Int): CommentDto? = withContext(Dispatchers.IO) {
        try {
            val rootComment = commentRepository.findRootCommentWithData(commentId)
            rootComment?.toDto()
        } catch (e: Exception) {
            logger.error("Error finding root comment data for comment $commentId: ${e.message}")
            null
        }
    }

    @Transactional(readOnly = true)
    suspend fun findRootParentId(commentId: Int): Int? = withContext(Dispatchers.IO) {
        try {
            commentRepository.findRootParentId(commentId)
        } catch (e: Exception) {
            logger.error("Error finding root parent for comment $commentId: ${e.message}")
            null
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


    @Transactional(readOnly = true)
    suspend fun getReplyCountsForComments(parentIds: List<Int>): List<ReplyCountDto> {
        if (parentIds.isEmpty()) return emptyList()

        return withContext(Dispatchers.IO) {
            commentRepository.getReplyCountsForComments(parentIds)
                .map { projection ->
                    ReplyCountDto(
                        parentId = projection.parentId,
                        replyCount = projection.replyCount
                    )
                }
        }
    }


}
