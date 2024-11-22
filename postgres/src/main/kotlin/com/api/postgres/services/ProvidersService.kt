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
// Suspend function to fetch all providers from the database without pagination
    suspend fun fetchProvidersFromDatabase(): List<SubscriptionProvider> {
        return withContext(Dispatchers.IO) {
            providerRepository.findAll().toList() // Assuming a method exists in the repository to fetch all records
        }
    }


    // Function to fetch provider IDs based on provider names
    @Transactional
    suspend fun fetchProviderIdsByNames(names: List<String>): List<Int> {
        return withContext(Dispatchers.IO) {
            val providerIds = providerRepository.findAllProviderIdsByNames(names)

            // Ensure that the order matches the input list, assuming you have provider names
            names.mapNotNull { name ->
                // Check if provider name is in providerIds list, assuming it's a string conversion
                providerIds.find { it.toString() == name } // convert providerId to string for comparison
            }
        }
    }



}
