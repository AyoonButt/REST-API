package com.api.postgres.services


import com.api.postgres.CastDto
import com.api.postgres.CastProjection
import com.api.postgres.CrewDto
import com.api.postgres.CrewProjection
import com.api.postgres.repositories.CastRepository
import com.api.postgres.repositories.CrewRepository
import com.api.postgres.repositories.PostRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Service
class Credits(
    private val castRepository: CastRepository,
    private val crewRepository: CrewRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(Credits::class.java)

    private fun CastProjection.toDto() = CastDto(
        personId = personId,
        name = name,
        gender = gender,
        knownForDepartment = knownForDepartment,
        character = character,
        episodeCount = episodeCount,
        orderIndex = orderIndex,
        popularity = popularity,
        profilePath = profilePath
    )

    private fun CrewProjection.toDto() = CrewDto(
        personId = personId,
        name = name,
        gender = gender,
        knownForDepartment = knownForDepartment,
        job = job,
        department = department,
        episodeCount = episodeCount,
        popularity = popularity,
        profilePath = profilePath
    )

    @Transactional
    suspend fun parseAndInsertCredits(creditsResponse: JSONObject, tmdbId: Int) {
        withContext(Dispatchers.IO) {
            val creditsJson = creditsResponse

            // Parse and insert cast details
            val castArray = creditsJson.optJSONArray("cast")
            for (i in 0 until castArray.length()) {
                val castMemberJson = castArray.getJSONObject(i)

                castRepository.insertCast(
                    postId = tmdbId,
                    personId = castMemberJson.getInt("id"),
                    name = castMemberJson.getString("name"),
                    gender = castMemberJson.optInt("gender", -1),
                    knownForDepartment = "Acting",
                    character = castMemberJson.optString("character", ""),
                    episodeCount = castMemberJson.optInt("episode_count", 0),
                    orderIndex = castMemberJson.optInt("order", -1),
                    popularity = castMemberJson.optDouble("popularity", 0.0),
                    profilePath = castMemberJson.optString("profile_path", "")
                )
            }

            // Parse and insert crew details
            val crewArray = creditsJson.optJSONArray("crew")
            for (i in 0 until crewArray.length()) {
                val crewMemberJson = crewArray.getJSONObject(i)

                crewRepository.insertCrew(
                    postId = tmdbId,
                    personId = crewMemberJson.getInt("id"),
                    name = crewMemberJson.getString("name"),
                    gender = crewMemberJson.optInt("gender", -1),
                    knownForDepartment = crewMemberJson.getString("department"),
                    job = crewMemberJson.optString("job", ""),
                    department = crewMemberJson.optString("department", ""),
                    episodeCount = crewMemberJson.optInt("episode_count", 0),
                    popularity = crewMemberJson.optDouble("popularity", 0.0),
                    profilePath = crewMemberJson.optString("profile_path", "")
                )
            }
        }
    }

    @Transactional(readOnly = true)
    suspend fun loadCreditsFromDatabase(postId: Int): Map<String, List<Any>> {
        return withContext(Dispatchers.IO) {
            mapOf(
                "cast" to castRepository.findDtosByPostId(postId),
                "crew" to crewRepository.findDtosByPostId(postId)
            )
        }
    }


}