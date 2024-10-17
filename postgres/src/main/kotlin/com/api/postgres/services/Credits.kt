package com.api.postgres.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class Credits(
    private val castRepository: CastRepository,
    private val crewRepository: CrewRepository
) {

    // Function to parse and insert credits into the database
    @Transactional
    suspend fun parseAndInsertCredits(creditsJson: String, postId: Int) {
        val credits = JSONObject(creditsJson)

        // Parse and insert cast details
        val castArray = credits.getJSONArray("cast")
        for (i in 0 until castArray.length()) {
            val castMember = castArray.getJSONObject(i)
            val roles = castMember.getJSONArray("roles").getJSONObject(0)

            withContext(Dispatchers.IO) {
                castRepository.save(
                    CastEntity(
                        postId = postId,
                        personId = castMember.getInt("id"),
                        name = castMember.getString("name"),
                        gender = castMember.optInt("gender", -1),
                        knownForDepartment = castMember.getString("known_for_department"),
                        character = roles.optString("character", ""),
                        episodeCount = roles.optInt("episode_count", 0),
                        orderIndex = castMember.optInt("order", -1),
                        popularity = castMember.optDouble("popularity", 0.0).toBigDecimal(),
                        profilePath = castMember.optString("profile_path", null)
                    )
                )
            }
        }

        // Parse and insert crew details
        val crewArray = credits.getJSONArray("crew")
        for (i in 0 until crewArray.length()) {
            val crewMember = crewArray.getJSONObject(i)
            val jobs = crewMember.getJSONArray("jobs").getJSONObject(0)

            withContext(Dispatchers.IO) {
                crewRepository.save(
                    CrewEntity(
                        postId = postId,
                        personId = crewMember.getInt("id"),
                        name = crewMember.getString("name"),
                        gender = crewMember.optInt("gender", -1),
                        knownForDepartment = crewMember.getString("known_for_department"),
                        job = jobs.optString("job", ""),
                        department = crewMember.optString("department", ""),
                        episodeCount = jobs.optInt("episode_count", 0),
                        popularity = crewMember.optDouble("popularity", 0.0).toBigDecimal(),
                        profilePath = crewMember.optString("profile_path", null)
                    )
                )
            }
        }
    }

    // Function to load credits from the database
    @Transactional(readOnly = true)
    suspend fun loadCreditsFromDatabase(postId: Int): Map<String, List<Map<String, Any?>>> {
        val castList = withContext(Dispatchers.IO) {
            castRepository.findByPostId(postId).map { cast ->
                mapOf(
                    "personId" to cast.personId,
                    "name" to cast.name,
                    "gender" to cast.gender,
                    "knownForDepartment" to cast.knownForDepartment,
                    "character" to cast.character,
                    "episodeCount" to cast.episodeCount,
                    "orderIndex" to cast.orderIndex,
                    "popularity" to cast.popularity,
                    "profilePath" to cast.profilePath
                )
            }
        }

        val crewList = withContext(Dispatchers.IO) {
            crewRepository.findByPostId(postId).map { crew ->
                mapOf(
                    "personId" to crew.personId,
                    "name" to crew.name,
                    "gender" to crew.gender,
                    "knownForDepartment" to crew.knownForDepartment,
                    "job" to crew.job,
                    "department" to crew.department,
                    "episodeCount" to crew.episodeCount,
                    "popularity" to crew.popularity,
                    "profilePath" to crew.profilePath
                )
            }
        }

        return mapOf("cast" to castList, "crew" to crewList)
    }
}
