package com.api.postgres.services

import com.api.postgres.models.Genre
import com.api.postgres.repositories.GenreRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class Genres(private val genreRepository: GenreRepository) {

    private val genresCache: MutableSet<Genre> = mutableSetOf()

    // Initialize with default genres
    init {
        insertGenres()
    }

    @Transactional
    fun insertGenres() {
        if (genresCache.isEmpty()) {
            val allGenres = genreRepository.findAll()
            if (allGenres.isEmpty()) {
                // Default genres to insert if the database is empty
                val defaultGenres = listOf(
                    Genre(genreId = 28, name = "Action"),
                    Genre(genreId = 12, name = "Adventure"),
                    Genre(genreId = 16, name = "Animation"),
                    Genre(genreId = 35, name = "Comedy"),
                    Genre(genreId = 80, name = "Crime"),
                    Genre(genreId = 99, name = "Documentary"),
                    Genre(genreId = 18, name = "Drama"),
                    Genre(genreId = 10751, name = "Family"),
                    Genre(genreId = 14, name = "Fantasy"),
                    Genre(genreId = 36, name = "History"),
                    Genre(genreId = 27, name = "Horror"),
                    Genre(genreId = 10402, name = "Music"),
                    Genre(genreId = 9648, name = "Mystery"),
                    Genre(genreId = 10749, name = "Romance"),
                    Genre(genreId = 878, name = "Science Fiction"),
                    Genre(genreId = 10770, name = "TV Movie"),
                    Genre(genreId = 53, name = "Thriller"),
                    Genre(genreId = 10752, name = "War"),
                    Genre(genreId = 37, name = "Western"),
                    Genre(genreId = 10759, name = "Action & Adventure"),
                    Genre(genreId = 10762, name = "Kids"),
                    Genre(genreId = 10763, name = "News"),
                    Genre(genreId = 10764, name = "Reality"),
                    Genre(genreId = 10765, name = "Sci-Fi & Fantasy"),
                    Genre(genreId = 10766, name = "Soap"),
                    Genre(genreId = 10767, name = "Talk"),
                    Genre(genreId = 10768, name = "War & Politics")
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

    fun filterGenres(query: String): List<Genre> {
        return if (query.isNotEmpty()) {
            genreRepository.findByNameContainingIgnoreCase(query).take(5)
        } else {
            emptyList()
        }
    }

    fun filterAvoidGenres(query: String): List<Genre> {
        return filterGenres(query) // Reusing filterGenres, modify if needed
    }

    @Transactional
    fun addGenre(genre: Genre) {
        genreRepository.save(genre)
        genresCache.add(genre)
    }
}
