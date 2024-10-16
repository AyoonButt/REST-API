package com.example.api.postgres.services

class Users {

    // Function to add a new user to the database
    suspend fun addUser(userData: UserData): Int {
        return transaction {
            // Insert the user
            val userId = Users.insertAndGetId {
                it[Users.name] = userData.name
                it[Users.username] = userData.username
                it[Users.email] = userData.email
                it[Users.pswd] = userData.password
                it[Users.language] = getIso6391(userData.selectedLanguage)
                it[Users.region] = getIsoCode(userData.selectedRegion) ?: "Default"
                it[Users.minMovie] = userData.selectedMinMovie
                it[Users.maxMovie] = userData.selectedMaxMovie
                it[Users.minTV] = userData.selectedMinTV
                it[Users.maxTV] = userData.selectedMaxTV
                it[Users.oldestDate] = selectedOldestDate
                it[Users.recentDate] = selectedMostRecentDate
                it[Users.createdAt] = getCurrentTimestamp()
            }.value
    
            // Insert user subscriptions
            val subscriptionIds = getProviderIds(userData.subscriptionNames)
            subscriptionIds.forEach { providerId ->
                UserSubscriptions.insert {
                    it[UserSubscriptions.userId] = userId
                    it[UserSubscriptions.providerID] = providerId
                    it[UserSubscriptions.avoidGenres] = userData.genresToAvoid.joinToString(", ")
                }
            }
    
            // Insert user genres
            val genreIds = getGenreIds(userData.genreNames)
            genreIds.forEach { genreId ->
                UserGenres.insert {
                    it[UserGenres.userId] = userId
                    it[UserGenres.genreID] = genreId
                }
            }
    
            return@transaction userId
        }
    }
    

    // Function to check user credentials
    suspend fun checkUserCredentials(username: String, password: String): Boolean {
        return transaction {
            Users.select {
                (Users.username eq username) and (Users.pswd eq password)
            }.singleOrNull() != null
        }
    }

    // Function to update the most recent login timestamp
    suspend fun updateRecentLogin(username: String) {
        transaction {
            Users.update({ Users.username eq username }) {
                it[Users.recentLogin] = getCurrentTimestamp()
            }
        }
    }

    // Function to fetch user parameters (settings)
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

    // Function to get the providers by priority
    fun getProvidersByPriority(userId: Int): List<Int> {
        return transaction {
            UserSubscriptions
                .select { UserSubscriptions.userId eq userId }
                .orderBy(UserSubscriptions.priority to SortOrder.ASC)
                .map { it[UserSubscriptions.providerID] }
        }
    }

    // New function to get all user information by userId
    fun getUserInfo(userId: Int): UserInfo? {
        return transaction {
            val user = Users.select { Users.userId eq userId }
                .mapNotNull {
                    UserInfo(
                        userId = it[Users.userId],
                        name = it[Users.name],
                        username = it[Users.username],
                        email = it[Users.email],
                        language = it[Users.language],
                        region = it[Users.region],
                        minMovie = it[Users.minMovie],
                        maxMovie = it[Users.maxMovie],
                        minTV = it[Users.minTV],
                        maxTV = it[Users.maxTV],
                        oldestDate = it[Users.oldestDate],
                        recentDate = it[Users.recentDate],
                        createdAt = it[Users.createdAt]
                    )
                }.singleOrNull()

            // Get user subscriptions
            val subscriptions = UserSubscriptions.select { UserSubscriptions.userId eq userId }
                .map { it[UserSubscriptions.providerID] }

            // Get user genres
            val genres = UserGenres.select { UserGenres.userId eq userId }
                .map { it[UserGenres.genreID] }

            // Combine all information into a UserInfo object
            if (user != null) {
                user.subscriptions = subscriptions
                user.genres = genres
            }

            return@transaction user
        }
    }
}