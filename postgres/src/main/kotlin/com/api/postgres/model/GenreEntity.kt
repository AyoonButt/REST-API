package com.api.postgres.model

import jakarta.persistence.*

@Entity
@Table(name = "genres")
data class Genre(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "genre_id")
    val genreId: Int? = null,

    @Column(name = "genre_name", length = 255, nullable = false)
    val genreName: String
)
