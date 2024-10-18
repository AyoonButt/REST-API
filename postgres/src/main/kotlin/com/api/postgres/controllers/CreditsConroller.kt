package com.api.postgres.controllers

import com.api.postgres.services.Credits
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/credits")
class CreditsController(
    private val creditsService: Credits
) {

    // Endpoint to parse and insert credits for a specific post
    @PostMapping("/post/{postId}")
    fun insertCredits(
        @PathVariable postId: Int,
        @RequestBody creditsJson: String
    ): ResponseEntity<String> {
        return runBlocking {
            creditsService.parseAndInsertCredits(creditsJson, postId)
            ResponseEntity.status(HttpStatus.CREATED).body("Credits inserted successfully for postId: $postId")
        }
    }

    // Endpoint to retrieve credits from the database for a specific post
    @GetMapping("/post/{postId}")
    fun getCredits(@PathVariable postId: Int): ResponseEntity<Map<String, List<Map<String, Any?>>>> {
        return runBlocking {
            val credits = creditsService.loadCreditsFromDatabase(postId)
            if (credits.isNotEmpty()) {
                ResponseEntity.ok(credits)
            } else {
                ResponseEntity.status(HttpStatus.NO_CONTENT).build()
            }
        }
    }
}
