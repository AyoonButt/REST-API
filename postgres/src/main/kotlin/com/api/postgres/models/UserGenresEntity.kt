package com.api.postgres.models

import jakarta.persistence.*
import java.io.Serializable

import jakarta.persistence.*


@Entity
@Table(name = "user_genres")
data class UserGenres(
    @EmbeddedId
    val id: UserGenreId,

    @ManyToOne
    @MapsId("userId") // Maps the userId part of the composite key
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    val user: UserEntity, // Assuming UserEntity exists

    @ManyToOne
    @MapsId("genreId") // Maps the genreId part of the composite key
    @JoinColumn(name = "genre_id", referencedColumnName = "genre_id")
    val genre: GenreEntity // Assuming GenreEntity exists
)

@Embeddable
data class UserGenreId(
    @Column(name = "user_id")
    val userId: Int,

    @Column(name = "genre_id")
    val genreId: Int
) : Serializable
