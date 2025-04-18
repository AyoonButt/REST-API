package com.api.postgres.controllers


import com.api.postgres.services.Credits
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/credits")
class CreditsController(
    private val creditsService: Credits
) {
    private val logger: Logger = LoggerFactory.getLogger(CreditsController::class.java)

    @PostMapping("/post/{tmdbId}")
    suspend fun insertCredits(
        @PathVariable tmdbId: Int,
        @RequestBody creditsResponse: JSONObject
    ): ResponseEntity<String> {
        logger.info("Received request to insert credits for postId: $tmdbId")
        return try {
            creditsService.parseAndInsertCredits(creditsResponse, tmdbId)
            ResponseEntity.status(HttpStatus.CREATED)
                .body("Credits inserted successfully for tmdbId: $tmdbId")
        } catch (e: Exception) {
            logger.error("Error inserting credits for postId: $tmdbId - ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to insert credits for tmdbId: $tmdbId")
        }
    }

    @GetMapping("/post/{postId}")
    suspend fun getCredits(@PathVariable postId: Int): ResponseEntity<Map<String, List<Any>>> {
        logger.info("Fetching credits for postId: $postId")
        val credits = creditsService.loadCreditsFromDatabase(postId)
        return if (credits.isNotEmpty()) {
            logger.info("Found credits for postId: $postId")
            ResponseEntity.ok(credits)
        } else {
            logger.warn("No credits found for postId: $postId")
            ResponseEntity.status(HttpStatus.NO_CONTENT).build()
        }
    }
}