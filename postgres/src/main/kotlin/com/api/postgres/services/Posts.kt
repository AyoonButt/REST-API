package com.api.postgres.services


import com.api.postgres.PostDto
import com.api.postgres.PostProjection
import com.api.postgres.models.PostEntity
import com.api.postgres.repositories.PostGenresRepository
import com.api.postgres.repositories.PostRepository
import com.api.postgres.repositories.PostSubscriptionsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Service
class Posts(
    private val postRepository: PostRepository,
    private val postGenresRepository: PostGenresRepository,
    private val postSubscriptionsRepository: PostSubscriptionsRepository,

) {

    private val logger: Logger = LoggerFactory.getLogger(Posts::class.java)

    private fun PostProjection.toDto() = PostDto(
        postId = postId,
        tmdbId = tmdbId,
        type = type,
        title = title,
        subscription = subscription,
        releaseDate = releaseDate,
        overview = overview,
        posterPath = posterPath,
        voteAverage = voteAverage,
        voteCount = voteCount,
        originalLanguage = originalLanguage,
        originalTitle = originalTitle,
        popularity = popularity,
        genreIds = genreIds,
        postLikeCount = postLikeCount,
        trailerLikeCount = trailerLikeCount,
        videoKey = videoKey
    )

    @Transactional
    suspend fun addPostsToDatabase(
        mediaType: String,
        dataList: List<PostDto>,
        providerId: Int
    ) {
        withContext(Dispatchers.IO) {
            dataList.forEach { data ->
                // Insert post
                val savedPost = postRepository.save(PostEntity(
                    tmdbId = data.tmdbId,
                    type = mediaType,
                    title = data.title,
                    subscription = providerId.toString(),
                    releaseDate = data.releaseDate.toString(),
                    overview = data.overview.toString(),
                    posterPath = data.posterPath.toString(),
                    voteAverage = data.voteAverage,
                    voteCount = data.voteCount,
                    originalLanguage = data.originalLanguage.toString(),
                    originalTitle = data.originalTitle.toString(),
                    popularity = data.popularity,
                    genreIds = data.genreIds,
                    postLikeCount = data.postLikeCount,
                    trailerLikeCount = data.trailerLikeCount,
                    videoKey = data.videoKey
                ))

                // Insert genre relationships
                data.genreIds.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .forEach { genreId ->
                        postGenresRepository.insertPostGenre(savedPost.postId!!, genreId)
                    }

                // Insert subscription relationship
                postSubscriptionsRepository.insertPostSubscription(savedPost.postId!!, providerId)
            }
        }
    }

    @Transactional(readOnly = true)
    suspend fun fetchPostsFromDatabase(
        limit: Int,
        offset: Int
    ): List<PostDto> = withContext(Dispatchers.IO) {
        try {
            postRepository.findAllDtosByOrderByPostId(limit, offset)
                .map { it.toDto() }
        } catch (e: Exception) {
            logger.error("Error fetching posts: ${e.message}")
            emptyList()
        }
    }



    @Transactional
    suspend fun updateLikeCount(postId: Int) {
        withContext(Dispatchers.IO) {
            postRepository.incrementPostLikeCount(postId)
        }
    }

    @Transactional
    suspend fun updateTrailerLikeCount(postId: Int) {
        withContext(Dispatchers.IO) {
            postRepository.incrementTrailerLikeCount(postId)
        }
    }

    @Transactional(readOnly = true)
    suspend fun fetchVideosFromDatabase(limit: Int, offset: Int): List<Pair<String, Int>> {
        return withContext(Dispatchers.IO) {
            postRepository.findAllDtosByOrderByPostId(limit, offset)
                .map { Pair(it.videoKey, it.postId ?: -1) }
        }
    }

    @Transactional(readOnly = true)
    suspend fun getPostById(postId: Int): PostDto? = withContext(Dispatchers.IO) {
        try {
            postRepository.findDtoById(postId)?.toDto()
        } catch (e: Exception) {
            logger.error("Error fetching post $postId: ${e.message}")
            null
        }
    }

    @Transactional(readOnly = true)
    suspend fun getPostIdByTmdbId(tmdbId: Int): Int? {
        return withContext(Dispatchers.IO) {
            postRepository.findPostIdByTmdbId(tmdbId)
        }
    }
}