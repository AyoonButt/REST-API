package com.api.postgres.services

import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

@Service
class ProvidersService @Autowired constructor(
    private val providerRepository: ProviderRepository // Assuming you have a ProviderRepository for database operations
) {
    private val client = OkHttpClient()

    // Suspend function to fetch providers from the API
    suspend fun fetchProvidersFromAPI(): List<Provider> {
        val request = Request.Builder()
            .url("https://api.themoviedb.org/3/watch/providers/movie?language=en-US") // Change to the appropriate endpoint for providers
            .get()
            .addHeader("accept", "application/json")
            .addHeader("Authorization", "Bearer YOUR_API_KEY")
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                responseBody?.let {
                    parseProviders(it) // Parses and returns List<Provider>
                } ?: emptyList()
            } catch (e: IOException) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    // Function to insert providers into the database
    @Transactional
    suspend fun addProvidersToDatabase(providersList: List<Provider>) {
        withContext(Dispatchers.IO) {
            providersList.forEach { provider ->
                providerRepository.save(provider) // Save each provider to the database
            }
        }
    }

    // Parse the providers from the API response
    private fun parseProviders(responseBody: String): List<Provider> {
        val jsonObject = JSONObject(responseBody)
        val results = jsonObject.getJSONObject("results") // Adjust based on the actual JSON structure
        val providers = mutableListOf<Provider>()

        // Assuming results is a JSONObject with provider data
        results.keys().forEach { key ->
            val providerInfo = results.getJSONObject(key)
            val provider = Provider(
                id = providerInfo.getInt("id"),
                name = providerInfo.getString("provider_name"),
                logoPath = providerInfo.optString("logo_path"),
                providerType = providerInfo.optString("provider_type") // Adjust as per your Provider data class
            )
            providers.add(provider)
        }

        return providers
    }

    // Suspend function to fetch providers from the database
    suspend fun fetchProvidersFromDatabase(limit: Int, offset: Int): List<Provider> {
        return withContext(Dispatchers.IO) {
            providerRepository.findAllByOrderById(limit, offset) // Assuming a method exists in the repository
        }
    }

    // Example of REST API to get providers with pagination
    suspend fun getPaginatedProvidersAPI(limit: Int, offset: Int): List<Provider> {
        return fetchProvidersFromDatabase(limit, offset)
    }
}
