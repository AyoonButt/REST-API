package com.api.postgres.services


import com.api.postgres.PostDto
import com.api.postgres.PostProjection
import com.api.postgres.UserPreferencesDto
import com.api.postgres.models.PostEntity
import com.api.postgres.models.PostLanguages
import com.api.postgres.repositories.PostGenresRepository
import com.api.postgres.repositories.PostLanguagesRepository
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
    private val postLanguagesRepository: PostLanguagesRepository,

) {

    private val logger: Logger = LoggerFactory.getLogger(Posts::class.java)

    private fun PostProjection.toDto(): PostDto? {
        val tmdbId = this.tmdbId ?: return null

        return PostDto(
            postId = postId,
            tmdbId = tmdbId,  // Use the non-null value
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
    }

    @Transactional
    suspend fun addPostsToDatabase(
        mediaType: String,
        language: String,
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

                // Insert requested language
                postLanguagesRepository.insertPostLanguage(savedPost.postId, language)
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
                .mapNotNull { projection ->
                    projection.tmdbId?.let { // Only map if tmdbId is not null
                        projection.toDto()
                    }
                }
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

    @Transactional(readOnly = true)
    suspend fun getPostDtosForInteractionIds(
        interactionIds: List<Int>
    ): List<PostDto> {
        if (interactionIds.isEmpty()) {
            println("InteractionIds is empty")
            return emptyList()
        }

        return try {
            println("Fetching posts with IDs: $interactionIds")

            // Fetch each post individually and maintain order
            interactionIds.mapNotNull { id ->
                postRepository.findDtoById(id)?.toDto()
            }.also {
                println("Final posts size: ${it.size}")
            }
        } catch (e: Exception) {
            println("Error fetching posts: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    @Transactional(readOnly = true)
    suspend fun getFilteredPostIds(
        preferences: UserPreferencesDto,
        loosenFiltering: Boolean = false,
        loosenAttempt: Int = 0, // 0: normal, 1: try English, 2: drop avoid genres, 3: drop subscriptions
        limit: Int = 100,
        offset: Int = 0
    ): List<Int> = withContext(Dispatchers.IO) {
        try {
            // Determine which language to use based on loosening attempt
            val language = when {
                loosenFiltering && loosenAttempt >= 1 -> "en" // Try English on first loosening attempt
                else -> preferences.language
            }

            // Get posts by language
            var postIds = postLanguagesRepository.findPostIdsByLanguage(language)
                .take(limit + offset)
                .drop(offset)

            // Apply subscription filter unless we're at loosening attempt 3+
            if (preferences.subscriptions.isNotEmpty() && postIds.isNotEmpty() &&
                !(loosenFiltering && loosenAttempt >= 3)) {

                val postsWithSubscriptions = postSubscriptionsRepository.findPostIdsBySubscriptions(
                    subscriptions = preferences.subscriptions
                )

                // Keep only posts with matching subscriptions
                postIds = postIds.filter { it in postsWithSubscriptions }
            }

            // Apply avoid genres filter unless we're at loosening attempt 2+
            if (preferences.avoidGenreIds.isNotEmpty() && postIds.isNotEmpty() &&
                !(loosenFiltering && loosenAttempt >= 2)) {

                val postsWithAvoidGenres = postGenresRepository.findPostIdsWithGenres(
                    postIds = postIds,
                    genreIds = preferences.avoidGenreIds
                )

                // Remove posts with avoided genres
                postIds = postIds.filter { it !in postsWithAvoidGenres }
            }

            // Log what filters were applied or skipped
            if (loosenFiltering) {
                logger.info(
                    "Loosening filters (attempt $loosenAttempt): " +
                            "language=$language, " +
                            "applySubscriptionFilter=${!(loosenAttempt >= 3)}, " +
                            "applyAvoidGenresFilter=${!(loosenAttempt >= 2)}"
                )
            }

            logger.info("Found ${postIds.size} filtered posts for user preferences")
            postIds
        } catch (e: Exception) {
            logger.error("Error fetching filtered posts: ${e.message}")
            emptyList()
        }
    }

}