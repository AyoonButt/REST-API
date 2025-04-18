package com.api.postgres.services

import com.api.postgres.UserGenreDto
import com.api.postgres.UserGenreProjection
import com.api.postgres.models.GenreEntity
import com.api.postgres.models.UserGenreId
import com.api.postgres.recommendations.UserVectorService
import com.api.postgres.repositories.GenreRepository
import com.api.postgres.repositories.UserAvoidGenresRepository
import com.api.postgres.repositories.UserGenresRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class Genres(private val genreRepository: GenreRepository,
             private val userGenresRepository: UserGenresRepository,
             private val userAvoidGenresRepository: UserAvoidGenresRepository,
             private val userVectorService: UserVectorService) {

    private val logger: Logger = LoggerFactory.getLogger(Genres::class.java)

    private val genresCache: MutableSet<GenreEntity> = mutableSetOf()

    private fun UserGenreProjection.toDto() = UserGenreDto(
        userId = userId,
        genreId = genreId,
        genreName = genreName,
        priority = priority
    )

    @Transactional
    fun insertGenres() {
        if (genresCache.isEmpty()) {
            val allGenres = genreRepository.findAll()
            if (allGenres.isEmpty()) {
                // Default genres to insert if the database is empty
                val defaultGenres = listOf(
                    GenreEntity(genreId = 28, genreName = "Action"),
                    GenreEntity(genreId = 12, genreName = "Adventure"),
                    GenreEntity(genreId = 16, genreName = "Animation"),
                    GenreEntity(genreId = 35, genreName = "Comedy"),
                    GenreEntity(genreId = 80, genreName = "Crime"),
                    GenreEntity(genreId = 99, genreName = "Documentary"),
                    GenreEntity(genreId = 18, genreName = "Drama"),
                    GenreEntity(genreId = 10751, genreName = "Family"),
                    GenreEntity(genreId = 14, genreName = "Fantasy"),
                    GenreEntity(genreId = 36, genreName = "History"),
                    GenreEntity(genreId = 27, genreName = "Horror"),
                    GenreEntity(genreId = 10402, genreName = "Music"),
                    GenreEntity(genreId = 9648, genreName = "Mystery"),
                    GenreEntity(genreId = 10749, genreName = "Romance"),
                    GenreEntity(genreId = 878, genreName = "Science Fiction"),
                    GenreEntity(genreId = 10770, genreName = "TV Movie"),
                    GenreEntity(genreId = 53, genreName = "Thriller"),
                    GenreEntity(genreId = 10752, genreName = "War"),
                    GenreEntity(genreId = 37, genreName = "Western"),
                    GenreEntity(genreId = 10759, genreName = "Action & Adventure"),
                    GenreEntity(genreId = 10762, genreName = "Kids"),
                    GenreEntity(genreId = 10763, genreName = "News"),
                    GenreEntity(genreId = 10764, genreName = "Reality"),
                    GenreEntity(genreId = 10765, genreName = "Sci-Fi & Fantasy"),
                    GenreEntity(genreId = 10766, genreName = "Soap"),
                    GenreEntity(genreId = 10767, genreName = "Talk"),
                    GenreEntity(genreId = 10768, genreName = "War & Politics")
                )

                // Insert default genres into the database
                genreRepository.saveAll(defaultGenres)

                // Add to the in-memory set
                genresCache.addAll(defaultGenres)
            } else {
                genresCache.addAll(allGenres)
            }
        }
    }

    @Transactional
    fun filterGenres(query: String): List<GenreEntity> {
        return if (query.isNotEmpty()) {
            genreRepository.findByGenreNameContainingIgnoreCase(query).take(5)
        } else {
            emptyList()
        }
    }

    @Transactional
    fun filterAvoidGenres(query: String): List<GenreEntity> {
        return filterGenres(query) // Reusing filterGenres, modify if needed
    }

    @Transactional
    fun addGenre(genre: GenreEntity) {
        genreRepository.save(genre)
        genresCache.add(genre)
    }

    @Transactional
    suspend fun fetchGenreIdsByNames(names: List<String>): List<Int> {
        return withContext(Dispatchers.IO) {
            if (names.isEmpty()) {
                return@withContext emptyList()
            }

            // Fetch genre entities from the database
            val genres = genreRepository.findAllGenreIdsByNames(names)

            // Create a map of genre names to IDs
            val genreNameToIdMap = genres.associateBy(
                keySelector = { it.genreName.lowercase() },
                valueTransform = { it.genreId }
            )

            // Map input names to IDs, maintaining order and handling case-insensitivity
            names.mapNotNull { name ->
                genreNameToIdMap[name.lowercase()]
            }
        }
    }

    @Transactional
    suspend fun getUserGenres(userId: Int): Result<List<UserGenreDto>> = withContext(Dispatchers.IO) {
        try {
            val projections = userGenresRepository.findProjectedByUserIdOrderByPriority(userId)
            Result.success(projections.map { it.toDto() })
        } catch (e: Exception) {
            logger.error("Error getting user genres for userId $userId: ${e.message}")
            Result.failure(e)
        }
    }

    @Transactional
    suspend fun updateUserGenres(userId: Int, newGenres: List<UserGenreDto>): Result<List<UserGenreDto>> = withContext(Dispatchers.IO) {
        try {
            // Get current genres using projection
            val currentGenres = userGenresRepository.findProjectedByUserIdOrderByPriority(userId)

            // Find genres to remove
            val newGenreIds = newGenres.map { it.genreId }
            val genresToRemove = currentGenres.filter {
                !newGenreIds.contains(it.genreId)
            }

            // Remove old genres
            genresToRemove.forEach { projection ->
                userGenresRepository.deleteById(UserGenreId(projection.userId, projection.genreId))
            }

            // Update or add genres
            newGenres.forEach { dto ->
                userGenresRepository.insertUserGenre(
                    userId = userId,
                    genreId = dto.genreId,
                    priority = dto.priority
                )
            }

            // Return updated genres using projection
            val updatedProjections = userGenresRepository.findProjectedByUserIdOrderByPriority(userId)

            userVectorService.regenerateUserVectorAfterPreferenceChange(userId)

            Result.success(updatedProjections.map { it.toDto() })


        } catch (e: Exception) {
            logger.error("Error updating user genres for userId $userId: ${e.message}")
            Result.failure(e)
        }
    }

    @Transactional
    suspend fun getUserAvoidGenres(userId: Int): Result<List<GenreEntity>> = withContext(Dispatchers.IO) {
        try {
            val avoidGenres = userAvoidGenresRepository.findGenresByUserId(userId)
            Result.success(avoidGenres)
        } catch (e: Exception) {
            logger.error("Error getting user avoid genres for userId $userId: ${e.message}")
            Result.failure(e)
        }
    }

    @Transactional
    suspend fun updateUserAvoidGenres(userId: Int, avoidGenreIds: List<Int>): Result<List<GenreEntity>> = withContext(Dispatchers.IO) {
        try {
            // Get current avoid genres
            val currentAvoidGenres = userAvoidGenresRepository.findGenreIdsByUserId(userId)

            // Find genres to remove
            val genresToRemove = currentAvoidGenres.filter {
                !avoidGenreIds.contains(it)
            }

            // Find genres to add
            val genresToAdd = avoidGenreIds.filter {
                !currentAvoidGenres.contains(it)
            }

            // Remove old avoid genres
            genresToRemove.forEach { genreId ->
                userAvoidGenresRepository.deleteByUserIdAndGenreId(userId, genreId)
            }

            // Add new avoid genres
            genresToAdd.forEach { genreId ->
                userAvoidGenresRepository.insertUserAvoidGenre(
                    userId = userId,
                    genreId = genreId
                )
            }

            // Update user vector for recommendations
            userVectorService.regenerateUserVectorAfterPreferenceChange(userId)

            // Return updated avoid genres
            val updatedAvoidGenres = userAvoidGenresRepository.findGenresByUserId(userId)
            Result.success(updatedAvoidGenres)
        } catch (e: Exception) {
            logger.error("Error updating user avoid genres for userId $userId: ${e.message}")
            Result.failure(e)
        }
    }


}
