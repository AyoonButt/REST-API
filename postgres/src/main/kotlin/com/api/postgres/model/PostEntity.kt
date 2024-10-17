package com.api.postgres.model

import jakarta.persistence.*

@Entity
@Table(name = "posts")
data class Post(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    val postId: Int? = null,

    @Column(name = "tmdb_id")
    val tmdbId: Int,

    @Column(name = "post_like_count")
    val postLikeCount: Int,

    @Column(name = "trailer_like_count")
    val trailerLikeCount: Int,

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
)
