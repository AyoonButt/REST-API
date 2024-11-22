package com.api.postgres.services


import com.api.postgres.models.PostEntity
import com.api.postgres.models.UserTrailerInteraction
import com.api.postgres.repositories.UserTrailerInteractionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TrailerInteractions(
    private val userTrailerInteractionRepository: UserTrailerInteractionRepository
) {

    @Transactional
    suspend fun updateInteractionTimestamp(postId: Int, timestamp: String) {
        val interaction = userTrailerInteractionRepository.findById(postId).orElse(null)
        if (interaction != null) {
            val updatedInteraction = interaction.copy(timestamp = timestamp.toString())
            userTrailerInteractionRepository.save(updatedInteraction)
        }
    }

    @Transactional
    suspend fun saveInteractionData(interactionData: UserTrailerInteraction) {
        val newInteraction = UserTrailerInteraction(
            user = interactionData.user,
            post = interactionData.post,
            timeSpent = interactionData.timeSpent,
            replayCount = interactionData.replayCount,
            isMuted = interactionData.isMuted,
            likeState = interactionData.likeState,
            saveState = interactionData.saveState,
            commentButtonPressed = interactionData.commentButtonPressed,
            commentMade = interactionData.commentMade,
            timestamp = System.currentTimeMillis().toString()
        )
        userTrailerInteractionRepository.save(newInteraction)
    }

    @Transactional(readOnly = true)
    suspend fun getTrailerInteractionsByUser(userID: Int): List<UserTrailerInteraction> {
        return userTrailerInteractionRepository.findByUserUserId(userID) // Query directly by userId
    }

    @Transactional(readOnly = true)
    suspend fun getTrailerInteraction(userId: Int, postId: Int): UserTrailerInteraction? {
        return userTrailerInteractionRepository.findByUserUserIdAndPostPostId(userId, postId)
    }

    @Transactional(readOnly = true)
    suspend fun getLikedTrailers(userId: Int): List<PostEntity> {
        return userTrailerInteractionRepository
            .findByUserUserIdAndLikeStateIsTrueOrderByTimestampDesc(userId)
            .map { it.post }
    }

    @Transactional(readOnly = true)
    suspend fun getSavedTrailers(userId: Int): List<PostEntity> {
        return userTrailerInteractionRepository
            .findByUserUserIdAndSaveStateIsTrueOrderByTimestampDesc(userId)
            .map { it.post }
    }

    @Transactional(readOnly = true)
    suspend fun getCommentMadeTrailers(userId: Int): List<PostEntity> {
        return userTrailerInteractionRepository
            .findByUserUserIdAndCommentMadeIsTrueOrderByTimestampDesc(userId)
            .map { it.post }
    }
}


