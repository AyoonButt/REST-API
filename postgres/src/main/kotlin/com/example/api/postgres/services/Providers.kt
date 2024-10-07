class Providers {

    private fun getProviderIds(names: List<String>): List<Int> {
        return transaction {
            // Query the SubscriptionProviders table
            SubscriptionProviders
                .select { SubscriptionProviders.providerName inList names }
                .map { it[SubscriptionProviders.providerId] }
        }
    }


    private suspend fun fetchProviders() {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://api.themoviedb.org/3/watch/providers/movie?language=en-US")
            .get()
            .addHeader("accept", "application/json")
            .addHeader(
                "Authorization",
                "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJkOWRkOWQzYWU4MzhkYjE4ZDUxZjg4Y2Q1MGU0NzllNCIsIm5iZiI6MTcxOTAzNTYxMS40MjM0NDgsInN1YiI6IjY2MjZiM2ZkMjU4ODIzMDE2NDkxODliMSIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.yDtneQaozCSDZZgvaIF4Dufey-QNNqPcw_BTfdUR2J4"
            )
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            if (responseBody != null) {
                val providers = parseProviders(responseBody)

                // Insert providers into the database
                transaction {
                    providers.forEach { provider ->
                        SubscriptionProviders.insert {
                            it[providerId] = provider.provider_id
                            it[providerName] = provider.provider_name
                        }
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun filterProviders(query: String) {
        transaction {
            if (query.isNotEmpty()) {
                val filteredProviders = SubscriptionProviders
                    .selectAll()
                    .map {
                        it[SubscriptionProviders.providerName] to lcs(
                            it[SubscriptionProviders.providerName],
                            query
                        )
                    }
                    .sortedByDescending { it.second }
                    .map { it.first }
                    .take(5)

                updateProviders(filteredProviders)
            } else {
                updateProviders(emptyList())
            }
        }
    }
}
