package com.api.postgres.services


import com.api.postgres.PostDto
import com.api.postgres.PostProjection
import com.api.postgres.UserPreferencesDto
import com.api.postgres.models.PostEntity
import com.api.postgres.models.PostLanguages
import com.api.postgres.recommendations.VectorInitialization
import com.api.postgres.recommendations.VectorService
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
    private val vectorInitialization: VectorInitialization
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

                vectorInitialization.initializePostVector(savedPost.postId)
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
        loosenAttempt: Int = 0, // 0: normal, 1: try English, 2: preferred genres only, 3: drop avoid genres, 4: drop subscriptions
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

            // Apply subscription filter unless we're at loosening attempt 4+
            if (preferences.subscriptions.isNotEmpty() && postIds.isNotEmpty() &&
                !(loosenFiltering && loosenAttempt >= 4)) {

                val postsWithSubscriptions = postSubscriptionsRepository.findPostIdsBySubscriptions(
                    subscriptions = preferences.subscriptions
                )

                // Keep only posts with matching subscriptions
                postIds = postIds.filter { it in postsWithSubscriptions }
            }

            // Apply preferred genres filter unless we're at loosening attempt 2+
            // This is the new block that prioritizes user's preferred genres
            if (preferences.genreIds.isNotEmpty() && postIds.isNotEmpty() &&
                loosenFiltering && loosenAttempt == 2) {

                val postsWithPreferredGenres = postGenresRepository.findPostIdsWithGenres(
                    postIds = postIds,
                    genreIds = preferences.genreIds
                )

                // If we have posts with preferred genres, only keep those
                if (postsWithPreferredGenres.isNotEmpty()) {
                    postIds = postsWithPreferredGenres
                    logger.info("Applied preferred genres filter: kept ${postIds.size} posts with user's preferred genres")
                } else {
                    logger.info("No posts found with preferred genres, keeping original filter results")
                }
            }

            // Apply avoid genres filter unless we're at loosening attempt 3+
            if (preferences.avoidGenreIds.isNotEmpty() && postIds.isNotEmpty() &&
                !(loosenFiltering && loosenAttempt >= 3)) {

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
                            "applySubscriptionFilter=${!(loosenAttempt >= 4)}, " +
                            "applyPreferredGenresFilter=${loosenAttempt == 2}, " +
                            "applyAvoidGenresFilter=${!(loosenAttempt >= 3)}"
                )
            }

            logger.info("Found ${postIds.size} filtered posts for user preferences")
            postIds
        } catch (e: Exception) {
            logger.error("Error fetching filtered posts: ${e.message}")
            emptyList()
        }
    }

    @Transactional(readOnly = true)
    suspend fun getPostLanguageCount(language: String): Int = withContext(Dispatchers.IO) {
        try {
            postLanguagesRepository.countByLanguage(language)
        } catch (e: Exception) {
            logger.error("Error counting posts for language $language: ${e.message}")
            0
        }
    }

    @Transactional(readOnly = true)
    suspend fun getPostsInsertedAfter(timestamp: Long): List<PostDto> = withContext(Dispatchers.IO) {
        try {
            val postIds = postLanguagesRepository.findPostIdsInsertedAfter(timestamp)
            if (postIds.isEmpty()) {
                return@withContext emptyList()
            }

            // Fetch the actual posts using the IDs
            postIds.mapNotNull { postId ->
                postRepository.findDtoById(postId)?.toDto()
            }
        } catch (e: Exception) {
            logger.error("Error fetching posts inserted after $timestamp: ${e.message}")
            emptyList()
        }
    }

}