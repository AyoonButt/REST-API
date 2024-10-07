class TrailerInteractions {

    private fun updateInteractionTimestamp(postId: Int) {
        transaction {
            UserTrailerInteractions.update({ UserTrailerInteractions.postId eq postId }) {
                it[timestamp] = getCurrentTimestamp()
            }
        }
    }

    fun saveInteractionData() {
        val playTime = customPlayer.getPlayTime()
        val replayCount = customPlayer.getReplayCount()
        val IsMuted = customPlayer.getIsMuted()
        val likeState = customPlayer.getLikeState()
        val saveState = customPlayer.getSaveState()
        val commentButtonPressed = customPlayer.wasCommentButtonPressed()
        val commentMade = customPlayer.wasCommentMade()
        val (_, postId) = videos[bindingAdapterPosition]

        // Save this data to the database using Exposed ORM
        transaction {
            UserTrailerInteractions.insert {
                it[userId] = UserId
                it[UserTrailerInteractions.postId] = postId
                it[timeSpent] = playTime
                it[replayTimes] = replayCount
                it[isMuted] = IsMuted
                it[trailerLikeState] = likeState
                it[trailerSaveState] = saveState
                it[UserTrailerInteractions.commentButtonPressed] = commentButtonPressed
                it[UserTrailerInteractions.commentMade] = commentMade
            }
        }
    }

}
