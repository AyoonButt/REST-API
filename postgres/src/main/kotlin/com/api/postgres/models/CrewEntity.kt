package com.api.postgres.models

import jakarta.persistence.*
import org.hibernate.annotations.DynamicUpdate
import java.util.Optional


@Entity
@DynamicUpdate
@Table(name = "crew")
data class CrewEntity(
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

    @Column(name = "job")
    val job: String,

    @Column(name = "department")
    val department: String,

    @Column(name = "episode_count")
    val episodeCount: Int,

    @Column(name = "popularity")
    val popularity: Double,

    @Column(name = "profile_path")
    val profilePath: String?
) {
    constructor() : this(
        id = null,
        post = PostEntity(),
        personId = 0,
        name = "",
        gender = 0,
        knownForDepartment = "",
        job = "",
        department = "",
        episodeCount = 0,
        popularity = 0.0,
        profilePath = null
    )
}