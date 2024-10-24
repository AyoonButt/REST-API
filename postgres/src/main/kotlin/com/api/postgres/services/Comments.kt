package com.api.postgres.services


import com.api.postgres.models.CommentEntity
import com.api.postgres.repositories.CommentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class Comments(
    private val commentRepository: CommentRepository
) {

    // Function to load comments from the database for a given postId
    @Transactional(readOnly = true)
    suspend fun loadComments(postId: Int): List<CommentEntity> {
        return withContext(Dispatchers.IO) {
            commentRepository.findByPostPostId(postId)
        }
    }

    // Function to insert a new comment into the database
    @Transactional
    suspend fun insertComment(newComment: CommentEntity) {
        withContext(Dispatchers.IO) {
            commentRepository.save(newComment)
        }
    }
}
