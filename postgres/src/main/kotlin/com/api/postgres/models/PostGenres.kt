package com.api.postgres.models

import jakarta.persistence.*
import java.io.Serializable


@Entity
@Table(name = "post_genres")
data class PostGenres(
    @EmbeddedId
    val id: PostGenreId,

    @ManyToOne
    @MapsId("postId") // Maps the postId part of the composite key
    @JoinColumn(name = "post_id", referencedColumnName = "post_id")
    val post: PostEntity, // Assuming PostEntity exists

    @ManyToOne
    @MapsId("genreId") // Maps the genreId part of the composite key
    @JoinColumn(name = "genre_id", referencedColumnName = "genre_id")
    val genre: GenreEntity // Assuming GenreEntity exists
)

@Embeddable
data class PostGenreId(
    @Column(name = "post_id")
    val postId: Int,

    @Column(name = "genre_id")
    val genreId: Int
) : Serializable
