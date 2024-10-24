package com.api.postgres.services



import com.api.postgres.UserInfo
import com.api.postgres.UserParams
import com.api.postgres.models.*
import com.api.postgres.repositories.GenreRepository
import com.api.postgres.repositories.UserAvoidGenresRepository
import com.api.postgres.repositories.UserGenresRepository
import com.api.postgres.repositories.UserRepository
import com.api.postgres.repositories.UserSubscriptionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class UsersService @Autowired constructor(
    private val userRepository: UserRepository,
    private val userSubscriptionRepository: UserSubscriptionRepository,
    private val userGenreRepository: UserGenresRepository,
    private val userAvoidGenreRepository: UserAvoidGenresRepository,
    private val genreRepository: GenreRepository,

) {
    // Transactional function to add a new user with subscriptions and genres provided as parameters
    @Transactional
    fun addUser(
        userData: UserEntity,
        subscriptions: List<Int>,
        genres: List<Int>,
        avoidGenres: List<Int>
    ): Int {
        val user = UserEntity(
            name = userData.name,
            username = userData.username,
            email = userData.email,
            password = userData.password,
            language = userData.language, // Language provided directly
            region = userData.region ?: "Default", // Default if not provided
            minMovie = userData.minMovie,
            maxMovie = userData.maxMovie,
            minTV = userData.minTV,
            maxTV = userData.maxTV,
            oldestDate = userData.oldestDate,
            recentDate = userData.recentDate,
            createdAt = userData.createdAt,
            recentLogin = userData.recentLogin
        )

        // Save the user entity
        val savedUser = userRepository.save(user)

        // Insert user subscriptions
        subscriptions.forEach { providerId ->
            val userSubscription = UserSubscription(
                id = UserSubscriptionId(
                    userId = savedUser.userId ?: throw IllegalStateException("User ID cannot be null"),
                    providerId = providerId
                ),
                priority = 1, // Set default priority or customize as needed
                user = savedUser
            )
            userSubscriptionRepository.save(userSubscription)
        }

        // Insert user genres from the provided list
        genres.forEach { genreId ->
            // Retrieve the GenreEntity using the genreId
            val genre = genreRepository.findById(genreId)
                .orElseThrow { IllegalArgumentException("Genre with ID $genreId not found") }

            val userGenre = UserGenres(
                id = UserGenreId(
                    userId = savedUser.userId ?: throw IllegalStateException("User ID cannot be null"),
                    genreId = genreId
                ),
                user = savedUser, // Setting the relationship with the user entity
                genre = genre // Set the valid GenreEntity
            )
            userGenreRepository.save(userGenre)
        }

        // Insert avoided genres from the provided list
        avoidGenres.forEach { avoidGenreId ->
            // Similar logic for avoided genres if necessary
            val avoidGenre = genreRepository.findById(avoidGenreId)
                .orElseThrow { IllegalArgumentException("Genre with ID $avoidGenreId not found") }

            val userAvoidGenre = UserAvoidGenres(
                id = UserAvoidGenreId(
                    userId = savedUser.userId ?: throw IllegalStateException("User ID cannot be null"),
                    genreId = avoidGenreId
                ),
                user = savedUser, // Setting the relationship with the user entity
                genre = avoidGenre // Set the valid GenreEntity for avoided genres
            )
            userAvoidGenreRepository.save(userAvoidGenre)
        }

        return savedUser.userId ?: throw IllegalStateException("User ID cannot be null")
    }



    // Transactional function to check user credentials
    @Transactional
    fun checkUserCredentials(username: String, password: String): Boolean {
        return userRepository.findByUsernameAndPassword(username, password) != null
    }

    // Transactional function to update the most recent login timestamp
    @Transactional
    fun updateRecentLogin(username: String, timestamp: LocalDateTime) {
        val user = userRepository.findByUsernameAndPassword(username, "") ?: return
        user.recentLogin = timestamp.toString() // Update with the passed timestamp
        userRepository.save(user)
    }

    // Function to fetch user parameters (settings)
    @Transactional
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

    @Transactional(readOnly = true)
    fun getUserInfo(userId: Int): UserInfo? {
        val user = userRepository.fetchUserParams(userId) ?: return null

        // Get user subscriptions directly from the repository
        val subscriptions = userSubscriptionRepository.findByIdUserId(userId).map { it.id.providerId }

        // Get user genres directly from the repository
        val genres = userGenreRepository.findByIdUserId(userId).map { it.id.genreId }

        val avoidGenres = userAvoidGenreRepository.findByIdUserId(userId).map { it.id.genreId }

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
            genres = genres,
            avoidGenres = avoidGenres
        )
    }
}
