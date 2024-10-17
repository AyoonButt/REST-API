package com.api.postgres.services

import com.api.postgres.entities.User
import com.api.postgres.repositories.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UsersService @Autowired constructor(
    private val userRepository: UserRepository,
    private val userSubscriptionsService: UserSubscriptionsService, // Assuming a service for user subscriptions
    private val userGenresService: UserGenresService // Assuming a service for user genres
) {
    // Function to add a new user to the database
    @Transactional
    fun addUser(userData: UserData): Int {
        val user = User(
            name = userData.name,
            username = userData.username,
            email = userData.email,
            pswd = userData.password,
            language = getIso6391(userData.selectedLanguage),
            region = getIsoCode(userData.selectedRegion) ?: "Default",
            minMovie = userData.selectedMinMovie,
            maxMovie = userData.selectedMaxMovie,
            minTV = userData.selectedMinTV,
            maxTV = userData.selectedMaxTV,
            oldestDate = selectedOldestDate,
            recentDate = selectedMostRecentDate,
            createdAt = getCurrentTimestamp()
        )

        val savedUser = userRepository.save(user)

        // Insert user subscriptions
        val subscriptionIds = getProviderIds(userData.subscriptionNames)
        subscriptionIds.forEach { providerId ->
            userSubscriptionsService.addUserSubscription(savedUser.userId!!, providerId, userData.genresToAvoid)
        }

        // Insert user genres
        val genreIds = getGenreIds(userData.genreNames)
        genreIds.forEach { genreId ->
            userGenresService.addUserGenre(savedUser.userId!!, genreId)
        }

        return savedUser.userId!!
    }

    // Function to check user credentials
    fun checkUserCredentials(username: String, password: String): Boolean {
        return userRepository.findByUsernameAndPswd(username, password) != null
    }

    // Function to update the most recent login timestamp
    @Transactional
    fun updateRecentLogin(username: String) {
        val user = userRepository.findByUsernameAndPswd(username, "") ?: return
        user.recentLogin = getCurrentTimestamp()
        userRepository.save(user)
    }

    // Function to fetch user parameters (settings)
    fun fetchUserParams(userId: Int): UserParams? {
        return userRepository.fetchUserParams(userId)?.let {
            UserParams(
                language = it.language,
                region = it.region,
                minMovie = it.minMovie,
                maxMovie = it.maxMovie,
                minTV = it.minTV,
                maxTV = it.maxTV,
                oldestDate = it.oldestDate,
                recentDate = it.recentDate
            )
        }
    }

    // Function to get the providers by priority
    fun getProvidersByPriority(userId: Int): List<Int> {
        return userSubscriptionsService.getProvidersByUserId(userId)
    }

    // New function to get all user information by userId
    fun getUserInfo(userId: Int): UserInfo? {
        val user = userRepository.fetchUserParams(userId) ?: return null

        // Get user subscriptions
        val subscriptions = userSubscriptionsService.getUserSubscriptions(userId)

        // Get user genres
        val genres = userGenresService.getUserGenres(userId)

        return UserInfo(
            userId = user.userId!!,
            name = user.name,
            username = user.username,
            email = user.email,
            language = user.language,
            region = user.region,
            minMovie = user.minMovie,
            maxMovie = user.maxMovie,
            minTV = user.minTV,
            maxTV = user.maxTV,
            oldestDate = user.oldestDate,
            recentDate = user.recentDate,
            createdAt = user.createdAt,
            subscriptions = subscriptions,
            genres = genres
        )
    }
}
