package com.api.postgres.model

import jakarta.persistence.*
import java.io.Serializable

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    val userId: Int? = null,

    @Column(name = "name", nullable = false, length = 255)
    val name: String,

    @Column(name = "username", nullable = false, unique = true, length = 255)
    val username: String,

    @Column(name = "email", nullable = false, length = 255)
    val email: String,

    @Column(name = "pswd", nullable = false, length = 255)
    val password: String,

    @Column(name = "language", nullable = false, length = 50)
    val language: String,

    @Column(name = "region", nullable = false, length = 50)
    val region: String,

    @Column(name = "min_movie")
    val minMovie: Int,

    @Column(name = "max_movie")
    val maxMovie: Int,

    @Column(name = "min_tv")
    val minTV: Int,

    @Column(name = "max_tv")
    val maxTV: Int,

    @Column(name = "oldest_date", length = 50)
    val oldestDate: String,

    @Column(name = "recent_date", length = 50)
    val recentDate: String,

    @Column(name = "recent_login", length = 75)
    val recentLogin: String,

    @Column(name = "created_at", length = 75)
    val createdAt: String
)
