package com.example.api.postgres.services



class Comments {

    // Function to load comments from the database for a given postId
    suspend fun loadComments(postId: Int): List<Comment> {
        return withContext(Dispatchers.IO) {
            transaction {
                CommentsTable.select { CommentsTable.postId eq postId }
                    .map {
                        Comment(
                            commentId = it[CommentsTable.commentId],
                            postId = it[CommentsTable.postId],
                            userId = it[CommentsTable.userId],
                            username = it[CommentsTable.username],
                            content = it[CommentsTable.content],
                            sentiment = it[CommentsTable.sentiment]
                        )
                    }
            }
        }
    }

    // Function to insert a new comment into the database
    suspend fun insertComment(newComment: Comment) {
        withContext(Dispatchers.IO) {
            transaction {
                CommentsTable.insert {
                    it[postId] = newComment.postId
                    it[CommentsTable.userId] = newComment.userId
                    it[username] = newComment.username
                    it[content] = newComment.content
                    it[sentiment] = newComment.sentiment
                }
            }
        }
    }
}

