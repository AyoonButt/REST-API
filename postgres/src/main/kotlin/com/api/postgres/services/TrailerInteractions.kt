package com.api.postgres.services

import com.api.postgres.InteractionStates
import com.api.postgres.TrailerInteractionDto
import com.api.postgres.TrailerInteractionProjection
import com.api.postgres.repositories.UserTrailerInteractionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Service
class TrailerInteractions(
    private val userTrailerInteractionRepository: UserTrailerInteractionRepository
) {

    private val logger: Logger = LoggerFactory.getLogger(TrailerInteractions::class.java)
    private fun TrailerInteractionProjection.toDto() = TrailerInteractionDto(
        interactionId = interactionId,
        userId = userId,
        postId = postId,
        startTimestamp = startTimestamp,
        endTimestamp = endTimestamp,
        replayCount = replayCount,
        isMuted = isMuted,
        likeState = likeState,
        saveState = saveState,
        commentButtonPressed = commentButtonPressed
    )


    @Transactional
    suspend fun saveInteractionData(interaction: TrailerInteractionDto) = withContext(Dispatchers.IO) {
        userTrailerInteractionRepository.insertInteraction(
            userId = interaction.userId,
            postId = interaction.postId,
            startTimestamp = interaction.startTimestamp,
            endTimestamp = interaction.endTimestamp,
            replayCount = interaction.replayCount,
            isMuted = interaction.isMuted,
            likeState = interaction.likeState,
            saveState = interaction.saveState,
            commentButtonPressed = interaction.commentButtonPressed
        )
    }


    @Transactional(readOnly = true)
    suspend fun getTrailerInteractionsByUser(userId: Int): List<TrailerInteractionDto> {
        return withContext(Dispatchers.IO) {
            userTrailerInteractionRepository
                .findDtosByUserId(userId)
                .map { it.toDto() }
        }
    }


    @Transactional(readOnly = true)
    suspend fun getTrailerInteraction(userId: Int, postId: Int): TrailerInteractionDto? {
        return withContext(Dispatchers.IO) {
            userTrailerInteractionRepository
                .findDtoByUserAndPost(userId, postId)
                ?.toDto()
        }
    }

    @Transactional(readOnly = true)
    suspend fun getLikedTrailers(userId: Int): List<Int> {
        return withContext(Dispatchers.IO) {
            userTrailerInteractionRepository.findLikedPostIds(userId)
                .map { it.postId }
        }
    }

    @Transactional(readOnly = true)
    suspend fun getSavedTrailers(userId: Int): List<Int> {
        return withContext(Dispatchers.IO) {
            userTrailerInteractionRepository.findSavedPostIds(userId)
                .map { it.postId }
        }
    }

    @Transactional(readOnly = true)
    suspend fun getTrailerInteractionStates(userId: Int, postId: Int): InteractionStates =
        withContext(Dispatchers.IO) {
            try {
                val projection = userTrailerInteractionRepository.getTrailerInteractionStates(userId, postId)
                InteractionStates(
                    isLiked = projection?.likeState == true,
                    isSaved = projection?.saveState == true
                )
            } catch (e: Exception) {
                logger.error("Error getting trailer interaction states for user $userId and post $postId: ${e.message}")
                InteractionStates(isLiked = false, isSaved = false)
            }
        }
}