class UserService {

    private fun saveSettings() {
        // Retrieve user registration data
        val name_string = intent.getStringExtra("name") ?: ""
        val username_string = intent.getStringExtra("username") ?: ""
        val email_string = intent.getStringExtra("email") ?: ""
        val password = intent.getStringExtra("password") ?: ""

        // Retrieve settings data
        val selectedLanguage = spinnerLanguage.selectedItem.toString()
        val selectedRegion = spinnerRegion.selectedItem.toString()
        val languageId = getIso6391(selectedLanguage)
        val countryId = getIsoCode(selectedRegion)

        val selectedMin = spinnerMin.selectedItem.toString().toInt()
        val selectedMax = spinnerMax.selectedItem.toString().toInt()
        val selectedMinTV = spinnerMinTV.selectedItem.toString().toInt()
        val selectedMaxTV = spinnerMaxTV.selectedItem.toString().toInt()

        val genresToAvoid = selectedGenres.joinToString(", ")
        val subscriptionNames = getOrderOfSubscriptionItemNames(linearLayoutSubscriptions)
        val genreNames = getOrderOfItemNames(linearLayoutGenres)
        val subscriptionIds = getProviderIds(subscriptionNames)
        val genreIds = getGenreIds(genreNames)

        Log.d(
            "SettingsInfo",
            "GenresToAvoid: $genresToAvoid, Subscriptions: $subscriptionIds, Genres: $genreIds"
        )

        // Insert data into the database
        val userId = transaction {
            // Insert the user
            Users.insert {
                it[name] = name_string
                it[username] = username_string
                it[email] = email_string
                it[pswd] = password
                it[language] = languageId
                it[region] = countryId ?: "Default"
                it[minMovie] = selectedMin
                it[maxMovie] = selectedMax
                it[minTV] = selectedMinTV
                it[maxTV] = selectedMaxTV
                it[oldestDate] = selectedOldestDate
                it[recentDate] = selectedMostRecentDate
                it[createdAt] = getCurrentTimestamp()
            }

            // Retrieve the userId of the inserted user
            Users
                .select { Users.email eq email_string } // Assuming email is unique
                .single()[Users.userId]
        }

        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putInt("userId", userId)
            apply()
        }


        // Insert user subscriptions
        transaction {
            subscriptionIds.forEach { providerId ->
                UserSubscriptions.insert {
                    it[this.userId] = userId // Use the retrieved userId
                    it[providerID] = providerId
                    it[avoidGenres] = genresToAvoid
                }
            }
        }

        // Insert user genres
        transaction {
            genreIds.forEach { genreId ->
                UserGenres.insert {
                    it[this.userId] = userId // Use the retrieved userId
                    it[genreID] = genreId
                }
            }
        }

        // Navigate to LoginForm
        val intent = Intent(this, LoginForm::class.java)
        startActivity(intent)
        finish()
    }

    private suspend fun checkUserCredentials(username: String, password: String): Boolean {
        return transaction {
            Users.select {
                (Users.username eq username) and (Users.pswd eq password)
            }.singleOrNull() != null
        }
    }

    private suspend fun updateRecentLogin(username: String) {
        transaction {
            Users.update({ Users.username eq username }) {
                it[recentLogin] = getCurrentTimestamp() // Update the recentDate column
            }
        }
    }

    fun fetchUserParams(userId: Int): UserParams? {
        return transaction {
            Users.select { Users.userId eq userId }
                .mapNotNull {
                    UserParams(
                        language = it[Users.language],
                        region = it[Users.region],
                        minMovie = it[Users.minMovie],
                        maxMovie = it[Users.maxMovie],
                        minTV = it[Users.minTV],
                        maxTV = it[Users.maxTV],
                        oldestDate = it[Users.oldestDate],
                        recentDate = it[Users.recentDate]
                    )
                }
                .singleOrNull()
        }
    }

    fun getProvidersByPriority(userId: Int): List<Int> {
        return transaction {
            UserSubscriptions
                .select { UserSubscriptions.userId eq userId }
                .orderBy(UserSubscriptions.priority to SortOrder.ASC)
                .map { it[UserSubscriptions.providerID] }
        }
    }
    
}
