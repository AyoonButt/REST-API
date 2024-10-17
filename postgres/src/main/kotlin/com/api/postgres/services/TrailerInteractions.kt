package com.api.postgres.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TrailerInteractions(
    private val userTrailerInteractionRepository: UserTrailerInteractionRepository
) {

    @Transactional
    suspend fun updateInteractionTimestamp(postId: Int, timestamp: Long) {
        val interaction = userTrailerInteractionRepository.findById(postId).orElse(null)
        if (interaction != null) {
            val updatedInteraction = interaction.copy(timestamp = timestamp)
            userTrailerInteractionRepository.save(updatedInteraction)
        }
    }

    @Transactional
    suspend fun saveInteractionData(interactionData: TrailerInteractionData) {
        val newInteraction = UserTrailerInteractionEntity(
            userId = interactionData.userId,
            postId = interactionData.postId,
            timeSpent = interactionData.playTime,
            replayTimes = interactionData.replayCount,
            isMuted = interactionData.isMuted,
            likeState = interactionData.likeState,
            saveState = interactionData.saveState,
            commentButtonPressed = interactionData.commentButtonPressed,
            commentMade = interactionData.commentMade,
            timestamp = System.currentTimeMillis()
        )
        userTrailerInteractionRepository.save(newInteraction)
    }

    @Transactional(readOnly = true)
    suspend fun getTrailerInteractionsByUser(userId: Int): List<UserTrailerInteractionEntity> {
        return userTrailerInteractionRepository.findAll().filter { it.userId == userId }
    }

    @Transactional(readOnly = true)
    suspend fun getTrailerInteraction(userId: Int, postId: Int): UserTrailerInteractionEntity? {
        return userTrailerInteractionRepository.findByUserIdAndPostId(userId, postId)
    }

    @Transactional(readOnly = true)
    suspend fun getLikedTrailers(userId: Int): List<Int> {
        return userTrailerInteractionRepository
            .findByUserIdAndTrailerLikeStateIsTrueOrderByTimestampDesc(userId)
            .map { it.postId }
    }

    @Transactional(readOnly = true)
    suspend fun getSavedTrailers(userId: Int): List<Int> {
        return userTrailerInteractionRepository
            .findByUserIdAndTrailerSaveStateIsTrueOrderByTimestampDesc(userId)
            .map { it.postId }
    }

    @Transactional(readOnly = true)
    suspend fun getCommentMadeTrailers(userId: Int): List<Int> {
        return userTrailerInteractionRepository
            .findByUserIdAndCommentMadeIsTrueOrderByTimestampDesc(userId)
            .map { it.postId }
    }
}


