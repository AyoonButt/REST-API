package com.api.postgres.model

import jakarta.persistence.*

@Entity
@Table(name = "user_genres")
data class UserGenre(
    @EmbeddedId
    val id: UserGenreId
)

@Embeddable
data class UserGenreId(
    @Column(name = "user_id")
    val userId: Int,

    @Column(name = "genre_id")
    val genreId: Int
) : Serializable
