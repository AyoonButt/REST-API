package com.api.postgres


interface CommentProjection {
    val commentId: Int?
    val userId: Int
    val username: String
    val postId: Int
    val content: String
    val sentiment: String?
    val timestamp: String?
    val parentCommentId: Int?
}

interface CastProjection {
    val personId: Int
    val name: String
    val gender: Int
    val knownForDepartment: String
    val character: String
    val episodeCount: Int
    val orderIndex: Int
    val popularity: Double
    val profilePath: String
}

interface CrewProjection {
    val personId: Int
    val name: String
    val gender: Int
    val knownForDepartment: String
    val job: String
    val department: String
    val episodeCount: Int
    val popularity: Double
    val profilePath: String
}
interface UserPostInteractionProjection {
    val interactionId: Int?
    val userId: Int
    val postId: Int
    val timeSpentOnPost: Long
    val likeState: Boolean
    val saveState: Boolean
    val commentButtonPressed: Boolean
    val commentMade: Boolean
    val timestamp: String
}

interface PostProjection {
    val postId: Int?
    val tmdbId: Int
    val type: String
    val title: String
    val subscription: String
    val releaseDate: String?
    val overview: String?
    val posterPath: String?
    val voteAverage: Double
    val voteCount: Int
    val originalLanguage: String?
    val originalTitle: String?
    val popularity: Double
    val genreIds: String
    val postLikeCount: Int
    val trailerLikeCount: Int
    val videoKey: String
}

interface TrailerInteractionProjection {
    val interactionId: Int
    val userId: Int
    val postId: Int
    val timeSpent: Long
    val replayCount: Int
    val isMuted: Boolean
    val likeState: Boolean
    val saveState: Boolean
    val commentButtonPressed: Boolean
    val commentMade: Boolean
    val timestamp: String
}
interface UserProjection {
    val userId: Int
    val name: String
    val username: String
    val password: String
    val email: String
    val language: String?
    val region: String?
    val minMovie: Int?
    val maxMovie: Int?
    val minTV: Int?
    val maxTV: Int?
    val oldestDate: String?
    val recentDate: String?
    val createdAt: String?
    val recentLogin: String?
}

interface UserPreferencesProjection {
    val userId: Int?
    val language: String
    val region: String
    val minMovie: Int?
    val maxMovie: Int?
    val minTV: Int?
    val maxTV: Int?
    val oldestDate: String
    val recentDate: String
    val subscriptions: List<Int>
    val genreIds: List<Int>
    val avoidGenreIds: List<Int>
}
