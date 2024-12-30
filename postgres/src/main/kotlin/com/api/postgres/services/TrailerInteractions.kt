package com.api.postgres.services



import com.api.postgres.TrailerInteractionDto
import com.api.postgres.TrailerInteractionProjection
import com.api.postgres.repositories.UserTrailerInteractionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Service
class TrailerInteractions(
    private val userTrailerInteractionRepository: UserTrailerInteractionRepository
) {

    private fun TrailerInteractionProjection.toDto() = TrailerInteractionDto(
        interactionId = interactionId,
        userId = userId,
        postId = postId,
        timeSpent = timeSpent,
        replayCount = replayCount,
        isMuted = isMuted,
        likeState = likeState,
        saveState = saveState,
        commentButtonPressed = commentButtonPressed,
        commentMade = commentMade,
        timestamp = timestamp
    )


    @Transactional
    suspend fun updateInteractionTimestamp(postId: Int, timestamp: String) {
        withContext(Dispatchers.IO) {
            userTrailerInteractionRepository.updateTimestamp(postId, timestamp)
        }
    }

    @Transactional
    suspend fun saveInteractionData(interaction: TrailerInteractionDto) {
        withContext(Dispatchers.IO) {
            userTrailerInteractionRepository.insertInteraction(
                userId = interaction.userId,
                postId = interaction.postId,
                timeSpent = interaction.timeSpent,
                replayCount = interaction.replayCount,
                isMuted = interaction.isMuted,
                likeState = interaction.likeState,
                saveState = interaction.saveState,
                commentButtonPressed = interaction.commentButtonPressed,
                commentMade = interaction.commentMade,
                timestamp = System.currentTimeMillis().toString()
            )
        }
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
        }
    }

    @Transactional(readOnly = true)
    suspend fun getSavedTrailers(userId: Int): List<Int> {
        return withContext(Dispatchers.IO) {
            userTrailerInteractionRepository.findSavedPostIds(userId)
        }
    }

    @Transactional(readOnly = true)
    suspend fun getCommentMadeTrailers(userId: Int): List<Int> {
        return withContext(Dispatchers.IO) {
            userTrailerInteractionRepository.findCommentMadePostIds(userId)
        }
    }
}