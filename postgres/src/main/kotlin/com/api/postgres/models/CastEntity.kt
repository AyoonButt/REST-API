package com.api.postgres.models

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "cast")
data class CastEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @Column(name = "post_id")
    val postId: Int,

    @Column(name = "person_id")
    val personId: Int,

    @Column(name = "name")
    val name: String,

    @Column(name = "gender")
    val gender: Int,

    @Column(name = "known_for_department")
    val knownForDepartment: String,

    @Column(name = "character")
    val character: String,

    @Column(name = "episode_count")
    val episodeCount: Int,

    @Column(name = "order_index")
    val orderIndex: Int,

    @Column(name = "popularity")
    val popularity: Double,

    @Column(name = "profile_path")
    val profilePath: String?
) {
    // Default constructor for JPA
    constructor() : this(
        id = null,
        postId = 0,
        personId = 0,
        name = "",
        gender = 0,
        knownForDepartment = "",
        character = "",
        episodeCount = 0,
        orderIndex = 0,
        popularity = 0.0,
        profilePath = null
    )
}
