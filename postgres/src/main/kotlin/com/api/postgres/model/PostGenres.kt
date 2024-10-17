package com.api.postgres.model

import jakarta.persistence.*

@Entity
@Table(name = "post_genres")
data class PostGenre(
    @EmbeddedId
    val id: PostGenreId
)

@Embeddable
data class PostGenreId(
    @Column(name = "post_id")
    val postId: Int,

    @Column(name = "genre_id")
    val genreId: Int
) : Serializable
