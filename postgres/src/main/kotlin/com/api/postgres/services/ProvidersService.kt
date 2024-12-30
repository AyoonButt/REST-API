package com.api.postgres.services


import com.api.postgres.models.SubscriptionProvider
import com.api.postgres.repositories.ProviderRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Service
class ProvidersService @Autowired constructor(
    private val providerRepository: ProviderRepository // Assuming you have a ProviderRepository for database operations
) {


    // Function to insert providers into the database
    @Transactional
    suspend fun addProvidersToDatabase(providersList: List<SubscriptionProvider>) {
        withContext(Dispatchers.IO) {
            providersList.forEach { provider ->
                providerRepository.save(provider) // Save each provider to the database
            }
        }
    }

    @Transactional
    fun filterProviders(query: String): List<SubscriptionProvider> {
        return if (query.isNotEmpty()) {
            providerRepository.findByProviderNameContainingIgnoreCase(query).take(5)
        } else {
            emptyList()
        }
    }

    @Transactional
// Suspend function to fetch all providers from the database without pagination
    suspend fun fetchProvidersFromDatabase(): List<SubscriptionProvider> {
        return withContext(Dispatchers.IO) {
            providerRepository.findAll().toList() // Assuming a method exists in the repository to fetch all records
        }
    }


    @Transactional(readOnly = true)
    suspend fun fetchProviderIdsByNames(names: List<String>): List<Int> {
        return withContext(Dispatchers.IO) {
            if (names.isEmpty()) {
                return@withContext emptyList()
            }

            // Fetch provider entities from the database
            val providers = providerRepository.findAllProviderIdsByNames(names)

            // Create a map of provider names to IDs
            val providerNameToIdMap = providers.associateBy(
                keySelector = { it.providerName},
                valueTransform = { it.providerId }
            )

            // Map input names to IDs, maintaining order and handling case-insensitivity
            names.mapNotNull { name ->
                providerNameToIdMap[name]
            }
        }
    }

}
