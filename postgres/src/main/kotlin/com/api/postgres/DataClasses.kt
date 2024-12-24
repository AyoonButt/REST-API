package com.api.postgres


import com.api.postgres.models.UserEntity
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class UserInfo @JsonCreator constructor(
    @JsonProperty("userId") val userId: Int,
    @JsonProperty("name") val name: String,
    @JsonProperty("username") val username: String,
    @JsonProperty("email") val email: String,
    @JsonProperty("language") val language: String,
    @JsonProperty("region") val region: String,
    @JsonProperty("minMovie") val minMovie: Int,
    @JsonProperty("maxMovie") val maxMovie: Int,
    @JsonProperty("minTV") val minTV: Int,
    @JsonProperty("maxTV") val maxTV: Int,
    @JsonProperty("oldestDate") val oldestDate: String?,
    @JsonProperty("recentDate") val recentDate: String?,
    @JsonProperty("createdAt") val createdAt: String?,
    @JsonProperty("subscriptions") var subscriptions: List<Int> = listOf(),
    @JsonProperty("genres") var genres: List<Int> = listOf(),
    @JsonProperty("avoidGenres") var avoidGenres: List<Int> = listOf()
)

data class UserParams @JsonCreator constructor(
    @JsonProperty("language") val language: String,
    @JsonProperty("region") val region: String,
    @JsonProperty("minMovie") val minMovie: Int,
    @JsonProperty("maxMovie") val maxMovie: Int,
    @JsonProperty("minTV") val minTV: Int,
    @JsonProperty("maxTV") val maxTV: Int,
    @JsonProperty("oldestDate") val oldestDate: String,  // Assuming it's a date in String format
    @JsonProperty("recentDate") val recentDate: String    // Assuming it's a date in String format
)

data class UserRequest @JsonCreator constructor(
    @JsonProperty("user") val user: UserEntity,
    @JsonProperty("subscriptions") val subscriptions: List<Int>,
    @JsonProperty("genres") val genres: List<Int>,
    @JsonProperty("avoidGenres") var avoidGenres: List<Int>
)

data class UserUpdateRequest @JsonCreator constructor(
    @JsonProperty("userData") val userData: UserEntity,  // The user entity with updated information
    @JsonProperty("subscriptions") val subscriptions: List<Int>,  // List of subscription provider IDs
    @JsonProperty("genres") val genres: List<Int>,  // List of genre IDs
    @JsonProperty("avoidGenres") val avoidGenres: List<Int>  // List of avoided genre IDs
)

data class ReplyRequest @JsonCreator constructor(
    @JsonProperty("postId") val postId: Int,
    @JsonProperty("content") val content: String,
    @JsonProperty("sentiment") val sentiment: String? = null
)

