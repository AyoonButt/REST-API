package com.api.postgres.models

import jakarta.persistence.*

@Entity
@Table(name = "users")
data class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    val userId: Int? = null,

    @Column(name = "name", nullable = false, length = 255)
    var name: String,

    @Column(name = "username", nullable = false, unique = true, length = 255)
    var username: String,

    @Column(name = "email", nullable = false, length = 255)
    var email: String,

    @Column(name = "pswd", nullable = false, length = 255)
    var password: String,

    @Column(name = "language", nullable = false, length = 50)
    var language: String,

    @Column(name = "region", nullable = false, length = 50)
    var region: String,

    @Column(name = "min_movie")
    var minMovie: Int?,

    @Column(name = "max_movie")
    var maxMovie: Int?,

    @Column(name = "min_tv")
    var minTV: Int?,

    @Column(name = "max_tv")
    var maxTV: Int?,

    @Column(name = "oldest_date", length = 50)
    var oldestDate: String,

    @Column(name = "recent_date", length = 50)
    var recentDate: String,

    @Column(name = "recent_login", length = 75)
    var recentLogin: String,

    @Column(name = "created_at", length = 75)
    val createdAt: String
) {
    // Default constructor for JPA
    constructor() : this(
        userId = null,
        name = "",
        username = "",
        email = "",
        password = "",
        language = "",
        region = "",
        minMovie = 0,
        maxMovie = 0,
        minTV = 0,
        maxTV = 0,
        oldestDate = "",
        recentDate = "",
        recentLogin = "",
        createdAt = ""
    )
}
