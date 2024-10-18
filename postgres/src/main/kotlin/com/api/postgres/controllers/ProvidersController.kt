package com.api.postgres.controllers

import com.api.postgres.models.Provider
import com.api.postgres.services.ProvidersService
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/providers")
class ProvidersController(private val providersService: ProvidersService) {

    // Endpoint to fetch providers from an external API and save them to the database
    @PostMapping("/fetch")
    fun fetchAndSaveProviders(): ResponseEntity<String> = runBlocking {
        val fetchedProviders = providersService.fetchProvidersFromAPI()
        if (fetchedProviders.isNotEmpty()) {
            providersService.addProvidersToDatabase(fetchedProviders)
            ResponseEntity.ok("Providers successfully fetched and saved.")
        } else {
            ResponseEntity.status(500).body("Failed to fetch providers.")
        }
    }

    // Endpoint to fetch paginated providers from the database
    @GetMapping("/paginated")
    fun getPaginatedProviders(
        @RequestParam("limit", defaultValue = "10") limit: Int,
        @RequestParam("offset", defaultValue = "0") offset: Int
    ): ResponseEntity<List<Provider>> = runBlocking {
        val providers = providersService.getPaginatedProvidersAPI(limit, offset)
        ResponseEntity.ok(providers)
    }

    // Endpoint to fetch all providers from the database
    @GetMapping("/all")
    fun getAllProviders(): ResponseEntity<List<Provider>> = runBlocking {
        val providers = providersService.fetchProvidersFromDatabase(limit = Int.MAX_VALUE, offset = 0)
        ResponseEntity.ok(providers)
    }
}
