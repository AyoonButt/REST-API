package com.example.api.postgres.services

class Providers {

    private val client = OkHttpClient()
    
    // Suspend function to fetch providers from the API
    suspend fun fetchProviders(): List<Provider> {
        val request = Request.Builder()
            .url("https://api.themoviedb.org/3/watch/providers/movie?language=en-US")
            .get()
            .addHeader("accept", "application/json")
            .addHeader(
                "Authorization",
                "Bearer YOUR_API_KEY"
            )
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

    // Function to insert providers into DB after fetching
    private suspend fun saveProvidersToDB(providers: List<Provider>) {
        transaction {
            providers.forEach { provider ->
                SubscriptionProviders.insertIgnore {
                    it[providerId] = provider.provider_id
                    it[providerName] = provider.provider_name
                }
            }
        }
    }

    // New method to filter the providers using a search query and return top 5 results
    suspend fun filterProviders(query: String): List<Provider> {
        return transaction {
            if (query.isNotEmpty()) {
                // Get providers from the DB
                SubscriptionProviders
                    .selectAll()
                    .map { Provider(it[SubscriptionProviders.providerId], it[SubscriptionProviders.providerName]) }
                    .filter { provider -> 
                        provider.provider_name.contains(query, ignoreCase = true) // Filter by query
                    }
                    .take(5) // Take the top 5 matching results
            } else {
                emptyList() // Return empty list if query is empty
            }
        }
    }

    // Function to parse providers from the response body
    private fun parseProviders(responseBody: String): List<Provider> {
        val jsonObject = JSONObject(responseBody)
        val results = jsonObject.getJSONArray("results")
        val providers = mutableListOf<Provider>()

        for (i in 0 until results.length()) {
            val providerObj = results.getJSONObject(i)
            val provider = Provider(
                provider_id = providerObj.getInt("provider_id"),
                provider_name = providerObj.getString("provider_name")
            )
            providers.add(provider)
        }

        return providers
    }

    // Example of REST API to get filtered providers
    suspend fun getFilteredProvidersAPI(query: String): List<Provider> {
        val providers = fetchProviders() // Fetch from API
        saveProvidersToDB(providers) // Save fetched providers to the database
        return filterProviders(query) // Filter based on user query
    }
}
