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
    // Suspend function to fetch providers from the database
    suspend fun fetchProvidersFromDatabase(limit: Int, offset: Int): List<SubscriptionProvider> {
        return withContext(Dispatchers.IO) {
            providerRepository.findAllWithLimitAndOffset(limit, offset) // Assuming a method exists in the repository
        }
    }


}
