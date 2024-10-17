package com.api.postgres.services

import com.api.postgres.entities.UserPostInteraction
import com.api.postgres.repositories.UserPostInteractionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostInteractions(private val interactionRepository: UserPostInteractionRepository) {

    /**
     * Updates the interaction data for a given post.
     */
    @Transactional
    fun saveInteractionData(interactionData: UserPostInteraction) {
        interactionRepository.save(interactionData)
    }

    /**
     * Updates the interaction timestamp for a given postId.
     */
    @Transactional
    fun updateInteractionTimestamp(postId: Int, timestamp: Long) {
        interactionRepository.findByUserIdAndPostId(interactionData.userId, postId)?.let { interaction ->
            interaction.timestamp = timestamp
            interactionRepository.save(interaction)
        }
    }

    /**
     * Retrieves all post interactions for a given user.
     * @param userId The ID of the user.
     * @return A list of post interactions for the user.
     */
    @Transactional(readOnly = true)
    fun getPostInteractionsByUser(userId: Int): List<UserPostInteraction> {
        return interactionRepository.findByUserId(userId)
    }

    /**
     * Retrieves a single post interaction for a given user and postId.
     * @param userId The ID of the user.
     * @param postId The ID of the post.
     * @return The post interaction data for the specified user and post, if available.
     */
    @Transactional(readOnly = true)
    fun getPostInteraction(userId: Int, postId: Int): UserPostInteraction? {
        return interactionRepository.findByUserIdAndPostId(userId, postId)
    }

    /**
     * Retrieves all posts where the final interaction state was 'Liked'.
     * @param userId The ID of the user.
     * @return A list of post IDs that were liked.
     */
    @Transactional(readOnly = true)
    fun getLikedPosts(userId: Int): List<Int> {
        return interactionRepository.findLikedPostsByUserId(userId)
    }

    /**
     * Retrieves all posts where the final interaction state was 'Saved'.
     * @param userId The ID of the user.
     * @return A list of post IDs that were saved.
     */
    @Transactional(readOnly = true)
    fun getSavedPosts(userId: Int): List<Int> {
        return interactionRepository.findSavedPostsByUserId(userId)
    }

    /**
     * Retrieves all posts where the final interaction state had a comment made.
     * @param userId The ID of the user.
     * @return A list of post IDs that had comments made.
     */
    @Transactional(readOnly = true)
    fun getCommentMadePosts(userId: Int): List<Int> {
        return interactionRepository.findCommentMadePostsByUserId(userId)
    }
}
