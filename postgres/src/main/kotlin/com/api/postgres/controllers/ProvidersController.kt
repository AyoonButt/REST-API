package com.api.postgres.controllers

import com.api.postgres.UserSubscriptionDto
import com.api.postgres.models.GenreEntity
import com.api.postgres.models.SubscriptionProvider
import com.api.postgres.services.ProvidersService
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/providers")
class ProvidersController(
    private val providersService: ProvidersService
) {

    private val logger: Logger = LoggerFactory.getLogger(ProvidersController::class.java)

    // Endpoint to add subscription providers to the database
    @PostMapping("/add")
    fun addProviders(
        @RequestBody providersList: List<SubscriptionProvider>
    ): ResponseEntity<String> {
        logger.info("Adding providers to the database. Providers count: ${providersList.size}")
        return runBlocking {
            providersService.addProvidersToDatabase(providersList)
            logger.info("Successfully added ${providersList.size} providers to the database.")
            ResponseEntity.ok("Providers added successfully")
        }
    }

    // Endpoint to fetch all providers without pagination
    @GetMapping("/list/")
    fun getAllProviders(): ResponseEntity<List<SubscriptionProvider>> {
        logger.info("Fetching all subscription providers from the database.")
        return runBlocking {
            val providers = providersService.fetchProvidersFromDatabase()
            logger.info("Fetched ${providers.size} providers from the database.")
            ResponseEntity.ok(providers)
        }
    }

    @GetMapping("/filter")
    fun filterProviders(@RequestParam query: String): ResponseEntity<List<SubscriptionProvider>> {
        logger.info("Filtering genres with query: $query")
        val providers = providersService.filterProviders(query)
        return if (providers.isNotEmpty()) {
            logger.info("Found ${providers.size} genres matching query: $query")
            ResponseEntity.ok(providers)
        } else {
            logger.warn("No genres found for query: $query")
            ResponseEntity.status(HttpStatus.NO_CONTENT).build()
        }
    }

    // Endpoint to fetch provider IDs by their names
    @GetMapping("/ids")
    suspend fun getProviderIdsByNames(
        @RequestParam names: List<String> // Accept a list of provider names
    ): ResponseEntity<List<Int>> {
        logger.info("Fetching provider IDs for the following names: $names")
        val providerIds = providersService.fetchProviderIdsByNames(names)
        return if (providerIds.isNotEmpty()) {
            logger.info("Found provider IDs: $providerIds")
            ResponseEntity.ok(providerIds)
        } else {
            logger.warn("No provider IDs found for names: $names")
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/user/{userId}/subscriptions")
    suspend fun getUserSubscriptions(
        @PathVariable userId: Int
    ): ResponseEntity<List<UserSubscriptionDto>> {
        return providersService.getUserSubscriptions(userId).fold(
            onSuccess = { subscriptions ->
                ResponseEntity.ok(subscriptions)
            },
            onFailure = { exception ->
                when (exception) {
                    is NoSuchElementException -> ResponseEntity.notFound().build()
                    else -> ResponseEntity.internalServerError().build()
                }
            }
        )
    }

    @PostMapping("/user/{userId}/update-subscriptions")
    suspend fun updateUserSubscriptions(
        @PathVariable userId: Int,
        @RequestBody subscriptions: List<UserSubscriptionDto>
    ): ResponseEntity<List<UserSubscriptionDto>> {
        return providersService.updateUserSubscriptions(userId, subscriptions).fold(
            onSuccess = { updatedSubscriptions ->
                ResponseEntity.ok(updatedSubscriptions)
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
