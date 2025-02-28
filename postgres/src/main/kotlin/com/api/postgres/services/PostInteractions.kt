package com.api.postgres.services

import com.api.postgres.InteractionStates
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
        startTimestamp = startTimestamp,
        endTimestamp = endTimestamp,
        likeState = likeState,
        saveState = saveState,
        commentButtonPressed = commentButtonPressed
    )

    @Transactional
    suspend fun saveInteractionData(interactionData: UserPostInteractionDto) {
        try {
            interactionRepository.insertInteraction(
                userId = interactionData.userId,
                postId = interactionData.postId,
                startTimestamp = interactionData.startTimestamp,
                endTimestamp = interactionData.endTimestamp,
                likeState = interactionData.likeState,
                saveState = interactionData.saveState,
                commentButtonPressed = interactionData.commentButtonPressed
            )
            logger.info("Successfully saved interaction for user: ${interactionData.userId}, post: ${interactionData.postId}")
        } catch (e: Exception) {
            logger.error("Error saving interaction: ${e.message}")
            throw e
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
                .map { it.postId }
        }
    }

    @Transactional(readOnly = true)
    suspend fun getSavedPosts(userId: Int): List<Int> {
        return withContext(Dispatchers.IO) {
            interactionRepository.findSavedPostsByUserUserId(userId)
                .map { it.postId }
        }
    }


    @Transactional(readOnly = true)
    suspend fun getPostInteractionStates(userId: Int, postId: Int): InteractionStates =
        withContext(Dispatchers.IO) {
            try {
                val projection = interactionRepository.getPostInteractionStates(userId, postId)
                InteractionStates(
                    isLiked = projection?.likeState == true,
                    isSaved = projection?.saveState == true
                )
            } catch (e: Exception) {
                logger.error("Error getting interaction states for user $userId and post $postId: ${e.message}")
                InteractionStates(isLiked = false, isSaved = false)
            }
        }

}
