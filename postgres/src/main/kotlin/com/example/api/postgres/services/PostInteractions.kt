package com.example.api.postgres.services

class PostInteractions {

    /**
     * Updates the interaction data for a given post.
     */
    suspend fun saveInteractionData(interactionData: PostInteractionData) {
        transaction {
            UserPostInteractions.insert {
                it[userId] = interactionData.userId
                it[postId] = interactionData.postId
                it[likeState] = interactionData.likeState
                it[saveState] = interactionData.saveState
                it[commentButtonPressed] = interactionData.commentButtonPressed
                it[commentMade] = interactionData.commentMade
                it[timestamp] = getCurrentTimestamp()

                // Only log timeSpent if it's provided
                interactionData.timeSpent?.let { spent ->
                    it[timeSpentOnPost] = spent
                }
            }
        }
    }

    /**
     * Updates the interaction timestamp for a given postId.
     */
    suspend fun updateInteractionTimestamp(postId: Int, timestamp: Long) {
        transaction {
            UserPostInteractions.update({ UserPostInteractions.postId eq postId }) {
                it[UserPostInteractions.timestamp] = timestamp
            }
        }
    }

    /**
     * Retrieves all post interactions for a given user.
     * @param userId The ID of the user.
     * @return A list of post interactions for the user.
     */
    suspend fun getPostInteractionsByUser(userId: Int): List<UserPostInteraction> {
        return transaction {
            UserPostInteractions
                .select { UserPostInteractions.userId eq userId }
                .map { row ->
                    UserPostInteraction(
                        postId = row[UserPostInteractions.postId],
                        timeSpent = row[UserPostInteractions.timeSpent],
                        likeState = row[UserPostInteractions.likeState],
                        saveState = row[UserPostInteractions.saveState],
                        commentButtonPressed = row[UserPostInteractions.commentButtonPressed],
                        commentMade = row[UserPostInteractions.commentMade],
                        timestamp = row[UserPostInteractions.timestamp]
                    )
                }
        }
    }

    /**
     * Retrieves a single post interaction for a given user and postId.
     * @param userId The ID of the user.
     * @param postId The ID of the post.
     * @return The post interaction data for the specified user and post, if available.
     */
    suspend fun getPostInteraction(userId: Int, postId: Int): UserPostInteraction? {
        return transaction {
            UserPostInteractions
                .select { (UserPostInteractions.userId eq userId) and (UserPostInteractions.postId eq postId) }
                .mapNotNull { row ->
                    UserPostInteraction(
                        postId = row[UserPostInteractions.postId],
                        timeSpent = row[UserPostInteractions.timeSpent],
                        likeState = row[UserPostInteractions.likeState],
                        saveState = row[UserPostInteractions.saveState],
                        commentButtonPressed = row[UserPostInteractions.commentButtonPressed],
                        commentMade = row[UserPostInteractions.commentMade],
                        timestamp = row[UserPostInteractions.timestamp]
                    )
                }
                .singleOrNull()
        }
    }

    /**
     * Retrieves all posts where the final interaction state was 'Liked'.
     * @param userId The ID of the user.
     * @return A list of post IDs that were liked.
     */
    suspend fun getLikedPosts(userId: Int): List<Int> {
        return transaction {
            UserPostInteractions
                .select { (UserPostInteractions.userId eq userId) and (UserPostInteractions.likeState eq true) }
                .orderBy(UserPostInteractions.timestamp to SortOrder.DESC)
                .map { row ->
                    row[UserPostInteractions.postId]
                }
                .distinct()
        }
    }

    /**
     * Retrieves all posts where the final interaction state was 'Saved'.
     * @param userId The ID of the user.
     * @return A list of post IDs that were saved.
     */
    suspend fun getSavedPosts(userId: Int): List<Int> {
        return transaction {
            UserPostInteractions
                .select { (UserPostInteractions.userId eq userId) and (UserPostInteractions.saveState eq true) }
                .orderBy(UserPostInteractions.timestamp to SortOrder.DESC)
                .map { row ->
                    row[UserPostInteractions.postId]
                }
                .distinct()
        }
    }

    /**
     * Retrieves all posts where the final interaction state had a comment made.
     * @param userId The ID of the user.
     * @return A list of post IDs that had comments made.
     */
    suspend fun getCommentMadePosts(userId: Int): List<Int> {
        return transaction {
            UserPostInteractions
                .select { (UserPostInteractions.userId eq userId) and (UserPostInteractions.commentMade eq true) }
                .orderBy(UserPostInteractions.timestamp to SortOrder.DESC)
                .map { row ->
                    row[UserPostInteractions.postId]
                }
                .distinct()
        }
    }
}
