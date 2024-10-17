package com.api.postgres.services

import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

@Service
class Posts @Autowired constructor(
    private val postRepository: PostRepository
) {
    private val client = OkHttpClient()

    // Suspend function to fetch posts (movies/series) from the API
    suspend fun fetchPostsFromAPI(mediaType: String, providerId: Int): List<Post> {
        val request = Request.Builder()
            .url("https://api.themoviedb.org/3/$mediaType/popular?provider=$providerId&language=en-US&page=1")
            .get()
            .addHeader("accept", "application/json")
            .addHeader("Authorization", "Bearer YOUR_API_KEY")
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                responseBody?.let {
                    parsePosts(it, mediaType, providerId) // Parses and returns List<Post>
                } ?: emptyList()
            } catch (e: IOException) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    // Function to insert posts into the database
    @Transactional
    suspend fun addPostsToDatabase(mediaType: String, dataList: List<Post>, providerId: Int) {
        withContext(Dispatchers.IO) {
            dataList.forEach { data ->
                val post = Post(
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
                    genreIds = data.genreIds ?: ""
                )
                postRepository.save(post) // Save each post to the database
            }
        }
    }

    // Parse the posts from the API response
    private fun parsePosts(responseBody: String, mediaType: String, providerId: Int): List<Post> {
        val jsonObject = JSONObject(responseBody)
        val results = jsonObject.getJSONArray("results")
        val posts = mutableListOf<Post>()

        for (i in 0 until results.length()) {
            val result = results.getJSONObject(i)
            val post = Post(
                tmdbId = result.getInt("id"),
                type = mediaType,
                title = result.optString("title"),
                subscription = providerId.toString(),
                releaseDate = result.optString("release_date"),
                overview = result.optString("overview"),
                posterPath = result.optString("poster_path"),
                voteAverage = result.optDouble("vote_average"),
                voteCount = result.optInt("vote_count"),
                originalLanguage = result.optString("original_language"),
                originalTitle = result.optString("original_title"),
                popularity = result.optDouble("popularity"),
                genreIds = result.optJSONArray("genre_ids")?.joinToString(",") { it.toString() },
                videoKey = null // Not available at this stage
            )
            posts.add(post)
        }

        return posts
    }

    // Suspend function to fetch posts from the database
    suspend fun fetchPostsFromDatabase(limit: Int, offset: Int): List<Post> {
        return withContext(Dispatchers.IO) {
            postRepository.findAllByOrderByPostId(limit, offset)
        }
    }

    // Update like count for a post
    @Transactional
    fun updateLikeCount(postId: Int) {
        val post = postRepository.findById(postId).orElseThrow { Exception("Post not found") }
        // Assuming postLikeCount is an Int field in Post
        post.likeCount = (post.likeCount ?: 0) + 1
        postRepository.save(post)
    }

    // Select the best video key based on priority
    fun selectBestVideoKey(videos: List<Video>): String? {
        val priorityOrder = listOf("Short", "Trailer", "Teaser", "Featurette", "Clip")

        val filteredVideos = videos
            .filter { video -> video.isOfficial && priorityOrder.contains(video.type) }
            .sortedWith(compareBy({ priorityOrder.indexOf(it.type) }, { it.publishedAt }))

        return filteredVideos.lastOrNull()?.key
            ?: videos.filter { video -> video.type == "Trailer" && !video.isOfficial }
                .maxByOrNull { it.publishedAt }
                ?.key ?: videos.maxByOrNull { it.publishedAt }?.key
    }

    // Fetch videos from the database
    fun fetchVideosFromDatabase(limit: Int, offset: Int): List<Pair<String, Int>> {
        return postRepository.findAllByOrderByPostId(limit, offset)
            .mapNotNull { post ->
                post.videoKey?.let { Pair(it, post.postId ?: -1) }
            }
    }

    // Example of REST API to get posts with pagination
    suspend fun getPaginatedPostsAPI(limit: Int, offset: Int): List<Post> {
        return fetchPostsFromDatabase(limit, offset)
    }
}
