package com.api.postgres.services

import com.api.postgres.CommentDto
import com.api.postgres.websocket.CommentWebSocketHandler
import org.springframework.stereotype.Service

@Service
class CommentCoordination(
    private val commentsService: Comments,
    private val webSocketHandler: CommentWebSocketHandler
) {
    suspend fun insertCommentAndNotify(comment: CommentDto): Int {
        // Insert comment and get ID
        val commentId = commentsService.insertComment(comment)
        val completeComment = comment.copy(commentId = commentId)

        // Handle WebSocket notifications
        if (comment.parentCommentId != null) {
            // For replies, find root parent and broadcast
            val rootParentId = commentsService.findRootParentId(comment.parentCommentId)
            webSocketHandler.broadcastNewReply(completeComment, comment.parentCommentId)

            // Update reply counts
            val replyCount = commentsService.getReplyCountsForComments(listOf(comment.parentCommentId))
                .firstOrNull()?.replyCount ?: 0
            webSocketHandler.broadcastReplyCount(comment.parentCommentId, replyCount)
        } else {
            // For root comments, just broadcast
            webSocketHandler.broadcastNewRootComment(completeComment)
        }

        return commentId
    }
}