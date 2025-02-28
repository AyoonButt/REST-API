package com.api.postgres.controllers

import com.api.postgres.UserGenreDto
import com.api.postgres.models.GenreEntity
import com.api.postgres.services.Genres
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/genres")
class GenresController(
    private val genresService: Genres
) {
    private val logger: Logger = LoggerFactory.getLogger(GenresController::class.java)

    // Endpoint to filter genres based on a query string
    @GetMapping("/filter")
    fun filterGenres(@RequestParam query: String): ResponseEntity<List<GenreEntity>> {
        logger.info("Filtering genres with query: $query")
        val genres = genresService.filterGenres(query)
        return if (genres.isNotEmpty()) {
            logger.info("Found ${genres.size} genres matching query: $query")
            ResponseEntity.ok(genres)
        } else {
            logger.warn("No genres found for query: $query")
            ResponseEntity.status(HttpStatus.NO_CONTENT).build()
        }
    }

    // Endpoint to filter genres to avoid based on a query string
    @GetMapping("/filter/avoid")
    fun filterAvoidGenres(@RequestParam query: String): ResponseEntity<List<GenreEntity>> {
        logger.info("Filtering genres to avoid with query: $query")
        val genres = genresService.filterAvoidGenres(query)
        return if (genres.isNotEmpty()) {
            logger.info("Found ${genres.size} genres to avoid for query: $query")
            ResponseEntity.ok(genres)
        } else {
            logger.warn("No genres to avoid found for query: $query")
            ResponseEntity.status(HttpStatus.NO_CONTENT).build()
        }
    }

    // Endpoint to add a new genre
    @PostMapping("/add")
    fun addGenre(@RequestBody genre: GenreEntity): ResponseEntity<String> {
        logger.info("Adding new genre: ${genre.genreName}")
        genresService.addGenre(genre)
        logger.info("Genre added successfully: ${genre.genreName}")
        return ResponseEntity.status(HttpStatus.CREATED).body("Genre added successfully: ${genre.genreName}")
    }

    // Endpoint to insert default genres (can be used during setup)
    @PostMapping("/insertDefaults")
    fun insertDefaultGenres(): ResponseEntity<String> {
        logger.info("Inserting default genres")
        genresService.insertGenres()
        logger.info("Default genres inserted successfully")
        return ResponseEntity.status(HttpStatus.CREATED).body("Default genres inserted successfully")
    }

    // Endpoint to get genre IDs by genre names
    @GetMapping("/ids")
    suspend fun getProviderIdsByNames(
        @RequestParam names: List<String> // Accept a list of provider names
    ): ResponseEntity<List<Int>> {
        logger.info("Fetching genre IDs for names: $names")
        val genreIds = genresService.fetchGenreIdsByNames(names)
        return if (genreIds.isNotEmpty()) {
            logger.info("Found ${genreIds.size} genre IDs for names: $names")
            ResponseEntity.ok(genreIds)
        } else {
            logger.warn("No genre IDs found for names: $names")
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/user/{userId}/genres")
    suspend fun getUserGenres(
        @PathVariable userId: Int
    ): ResponseEntity<List<UserGenreDto>> {
        return genresService.getUserGenres(userId).fold(
            onSuccess = { genres ->
                ResponseEntity.ok(genres)
            },
            onFailure = { exception ->
                when (exception) {
                    is NoSuchElementException -> ResponseEntity.notFound().build()
                    else -> ResponseEntity.internalServerError().build()
                }
            }
        )
    }

    @PostMapping("/user/{userId}/update-genres")
    suspend fun updateUserGenres(
        @PathVariable userId: Int,
        @RequestBody genres: List<UserGenreDto>
    ): ResponseEntity<List<UserGenreDto>> {
        return genresService.updateUserGenres(userId, genres).fold(
            onSuccess = { updatedGenres ->
                ResponseEntity.ok(updatedGenres)
            },
            onFailure = { exception ->
                when (exception) {
                    is NoSuchElementException -> ResponseEntity.notFound().build()
                    else -> ResponseEntity.internalServerError().build()
                }
            }
        )
    }

}
