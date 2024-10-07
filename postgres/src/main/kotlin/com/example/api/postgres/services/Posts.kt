class Posts {

    private suspend fun addPostsToDatabase(
        mediaType: String,
        dataList: List<Data>,
        providerId: Int
    ) {
        withContext(Dispatchers.IO) {
            transaction {
                dataList.forEach { data ->
                    Posts.insert {
                        it[tmdbId] = data.id
                        it[title] = data.title
                        it[releaseDate] = data.release_date
                        it[overview] = data.overview
                        it[posterPath] = data.poster_path
                        it[voteAverage] = data.vote_average
                        it[voteCount] = data.vote_count
                        it[genreIds] = data.genre_ids.joinToString(",")
                        it[originalLanguage] = data.original_language
                        it[originalTitle] = data.original_title
                        it[popularity] = data.popularity
                        it[type] = mediaType
                        it[subscription] =
                            providerId.toString()  // Store providerId in subscription column
                    }
                }
            }
        }
    }


    private suspend fun fetchPostsFromDatabase(limit: Int, offset: Int): List<Post> {
        return transaction {
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

    private fun updateLikeCount(postId: Int) {
        transaction {
            Posts.update({ Posts.postId eq postId }) {
                // Use a subquery to safely increment the count
                it[postLikeCount] =
                    (Posts.slice(postLikeCount).select { Posts.postId eq postId }
                        .singleOrNull()?.get(postLikeCount) ?: 0) + 1
            }
        }
    }

    fun selectBestVideoKey(videos: List<Video>): String? {
        // Define the priority order for video types
        val priorityOrder = listOf("Short", "Trailer", "Teaser", "Featurette", "Clip")
    
        // Filter and sort videos based on priority and official status
        val filteredVideos = videos
            .filter { video ->
                video.isOfficial && priorityOrder.contains(video.type)
            }
            .sortedWith(
                compareBy(
                    { priorityOrder.indexOf(it.type) },  // Prioritize by type
                    { it.publishedAt }  // Then by publication date (newest first)
                )
            )
    
        // If no official videos found, look for unofficial trailers or any video published most recently
        val bestVideo = filteredVideos.lastOrNull()
            ?: videos
                .filter { video ->
                    video.type == "Trailer" && !video.isOfficial
                }
                .maxByOrNull { it.publishedAt }
            ?: videos.maxByOrNull { it.publishedAt }
    
        return bestVideo?.key
    }

    private fun fetchVideosFromDatabase(limit: Int, offset: Int): List<Pair<String, Int>> {
        val videoData = mutableListOf<Pair<String, Int>>()

        transaction {
            val postsQuery = Posts
                .selectAll()
                .limit(limit, offset.toLong())

            for (post in postsQuery) {
                val postId = post[Posts.postId]
                val videoKey = post[Posts.videoKey]
                if (videoKey != null) {
                    videoData.add(Pair(videoKey, postId))
                }
            }
        }

        return videoData
    }
}
