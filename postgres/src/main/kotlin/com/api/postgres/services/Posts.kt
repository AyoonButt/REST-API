package com.api.postgres.services


import com.api.postgres.models.PostEntity
import com.api.postgres.repositories.PostRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Service
class Posts(
    private val postRepository: PostRepository
) {

    // Function to insert posts into the database
    @Transactional
    suspend fun addPostsToDatabase(mediaType: String, dataList: List<PostEntity>, providerId: Int) {
        withContext(Dispatchers.IO) {
            dataList.forEach { data ->
                val post = PostEntity(
                    tmdbId = data.tmdbId,
                    type = mediaType,
                    title = data.title,
                    subscription = providerId.toString(),
                    releaseDate = data.releaseDate,
                    overview = data.overview,
                    posterPath = data.posterPath,
                    voteAverage = data.voteAverage,
                    voteCount = data.voteCount,
                    originalLanguage = data.originalLanguage,
                    originalTitle = data.originalTitle,
                    popularity = data.popularity,
                    genreIds = data.genreIds ?: "",
                    postLikeCount = data.postLikeCount,
                    trailerLikeCount = data.trailerLikeCount,
                    videoKey = data.videoKey
                )
                postRepository.save(post) // Save each post to the database
            }
        }
    }

    // Suspend function to fetch posts from the database
    @Transactional
    suspend fun fetchPostsFromDatabase(limit: Int, offset: Int): List<PostEntity> {
        return withContext(Dispatchers.IO) {
            postRepository.findAllByOrderByPostId(limit, offset)
        }
    }

    // Update like count for a post
    @Transactional
    fun updateLikeCount(postId: Int) {
        val post = postRepository.findById(postId).orElseThrow { Exception("Post not found") }
        // Assuming postLikeCount is an Int field in Post
        post.postLikeCount = (post.postLikeCount ?: 0) + 1
        postRepository.save(post)
    }

    @Transactional
    fun updateTrailerLikeCount(postId: Int) {
        val post = postRepository.findById(postId).orElseThrow {
            IllegalArgumentException("Post with ID $postId not found")
        }
        post.trailerLikeCount += 1
        postRepository.save(post)
    }

    // Fetch videos from the database
    @Transactional
    fun fetchVideosFromDatabase(limit: Int, offset: Int): List<Pair<String, Int>> {
        return postRepository.findAllByOrderByPostId(limit, offset)
            .mapNotNull { post ->
                post.videoKey?.let { Pair(it, post.postId ?: -1) }
            }
    }

    //get posts with pagination
    @Transactional
    suspend fun getPaginatedPostsAPI(limit: Int, offset: Int): List<PostEntity> {
        return fetchPostsFromDatabase(limit, offset)
    }

    @Transactional(readOnly = true)
    fun getPostById(postId: Int): PostEntity? {
        return postRepository.findById(postId).orElse(null)
    }

    @Transactional(readOnly = true)
    fun fetchPostEntityById(postId: Int): PostEntity? {
        return postRepository.findPostById(postId)
    }

    fun getPostIdByTmdbId(tmdbId: Int): Int? {
        return postRepository.findPostIdByTmdbId(tmdbId)
    }


}
