package com.api.postgres.models

import jakarta.persistence.*
import org.hibernate.annotations.DynamicUpdate

@Entity
@DynamicUpdate
@Table(name = "cast_members")
data class CastEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @ManyToOne
    @JoinColumn(name = "tmdb_id", referencedColumnName = "tmdb_id")  // Foreign key mapping
    val post: PostEntity? = null,

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
    constructor() : this(
        id = null,
        post = null,  // Initialize with null
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
