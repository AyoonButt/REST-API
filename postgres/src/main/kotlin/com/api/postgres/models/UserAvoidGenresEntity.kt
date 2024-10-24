package com.api.postgres.models

import jakarta.persistence.*
import java.io.Serializable

@Entity
@Table(name = "user_avoid_genres")
data class UserAvoidGenres(
    @EmbeddedId
    val id: UserAvoidGenreId,

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
data class UserAvoidGenreId(
    @Column(name = "user_id")
    val userId: Int,

    @Column(name = "genre_id")
    val genreId: Int
) : Serializable
