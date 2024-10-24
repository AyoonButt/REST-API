package com.api.postgres.models

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "crew")
data class CrewEntity(
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

    @Column(name = "job")
    val job: String,

    @Column(name = "department")
    val department: String,

    @Column(name = "episode_count")
    val episodeCount: Int,

    @Column(name = "popularity")
    val popularity: BigDecimal,

    @Column(name = "profile_path")
    val profilePath: String?
)
