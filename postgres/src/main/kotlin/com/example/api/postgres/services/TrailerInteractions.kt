package com.example.api.postgres.services

class TrailerInteractions {

    /**
     * Updates the interaction timestamp for a given postId in the database.
     */
    suspend fun updateInteractionTimestamp(postId: Int, timestamp: Long) {
        transaction {
            UserTrailerInteractions.update({ UserTrailerInteractions.postId eq postId }) {
                it[UserTrailerInteractions.timestamp] = timestamp
            }
        }
    }
    

    /**
     * Saves trailer interaction data to the database.
     */
    +
    suspend fun saveInteractionData(interactionData: TrailerInteractionData) {
        transaction {
            UserTrailerInteractions.insert {
                it[this.userId] = interactionData.userId
                it[this.postId] = interactionData.postId
                it[this.timeSpent] = interactionData.playTime
                it[this.replayTimes] = interactionData.replayCount
                it[this.isMuted] = interactionData.isMuted
                it[this.trailerLikeState] = interactionData.likeState
                it[this.trailerSaveState] = interactionData.saveState
                it[this.commentButtonPressed] = interactionData.commentButtonPressed
                it[this.commentMade] = interactionData.commentMade
            }
        }
    }

    /**
     * Retrieves all trailer interactions for a given user.
     * @param userId The ID of the user.
     * @return A list of trailer interactions for the user.
     */
    suspend fun getTrailerInteractionsByUser(userId: Int): List<UserTrailerInteraction> {
        return transaction {
            UserTrailerInteractions
                .select { UserTrailerInteractions.userId eq userId }
                .map { row ->
                    UserTrailerInteraction(
                        postId = row[UserTrailerInteractions.postId],
                        timeSpent = row[UserTrailerInteractions.timeSpent],
                        replayTimes = row[UserTrailerInteractions.replayTimes],
                        isMuted = row[UserTrailerInteractions.isMuted],
                        likeState = row[UserTrailerInteractions.trailerLikeState],
                        saveState = row[UserTrailerInteractions.trailerSaveState],
                        commentButtonPressed = row[UserTrailerInteractions.commentButtonPressed],
                        commentMade = row[UserTrailerInteractions.commentMade],
                        timestamp = row[UserTrailerInteractions.timestamp]
                    )
                }
        }
    }
    
    /**
     * Retrieves a single trailer interaction for a given user and postId.
     * @param userId The ID of the user.
     * @param postId The ID of the post.
     * @return The trailer interaction data for the specified user and post, if available.
     */
    suspend fun getTrailerInteraction(userId: Int, postId: Int): UserTrailerInteraction? {
        return transaction {
            UserTrailerInteractions
                .select { (UserTrailerInteractions.userId eq userId) and (UserTrailerInteractions.postId eq postId) }
                .mapNotNull { row ->
                    UserTrailerInteraction(
                        postId = row[UserTrailerInteractions.postId],
                        timeSpent = row[UserTrailerInteractions.timeSpent],
                        replayTimes = row[UserTrailerInteractions.replayTimes],
                        isMuted = row[UserTrailerInteractions.isMuted],
                        likeState = row[UserTrailerInteractions.trailerLikeState],
                        saveState = row[UserTrailerInteractions.trailerSaveState],
                        commentButtonPressed = row[UserTrailerInteractions.commentButtonPressed],
                        commentMade = row[UserTrailerInteractions.commentMade],
                        timestamp = row[UserTrailerInteractions.timestamp]
                    )
                }
                .singleOrNull()
        }
    }

    /**
     * Retrieves all trailers where the final interaction state was 'Liked'.
     * @param userId The ID of the user.
     * @return A list of trailer IDs that were liked.
     */
    suspend fun getLikedTrailers(userId: Int): List<Int> {
        return transaction {
            UserTrailerInteractions
                .select { (UserTrailerInteractions.userId eq userId) and (UserTrailerInteractions.trailerLikeState eq true) }
                .orderBy(UserTrailerInteractions.timestamp to SortOrder.DESC)
                .map { row ->
                    row[UserTrailerInteractions.postId]
                }
                .distinct()
        }
    }

    /**
     * Retrieves all trailers where the final interaction state was 'Saved'.
     * @param userId The ID of the user.
     * @return A list of trailer IDs that were saved.
     */
    suspend fun getSavedTrailers(userId: Int): List<Int> {
        return transaction {
            UserTrailerInteractions
                .select { (UserTrailerInteractions.userId eq userId) and (UserTrailerInteractions.trailerSaveState eq true) }
                .orderBy(UserTrailerInteractions.timestamp to SortOrder.DESC)
                .map { row ->
                    row[UserTrailerInteractions.postId]
                }
                .distinct()
        }
    }

    /**
     * Retrieves all trailers where the final interaction state had a comment made.
     * @param userId The ID of the user.
     * @return A list of trailer IDs that had comments made.
     */
    suspend fun getCommentMadeTrailers(userId: Int): List<Int> {
        return transaction {
            UserTrailerInteractions
                .select { (UserTrailerInteractions.userId eq userId) and (UserTrailerInteractions.commentMade eq true) }
                .orderBy(UserTrailerInteractions.timestamp to SortOrder.DESC)
                .map { row ->
                    row[UserTrailerInteractions.postId]
                }
                .distinct()
        }
    }
}


