package com.api.postgres


import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlinx.serialization.Serializable


@JsonIgnoreProperties(ignoreUnknown = true)
data class ApiResponse(
    @JsonProperty("success") val success: Boolean,
    @JsonProperty("message") val message: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CommentResponse(
    @JsonProperty("success") val success: Boolean,
    @JsonProperty("message") val message: String,
    @JsonProperty("commentId") val commentId: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserRequest(
    @JsonProperty("user_dto") val userDto: UserDto,
    @JsonProperty("subscriptions") val subscriptions: List<Int>,
    @JsonProperty("genres") val genres: List<Int>,
    @JsonProperty("avoid_genres") val avoidGenres: List<Int>
)


@JsonIgnoreProperties(ignoreUnknown = true)
data class CommentDto(
    @JsonProperty("comment_id") val commentId: Int?,
    @JsonProperty("user_id") val userId: Int,
    @JsonProperty("username") val username: String,
    @JsonProperty("post_id") val postId: Int,
    @JsonProperty("content") val content: String,
    @JsonProperty("sentiment") val sentiment: String?,
    @JsonProperty("timestamp") val timestamp: String?,
    @JsonProperty("parent_comment_id") val parentCommentId: Int? = null,
    @JsonProperty("comment_type") val commentType: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CastDto(
    @JsonProperty("person_id") val personId: Int,
    @JsonProperty("name") val name: String,
    @JsonProperty("gender") val gender: Int,
    @JsonProperty("known_for_department") val knownForDepartment: String,
    @JsonProperty("character") val character: String,
    @JsonProperty("episode_count") val episodeCount: Int,
    @JsonProperty("order_index") val orderIndex: Int,
    @JsonProperty("popularity") val popularity: Double,
    @JsonProperty("profile_path") val profilePath: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CrewDto(
    @JsonProperty("person_id") val personId: Int,
    @JsonProperty("name") val name: String,
    @JsonProperty("gender") val gender: Int,
    @JsonProperty("known_for_department") val knownForDepartment: String,
    @JsonProperty("job") val job: String,
    @JsonProperty("department") val department: String,
    @JsonProperty("episode_count") val episodeCount: Int,
    @JsonProperty("popularity") val popularity: Double,
    @JsonProperty("profile_path") val profilePath: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PostDto(
    @JsonProperty("post_id") val postId: Int?,
    @JsonProperty("tmdb_id") val tmdbId: Int,
    @JsonProperty("type") val type: String,
    @JsonProperty("title") val title: String,
    @JsonProperty("subscription") val subscription: String,
    @JsonProperty("release_date") val releaseDate: String?,
    @JsonProperty("overview") val overview: String?,
    @JsonProperty("poster_path") val posterPath: String?,
    @JsonProperty("vote_average") val voteAverage: Double,
    @JsonProperty("vote_count") val voteCount: Int,
    @JsonProperty("original_language") val originalLanguage: String?,
    @JsonProperty("original_title") val originalTitle: String?,
    @JsonProperty("popularity") val popularity: Double,
    @JsonProperty("genre_ids") val genreIds: String,
    @JsonProperty("post_like_count") val postLikeCount: Int = 0,
    @JsonProperty("trailer_like_count") val trailerLikeCount: Int = 0,
    @JsonProperty("video_key") val videoKey: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserPostInteractionDto(
    @JsonProperty("interaction_id") val interactionId: Int?,
    @JsonProperty("user_id") val userId: Int,
    @JsonProperty("post_id") val postId: Int,
    @JsonProperty("start_timestamp") val startTimestamp: String,
    @JsonProperty("end_timestamp") val endTimestamp: String,
    @JsonProperty("like_state") val likeState: Boolean = false,
    @JsonProperty("save_state") val saveState: Boolean = false,
    @JsonProperty("comment_button_pressed") val commentButtonPressed: Boolean = false,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TrailerInteractionDto(
    @JsonProperty("interaction_id") val interactionId: Int?,
    @JsonProperty("user_id") val userId: Int,
    @JsonProperty("post_id") val postId: Int,
    @JsonProperty("start_timestamp") val startTimestamp: String,
    @JsonProperty("end_timestamp") val endTimestamp: String,
    @JsonProperty("replay_count") val replayCount: Int,
    @JsonProperty("is_muted") val isMuted: Boolean,
    @JsonProperty("like_state") val likeState: Boolean,
    @JsonProperty("save_state") val saveState: Boolean,
    @JsonProperty("comment_button_pressed") val commentButtonPressed: Boolean
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserDto(
    @JsonProperty("user_id") val userId: Int?,
    @JsonProperty("name") val name: String,
    @JsonProperty("username") val username: String,
    @JsonProperty("password") val password: String,
    @JsonProperty("email") val email: String,
    @JsonProperty("language") val language: String,
    @JsonProperty("region") val region: String,
    @JsonProperty("min_movie") val minMovie: Int?,
    @JsonProperty("max_movie") val maxMovie: Int?,
    @JsonProperty("min_tv") val minTV: Int?,
    @JsonProperty("max_tv") val maxTV: Int?,
    @JsonProperty("oldest_date") val oldestDate: String,
    @JsonProperty("recent_date") val recentDate: String,
    @JsonProperty("created_at") val createdAt: String,
    @JsonProperty("recent_login") val recentLogin: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserPreferencesDto(
    @JsonProperty("user_id") val userId: Int?,
    @JsonProperty("language") val language: String,
    @JsonProperty("region") val region: String,
    @JsonProperty("min_movie") val minMovie: Int?,
    @JsonProperty("max_movie") val maxMovie: Int?,
    @JsonProperty("min_tv") val minTV: Int?,
    @JsonProperty("max_tv") val maxTV: Int?,
    @JsonProperty("oldest_date") val oldestDate: String,
    @JsonProperty("recent_date") val recentDate: String,
    @JsonProperty("subscriptions") val subscriptions: List<Int>,
    @JsonProperty("genre_ids") val genreIds: List<Int>,
    @JsonProperty("avoid_genre_ids") val avoidGenreIds: List<Int>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ReplyCountDto(
    @JsonProperty("parent_id") val parentId: Int,
    @JsonProperty("reply_count") val replyCount: Int
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ClientMessage.UpdateSubscription::class, name = "UPDATE_SUBSCRIPTION")
)
sealed class ClientMessage {
    data class UpdateSubscription(
        val expandedSections: Set<Int>
    ) : ClientMessage()
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ServerMessage.NewRootComment::class, name = "NEW_ROOT_COMMENT"),
    JsonSubTypes.Type(value = ServerMessage.NewReply::class, name = "NEW_REPLY"),
    JsonSubTypes.Type(value = ServerMessage.ReplyCountUpdate::class, name = "REPLY_COUNT_UPDATE")
)
sealed class ServerMessage {
    data class NewRootComment(
        val comment: CommentDto
    ) : ServerMessage()

    data class NewReply(
        val comment: CommentDto,
        val parentId: Int
    ) : ServerMessage()

    data class ReplyCountUpdate(
        val commentId: Int,
        val newCount: Int
    ) : ServerMessage()
}

data class PersonDto(
    @JsonProperty("tmdb_id")
    val tmdbId: Int,
    @JsonProperty("name")
    val name: String,
    @JsonProperty("biography")
    val biography: String,
    @JsonProperty("birthday")
    val birthday: String?,
    @JsonProperty("deathday")
    val deathday: String?,
    @JsonProperty("place_of_birth")
    val placeOfBirth: String?,
    @JsonProperty("profile_path")
    val profilePath: String?,
    @JsonProperty("known_for_department")
    val knownForDepartment: String?,
    @JsonProperty("credit_ids")
    val creditIds: List<Int>
)

data class MediaDto(
    @JsonProperty("tmdb_id")
    val tmdbId: Int,
    @JsonProperty("title")
    val title: String,
    @JsonProperty("overview")
    val overview: String,
    @JsonProperty("release_date")
    val releaseDate: String?,
    @JsonProperty("runtime")
    val runtime: Int?,
    @JsonProperty("last_air_date")
    val lastAirDate: String?,
    @JsonProperty("in_production")
    val inProduction: Boolean?,
    @JsonProperty("next_episode_to_air")
    val nextEpisodeToAir: String?,
    @JsonProperty("number_of_episodes")
    val numberOfEpisodes: Int?,
    @JsonProperty("number_of_seasons")
    val numberOfSeasons: Int?,
    @JsonProperty("genres")
    val genres: List<String>,
    @JsonProperty("origin_countries")
    val originCountries: List<String>,
    @JsonProperty("production_companies")
    val productionCompanies: List<String>,
    @JsonProperty("collection_name")
    val collectionName: String?,
    @JsonProperty("poster_path")
    val posterPath: String?,
    @JsonProperty("media_type")
    val mediaType: String
)

data class InfoDto(
    @JsonProperty("tmdb_id")
    val tmdbId: Int,
    @JsonProperty("type")
    val type: String,
    @JsonProperty("start_timestamp")
    val startTimestamp: String,
    @JsonProperty("end_timestamp")
    val endTimestamp: String,
    @JsonProperty("user_id")
    val userId: Int
)

data class UserSubscriptionDto(
    @JsonProperty("user_id")
    val userId: Int,
    @JsonProperty("provider_id")
    val providerId: Int,
    @JsonProperty("provider_name")
    val providerName: String,
    @JsonProperty("priority")
    val priority: Int
)


data class UserGenreDto(
    @JsonProperty("user_id") val userId: Int,
    @JsonProperty("genre_id") val genreId: Int,
    @JsonProperty("genre_name") val genreName: String,
    @JsonProperty("priority") val priority: Int
)

data class InteractionStates(
    @JsonProperty("isLiked")val isLiked: Boolean,
    @JsonProperty("isSaved")val isSaved: Boolean
)


data class UserUpdate(
    @JsonProperty("language") val language: String? = null,
    @JsonProperty("region") val region: String? = null,
    @JsonProperty("minMovie") val minMovie: Int? = null,
    @JsonProperty("maxMovie")val maxMovie: Int? = null,
    @JsonProperty("minTV") val minTV: Int? = null,
    @JsonProperty("maxTV") val maxTV: Int? = null,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("username") val username: String? = null,
    @JsonProperty("email") val email: String? = null,
    @JsonProperty("oldestDate") val oldestDate: String? = null,
    @JsonProperty("recentDate") val recentDate: String? = null
)