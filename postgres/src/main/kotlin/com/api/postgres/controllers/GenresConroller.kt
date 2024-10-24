package com.api.postgres.controllers

import com.api.postgres.models.GenreEntity
import com.api.postgres.services.Genres
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/genres")
class GenresController(
    private val genresService: Genres
) {

    // Endpoint to filter genres based on a query string
    @GetMapping("/filter")
    fun filterGenres(@RequestParam query: String): ResponseEntity<List<GenreEntity>> {
        val genres = genresService.filterGenres(query)
        return if (genres.isNotEmpty()) {
            ResponseEntity.ok(genres)
        } else {
            ResponseEntity.status(HttpStatus.NO_CONTENT).build()
        }
    }

    // Endpoint to filter genres to avoid based on a query string
    @GetMapping("/filter/avoid")
    fun filterAvoidGenres(@RequestParam query: String): ResponseEntity<List<GenreEntity>> {
        val genres = genresService.filterAvoidGenres(query)
        return if (genres.isNotEmpty()) {
            ResponseEntity.ok(genres)
        } else {
            ResponseEntity.status(HttpStatus.NO_CONTENT).build()
        }
    }

    // Endpoint to add a new genre
    @PostMapping("/add")
    fun addGenre(@RequestBody genre: GenreEntity): ResponseEntity<String> {
        genresService.addGenre(genre)
        return ResponseEntity.status(HttpStatus.CREATED).body("Genre added successfully: ${genre.genreName}")
    }

    // Endpoint to insert default genres (can be used during setup)
    @PostMapping("/insertDefaults")
    fun insertDefaultGenres(): ResponseEntity<String> {
        genresService.insertGenres()
        return ResponseEntity.status(HttpStatus.CREATED).body("Default genres inserted successfully")
    }
}
