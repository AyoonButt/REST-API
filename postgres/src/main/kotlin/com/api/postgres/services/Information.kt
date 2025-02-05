package com.api.postgres.services

import com.api.postgres.InfoDto
import com.api.postgres.InfoItemProjection
import com.api.postgres.MediaDto
import com.api.postgres.PersonDto
import com.api.postgres.models.ViewingSession
import com.api.postgres.repositories.InfoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class Information(
    private val infoRepository: InfoRepository
) {

    private fun InfoItemProjection.toDto() = InfoDto(
        tmdbId = tmdbId,
        type = type,
        userId = userId,
        startTimestamp = sessions.first().startTimestamp,
        endTimestamp = sessions.first().endTimestamp
    )

    private val logger: Logger = LoggerFactory.getLogger(Information::class.java)

    @Transactional
    suspend fun insertInfo(infoDto: InfoDto): Unit = withContext(Dispatchers.IO) {
        try {
            val existingInfoProjection = infoRepository.findByTmdbIdAndTypeAndUserUserId(
                infoDto.tmdbId,
                infoDto.type,
                infoDto.userId
            )

            val session = ViewingSession(
                startTimestamp = infoDto.startTimestamp,
                endTimestamp = infoDto.endTimestamp
            )

            if (existingInfoProjection != null) {
                // Delete existing sessions
                infoRepository.deleteExistingSessions(
                    infoDto.tmdbId,
                    infoDto.type,
                    infoDto.userId
                )

                // Get current sessions and add new one
                val sessions = existingInfoProjection.sessions.toMutableList().apply {
                    add(session)
                }

                // Insert all sessions
                sessions.forEachIndexed { index, session ->
                    infoRepository.insertSession(
                        infoDto.tmdbId,
                        infoDto.type,
                        infoDto.userId,
                        index,
                        session.startTimestamp,
                        session.endTimestamp
                    )
                }
            } else {
                // Insert new info first
                infoRepository.insertNewInfo(
                    infoDto.tmdbId,
                    infoDto.type,
                    infoDto.userId
                )

                // Then insert the first session
                infoRepository.insertSession(
                    infoDto.tmdbId,
                    infoDto.type,
                    infoDto.userId,
                    0,
                    session.startTimestamp,
                    session.endTimestamp
                )
            }
        } catch (e: Exception) {
            logger.error("Error saving info for user ${infoDto.userId}: ${e.message}", e)
            throw e
        }
    }


    @Transactional
    fun createDto(tmdbId: Int, type: String, jsonData: String): Any {
        val data = JSONObject(jsonData)

        return when(type) {
            "person" -> createPersonDto(tmdbId, data)
            "movie", "tv" -> createMediaDto(tmdbId, type, data)
            else -> throw IllegalArgumentException("Invalid type: $type")
        }
    }

    @Transactional
    private fun createPersonDto(tmdbId: Int, data: JSONObject): PersonDto {
        val creditIds = data.getJSONObject("combined_credits")
            .getJSONArray("cast")
            .let { array ->
                (0 until array.length()).map { i ->
                    array.getJSONObject(i).getInt("id")
                }
            }

        return PersonDto(
            tmdbId = tmdbId,
            name = data.getString("name"),
            biography = data.getString("biography"),
            birthday = if (data.isNull("birthday")) null else data.getString("birthday"),
            deathday = if (data.isNull("deathday")) null else data.getString("deathday"),
            placeOfBirth = if (data.isNull("place_of_birth")) null else data.getString("place_of_birth"),
            profilePath = if (data.isNull("profile_path")) null else data.getString("profile_path"),
            knownForDepartment = if (data.isNull("known_for_department")) null else data.getString("known_for_department"),
            creditIds = creditIds
        )
    }

    @Transactional
    private fun createMediaDto(tmdbId: Int, type: String, data: JSONObject): MediaDto {
        val isMovie = type == "movie"

        return MediaDto(
            tmdbId = tmdbId,
            title = if (isMovie) data.getString("title") else data.getString("name"),
            overview = data.getString("overview"),
            releaseDate = if (isMovie) data.optString("release_date") else data.optString("first_air_date"),
            runtime = if (isMovie) {
                data.optInt("runtime")
            } else {
                data.optJSONArray("episode_run_time")?.let {
                    if (it.length() > 0) it.getInt(0) else null
                }
            },
            lastAirDate = if (!isMovie) {
                if (data.isNull("last_air_date")) null else data.getString("last_air_date")
            } else null,
            inProduction = if (!isMovie) data.optBoolean("in_production") else null,
            nextEpisodeToAir = if (!isMovie) {
                data.optJSONObject("next_episode_to_air")?.optString("name")
            } else null,
            numberOfEpisodes = if (!isMovie) data.optInt("number_of_episodes") else null,
            numberOfSeasons = if (!isMovie) data.optInt("number_of_seasons") else null,
            genres = data.getJSONArray("genres")
                .let { array ->
                    (0 until array.length()).map { i ->
                        array.getJSONObject(i).getString("name")
                    }
                },
            originCountries = data.getJSONArray("origin_country")
                .let { array ->
                    (0 until array.length()).map { i ->
                        array.getString(i)
                    }
                },
            productionCompanies = data.getJSONArray("production_companies")
                .let { array ->
                    (0 until array.length()).map { i ->
                        array.getJSONObject(i).getString("name")
                    }
                },
            collectionName = data.optJSONObject("belongs_to_collection")?.optString("name"),
            posterPath = data.optString("poster_path"),
            mediaType = type
        )
    }

}
