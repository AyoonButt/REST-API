package com.api.postgres.controllers


import com.api.postgres.models.SubscriptionProvider
import com.api.postgres.services.ProvidersService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import kotlinx.coroutines.runBlocking

@RestController
@RequestMapping("/api/providers")
class ProvidersController(
    private val providersService: ProvidersService
) {

    // Endpoint to add subscription providers to the database
    @PostMapping("/add")
    fun addProviders(
        @RequestBody providersList: List<SubscriptionProvider>
    ): ResponseEntity<String> {
        return runBlocking {
            providersService.addProvidersToDatabase(providersList)
            ResponseEntity.ok("Providers added successfully")
        }
    }

    // Endpoint to fetch all providers without pagination
    @GetMapping("/list/")
    fun getAllProviders(): ResponseEntity<List<SubscriptionProvider>> {
        return runBlocking {
            val providers = providersService.fetchProvidersFromDatabase()
            ResponseEntity.ok(providers)
        }
    }

    @GetMapping("/ids")
    suspend fun getProviderIdsByNames(
        @RequestParam names: List<String> // Accept a list of provider names
    ): ResponseEntity<List<Int>> {
        val providerIds = providersService.fetchProviderIdsByNames(names)
        return if (providerIds.isNotEmpty()) {
            ResponseEntity.ok(providerIds)
        } else {
            ResponseEntity.notFound().build()
        }
    }

}

