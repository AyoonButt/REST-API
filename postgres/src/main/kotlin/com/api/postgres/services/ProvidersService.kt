package com.api.postgres.services



import com.api.postgres.UserSubscriptionDto
import com.api.postgres.UserSubscriptionProjection
import com.api.postgres.models.SubscriptionProvider
import com.api.postgres.models.UserSubscriptionId
import com.api.postgres.repositories.ProviderRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.api.postgres.repositories.UserSubscriptionRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.Int

@Service
class ProvidersService @Autowired constructor(
    private val providerRepository: ProviderRepository,
    private val userSubscriptionRepository: UserSubscriptionRepository,
) {

    private val logger: Logger = LoggerFactory.getLogger(ProvidersService::class.java)

    private fun UserSubscriptionProjection.toDto() = UserSubscriptionDto(
        userId = userId,
        providerId = providerId,
        providerName =  providerName,
        priority= priority
    )


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

    @Transactional
    suspend fun getUserSubscriptions(userId: Int): Result<List<UserSubscriptionDto>> = withContext(Dispatchers.IO) {
        try {
            val projections = userSubscriptionRepository.findProjectedByUserIdOrderByPriority(userId)
            val dtos = projections.map { projection ->
                projection.toDto() // Using the extension function defined at the top
            }
            Result.success(dtos)
        } catch (e: Exception) {
            logger.error("Error getting user subscriptions for userId $userId: ${e.message}")
            Result.failure(e)
        }
    }

    @Transactional
    suspend fun updateUserSubscriptions(userId: Int, newSubscriptions: List<UserSubscriptionDto>): Result<List<UserSubscriptionDto>> = withContext(Dispatchers.IO) {
        try {
            // Get current subscriptions using projection
            val currentSubscriptions = userSubscriptionRepository.findProjectedByUserIdOrderByPriority(userId)

            // Find subscriptions to remove
            val newProviderIds = newSubscriptions.map { it.providerId }
            val subscriptionsToRemove = currentSubscriptions.filter {
                !newProviderIds.contains(it.providerId)
            }

            // Remove old subscriptions
            subscriptionsToRemove.forEach { projection ->
                userSubscriptionRepository.deleteById(UserSubscriptionId(projection.userId, projection.providerId))
            }

            newSubscriptions.forEach { dto ->
                userSubscriptionRepository.insertUserSubscription(
                    userId = userId,
                    providerId = dto.providerId,
                    priority = dto.priority
                )
            }

            // Return updated subscriptions using projection
            val updatedProjections = userSubscriptionRepository.findProjectedByUserIdOrderByPriority(userId)
            Result.success(updatedProjections.map { it.toDto() })

        } catch (e: Exception) {
            logger.error("Error updating user subscriptions for userId $userId: ${e.message}")
            Result.failure(e)
        }
    }
}
