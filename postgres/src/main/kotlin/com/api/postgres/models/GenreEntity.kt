package com.api.postgres.models

import jakarta.persistence.*

@Entity
@Table(name = "genres")
data class GenreEntity(
    @Id
    @Column(name = "genre_id")
    val genreId: Int? = null,

    @Column(name = "genre_name", length = 255, nullable = false)
    val genreName: String
) {
    // Default constructor for JPA
    constructor() : this(
        genreId = null,
        genreName = "" // Provide a default value for genreName
    )
}
