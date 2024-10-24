package com.api.postgres

import com.api.postgres.models.UserAvoidGenres
import com.api.postgres.models.UserEntity

data class UserInfo(
    val userId: Int,
    val name: String,
    val username: String,
    val email: String,
    val language: String,
    val region: String,
    val minMovie: Int,
    val maxMovie: Int,
    val minTV: Int,
    val maxTV: Int,
    val oldestDate: String?,
    val recentDate: String?,
    val createdAt: String?,
    var subscriptions: List<Int> = listOf(),
    var genres: List<Int> = listOf(),
    var avoidGenres: List<Int> = listOf()
)


data class UserParams(
    val language: String,
    val region: String,
    val minMovie: Int,
    val maxMovie: Int,
    val minTV: Int,
    val maxTV: Int,
    val oldestDate: String,   // Assuming it's a date in String format
    val recentDate: String    // Assuming it's a date in String format
)

data class UserRequest(
    val user: UserEntity,
    val subscriptions: List<Int>,
    val genres: List<Int>,
    var avoidGenres: List<Int>
)


