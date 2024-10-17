package com.api.postgres

data class UserInfo(
    val userId: Int,
    val name: String,
    val username: String,
    val email: String,
    val language: String,
    val region: String,
    val minMovie: Int,
    val maxMovie: Int,
    val minTV: Int,
    val maxTV: Int,
    val oldestDate: String?,
    val recentDate: String?,
    val createdAt: String?,
    var subscriptions: List<Int> = listOf(),
    var genres: List<Int> = listOf()
)

data class UserData(
    val name: String,
    val username: String,
    val email: String,
    val password: String,
    val selectedLanguage: String,
    val selectedRegion: String,
    val selectedMinMovie: Int,
    val selectedMaxMovie: Int,
    val selectedMinTV: Int,
    val selectedMaxTV: Int,
    val genresToAvoid: List<String>,
    val subscriptionNames: List<String>,
    val genreNames: List<String>
)

data class TrailerInteractionData(
    val userId: Int,
    val postId: Int,
    val playTime: Int,
    val replayCount: Int,
    val isMuted: Boolean,
    val likeState: Boolean,
    val saveState: Boolean,
    val commentButtonPressed: Boolean,
    val commentMade: Boolean
)

data class Genre(
    val id: Int,
     var name: String
)

data class Post(
    val postId: Int,
    val tmdbId: Int,
    val type: String,
    val title: String?,
    val subscription: String?,
    val releaseDate: String?,
    val overview: String?,
    val posterPath: String?,
    val voteAverage: Double?,
    val voteCount: Int?,
    val originalLanguage: String?,
    val originalTitle: String?,
    val popularity: Double?,
    val genreIds: String?,
    val videoKey: String?
)

data class Video(
    val key: String,
    val type: String,
    val isOfficial: Boolean,
    val publishedAt: String
)

data class Comment(
    val commentId: Int,
    val postId: Int,
    val userId: Int,
    val username: String,
    val content: String,
    val sentiment: String
)


