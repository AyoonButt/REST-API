package com.api.postgres.models

import jakarta.persistence.*

@Entity
@Table(name = "posts")
data class PostEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    val postId: Int? = null,

    @Column(name = "tmdb_id")
    val tmdbId: Int,

    @Column(name = "post_like_count")
    var postLikeCount: Int,

    @Column(name = "trailer_like_count")
    var trailerLikeCount: Int,

    @Column(name = "type", length = 50)
    val type: String,

    @Column(name = "title", length = 255)
    val title: String,

    @Column(name = "subscription", length = 255)
    val subscription: String,

    @Column(name = "release_date", length = 100)
    val releaseDate: String,

    @Column(name = "overview")
    val overview: String,

    @Column(name = "poster_path")
    val posterPath: String,

    @Column(name = "vote_average")
    val voteAverage: Double,

    @Column(name = "vote_count")
    val voteCount: Int,

    @Column(name = "original_language", length = 50)
    val originalLanguage: String,

    @Column(name = "original_title", length = 255)
    val originalTitle: String,

    @Column(name = "popularity")
    val popularity: Double,

    @Column(name = "genre_ids", length = 255)
    val genreIds: String,

    @Column(name = "video_key", length = 100)
    val videoKey: String
) {
    // Default constructor for JPA
    constructor() : this(
        postId = null,
        tmdbId = 0, // Provide a default value for tmdbId
        postLikeCount = 0, // Provide a default value for postLikeCount
        trailerLikeCount = 0, // Provide a default value for trailerLikeCount
        type = "", // Provide a default value for type
        title = "", // Provide a default value for title
        subscription = "", // Provide a default value for subscription
        releaseDate = "", // Provide a default value for releaseDate
        overview = "", // Provide a default value for overview
        posterPath = "", // Provide a default value for posterPath
        voteAverage = 0.0, // Provide a default value for voteAverage
        voteCount = 0, // Provide a default value for voteCount
        originalLanguage = "", // Provide a default value for originalLanguage
        originalTitle = "", // Provide a default value for originalTitle
        popularity = 0.0, // Provide a default value for popularity
        genreIds = "", // Provide a default value for genreIds
        videoKey = "" // Provide a default value for videoKey
    )
}
