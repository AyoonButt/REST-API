package com.api.postgres.services

import com.api.postgres.models.GenreEntity
import com.api.postgres.repositories.GenreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class Genres(private val genreRepository: GenreRepository) {

    private val genresCache: MutableSet<GenreEntity> = mutableSetOf()

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





}
