class PostInteractions {

    private fun updateData(holder: PostHolder, mypostId: Int, timeSpent: Long? = null) {
        transaction {
            UserPostInteractions.insert {
                it[userId] = UserID
                it[postId] = mypostId
                it[likeState] = holder.likeState
                it[saveState] = holder.saveState
                it[commentButtonPressed] = holder.commentButtonPressed
                it[commentMade] = holder.commentMade
                it[timestamp] = getCurrentTimestamp()

                // Only log timeSpent if it's provided
                timeSpent?.let { spent ->
                    it[timeSpentOnPost] = spent
                }
            }
        }
    }
}
