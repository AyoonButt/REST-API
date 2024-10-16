package com.example.api.postgres.services

class Posts {

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
    suspend fun addPostsToDatabase(mediaType: String, dataList: List<Post>, providerId: Int) {
        withContext(Dispatchers.IO) {
            transaction {
                dataList.forEach { data ->
                    Posts.insert {
                        it[tmdbId] = data.tmdbId
                        it[title] = data.title
                        it[releaseDate] = data.releaseDate
                        it[overview] = data.overview
                        it[posterPath] = data.posterPath
                        it[voteAverage] = data.voteAverage
                        it[voteCount] = data.voteCount
                        it[genreIds] = data.genreIds ?: ""
                        it[originalLanguage] = data.originalLanguage
                        it[originalTitle] = data.originalTitle
                        it[popularity] = data.popularity
                        it[type] = mediaType
                        it[subscription] = providerId.toString()
                    }
                }
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
            transaction {
                Posts.selectAll()
                    .limit(limit, offset.toLong())
                    .map { row ->
                        Post(
                            postId = row[Posts.postId],
                            tmdbId = row[Posts.tmdbId],
                            type = row[Posts.type],
                            title = row[Posts.title],
                            subscription = row[Posts.subscription],
                            releaseDate = row[Posts.releaseDate],
                            overview = row[Posts.overview],
                            posterPath = row[Posts.posterPath],
                            voteAverage = row[Posts.voteAverage],
                            voteCount = row[Posts.voteCount],
                            originalLanguage = row[Posts.originalLanguage],
                            originalTitle = row[Posts.originalTitle],
                            popularity = row[Posts.popularity],
                            genreIds = row[Posts.genreIds],
                            videoKey = row[Posts.videoKey]
                        )
                    }
            }
        }
    }

    // Update like count for a post
    fun updateLikeCount(postId: Int) {
        transaction {
            Posts.update({ Posts.postId eq postId }) {
                it[postLikeCount] =
                    (Posts.slice(postLikeCount).select { Posts.postId eq postId }
                        .singleOrNull()?.get(postLikeCount) ?: 0) + 1
            }
        }
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
        val videoData = mutableListOf<Pair<String, Int>>()

        transaction {
            Posts.selectAll()
                .limit(limit, offset.toLong())
                .forEach { post ->
                    val postId = post[Posts.postId]
                    val videoKey = post[Posts.videoKey]
                    if (videoKey != null) {
                        videoData.add(Pair(videoKey, postId))
                    }
                }
        }

        return videoData
    }

    // Example of REST API to get posts with pagination
    suspend fun getPaginatedPostsAPI(limit: Int, offset: Int): List<Post> {
        return fetchPostsFromDatabase(limit, offset)
    }
}
