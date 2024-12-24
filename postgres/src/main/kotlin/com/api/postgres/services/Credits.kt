package com.api.postgres.services



import com.api.postgres.models.CastEntity
import com.api.postgres.models.CrewEntity
import com.api.postgres.models.PostEntity
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
    private val postRepository: PostRepository
) {

    private val logger: Logger = LoggerFactory.getLogger(Credits::class.java)


    @Transactional
    suspend fun parseAndInsertCredits(creditsResponse: JSONObject, tmdbId: Int) {
        withContext(Dispatchers.IO) {
            val creditsJson = creditsResponse

            val postEntity: PostEntity? = postRepository.findById(tmdbId).orElse(null)

            // Parse and insert cast details
            val castArray = creditsJson.optJSONArray("cast")
            for (i in 0 until castArray.length()) {
                val castMemberJson = castArray.getJSONObject(i)


                // Create CastEntity
                val castEntity = CastEntity(
                    post = postEntity,
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

                // Log CastEntity details before saving
                logger.info("Cast Entity to insert: $castEntity")

                // Save CastEntity
                castRepository.save(castEntity)
            }

            // Parse and insert crew details
            val crewArray = creditsJson.optJSONArray("crew")
            for (i in 0 until crewArray.length()) {
                val crewMemberJson = crewArray.getJSONObject(i)

                // Create CrewEntity
                val crewEntity = CrewEntity(
                    post = postEntity,
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

                // Log CrewEntity details before saving
                logger.info("Crew Entity to insert: $crewEntity")

                // Save CrewEntity
                crewRepository.save(crewEntity)
            }
        }
    }






    // Function to load credits from the database
    @Transactional(readOnly = true)
    suspend fun loadCreditsFromDatabase(postId: Int): Map<String, List<Map<String, Any?>>> {
        val castList = withContext(Dispatchers.IO) {
            castRepository.findByPostPostId(postId).map { cast ->
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
            crewRepository.findByPostPostId(postId).map { crew ->
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
