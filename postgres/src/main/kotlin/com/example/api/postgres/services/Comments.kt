class Comments {

    private fun loadComments() {
        lifecycleScope.launch {
            val comments = withContext(Dispatchers.IO) {
                transaction {
                    Comments.select { Comments.postId eq postId }
                        .map {
                            Comment(
                                commentId = it[Comments.commentId],
                                postId = it[Comments.postId],
                                userId = it[Comments.userId],
                                username = it[Comments.username],
                                content = it[Comments.content],
                                sentiment = it[Comments.sentiment]
                            )
                        }
                }
            }
            if (comments.isNotEmpty()) {
                commentsAdapter.updateComments(comments)
            }
        }
    }

 

    fun insertComment(comment: Comment) {
        try {
            withContext(Dispatchers.IO) {
                // Insert new comment into the database
                transaction {
                    Comments.insert {
                        it[postId] = newComment.postId
                        it[Comments.userId] = newComment.userId
                        it[username] = newComment.username
                        it[content] = newComment.content
                        it[sentiment] = newComment.sentiment
                    }
                }
            }
        }
    }
}