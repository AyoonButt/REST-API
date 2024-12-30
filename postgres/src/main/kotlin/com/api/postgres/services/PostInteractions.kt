package com.api.postgres.services

import com.api.postgres.UserPostInteractionDto
import com.api.postgres.UserPostInteractionProjection
import com.api.postgres.repositories.UserPostInteractionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostInteractions(private val interactionRepository: UserPostInteractionRepository) {


    private val logger: Logger = LoggerFactory.getLogger(PostInteractions::class.java)

    private fun UserPostInteractionProjection.toDto() = UserPostInteractionDto(
        interactionId = interactionId,
        userId = userId,
        postId = postId,
        timeSpentOnPost = timeSpentOnPost,
        likeState = likeState,
        saveState = saveState,
        commentButtonPressed = commentButtonPressed,
        commentMade = commentMade,
        timestamp = timestamp
    )

    @Transactional
    suspend fun saveInteractionData(interactionData: UserPostInteractionDto) {
        withContext(Dispatchers.IO) {
            interactionRepository.insertInteraction(
                userId = interactionData.userId,
                postId = interactionData.postId,
                timeSpentOnPost = interactionData.timeSpentOnPost,
                likeState = interactionData.likeState,
                saveState = interactionData.saveState,
                commentButtonPressed = interactionData.commentButtonPressed,
                commentMade = interactionData.commentMade,
                timestamp = interactionData.timestamp
            )
        }
    }

    @Transactional
    suspend fun updateInteractionTimestamp(userId: Int, postId: Int, timestamp: Long) {
        withContext(Dispatchers.IO) {
            interactionRepository.updateTimestamp(
                userId = userId,
                postId = postId,
                timestamp = timestamp.toString()
            )
        }
    }

    @Transactional(readOnly = true)
    suspend fun getPostInteractionsByUser(userId: Int): List<UserPostInteractionDto> =
        withContext(Dispatchers.IO) {
            try {
                interactionRepository.findInteractionsByUserId(userId)
                    .map { it.toDto() }
            } catch (e: Exception) {
                logger.error("Error finding interactions for user $userId: ${e.message}")
                emptyList()
            }
        }

    @Transactional(readOnly = true)
    suspend fun getPostInteraction(userId: Int, postId: Int): UserPostInteractionDto? =
        withContext(Dispatchers.IO) {
            try {
                interactionRepository.findInteractionByUserAndPost(userId, postId)?.toDto()
            } catch (e: Exception) {
                logger.error("Error finding interaction for user $userId and post $postId: ${e.message}")
                null
            }
        }

    @Transactional(readOnly = true)
    suspend fun getLikedPosts(userId: Int): List<Int> {
        return withContext(Dispatchers.IO) {
            interactionRepository.findLikedPostsByUserUserId(userId)
        }
    }

    @Transactional(readOnly = true)
    suspend fun getSavedPosts(userId: Int): List<Int> {
        return withContext(Dispatchers.IO) {
            interactionRepository.findSavedPostsByUserUserId(userId)
        }
    }

    @Transactional(readOnly = true)
    suspend fun getCommentMadePosts(userId: Int): List<Int> {
        return withContext(Dispatchers.IO) {
            interactionRepository.findCommentMadePostsByUserUserId(userId)
        }
    }
}
