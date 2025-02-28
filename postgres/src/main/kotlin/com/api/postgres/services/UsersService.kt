package com.api.postgres.services

import com.api.postgres.UserDto
import com.api.postgres.UserPreferencesDto
import com.api.postgres.UserPreferencesProjection
import com.api.postgres.UserProjection
import com.api.postgres.UserUpdate

import com.api.postgres.models.*

import com.api.postgres.repositories.UserAvoidGenresRepository
import com.api.postgres.repositories.UserGenresRepository
import com.api.postgres.repositories.UserRepository
import com.api.postgres.repositories.UserSubscriptionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.Int


@Service
class UsersService @Autowired constructor(
    private val userRepository: UserRepository,
    private val userSubscriptionRepository: UserSubscriptionRepository,
    private val userGenreRepository: UserGenresRepository,
    private val userAvoidGenreRepository: UserAvoidGenresRepository
) {
    private val logger: Logger = LoggerFactory.getLogger(Posts::class.java)
    private fun UserProjection.toDto() = UserDto(
        userId = userId,
        name = name,
        username = username,
        password = password,
        email = email,
        language = language.toString(),
        region = region.toString(),
        minMovie = minMovie,
        maxMovie = maxMovie,
        minTV = minTV,
        maxTV = maxTV,
        oldestDate = oldestDate.toString(),
        recentDate = recentDate.toString(),
        createdAt = createdAt.toString(),
        recentLogin = recentLogin
    )

    private fun UserPreferencesProjection.toDto() = UserPreferencesDto(
        userId = userId,
        language = language,
        region = region,
        minMovie = minMovie,
        maxMovie = maxMovie,
        minTV = minTV,
        maxTV = maxTV,
        oldestDate = oldestDate,
        recentDate = recentDate,
        subscriptions = subscriptions,
        genreIds = genreIds,
        avoidGenreIds = avoidGenreIds
    )

    @Transactional(readOnly = true)
    suspend fun getUserByUsername(username: String): Result<UserEntity?> = withContext(Dispatchers.IO) {
        try {
            val user = userRepository.findByUsername(username)
            logger.info("Found user for username: $username")
            Result.success(user)
        } catch (e: Exception) {
            logger.error("Error finding user by username $username: ${e.message}")
            Result.failure(e)
        }
    }

    @Transactional
    suspend fun addUser(
        userDto: UserDto,
        subscriptions: List<Int>,
        genres: List<Int>,
        avoidGenres: List<Int>
    ): Int = withContext(Dispatchers.IO) {
        // Insert user first
        val user = userRepository.save(UserEntity(
            name = userDto.name,
            username = userDto.username,
            email = userDto.email,
            password = userDto.password,
            language = userDto.language,
            region = userDto.region,
            minMovie = userDto.minMovie,
            maxMovie = userDto.maxMovie,
            minTV = userDto.minTV,
            maxTV = userDto.maxTV,
            oldestDate = userDto.oldestDate,
            recentDate = userDto.recentDate,
            createdAt = userDto.createdAt,
            recentLogin = userDto.recentLogin.toString()
        ))

        // Insert subscriptions with priorities
        subscriptions.forEachIndexed { index, providerId ->
            userSubscriptionRepository.insertUserSubscription(
                userId = user.userId!!,
                providerId = providerId,
                priority = index + 1
            )
        }

        // Insert genres with priorities
        genres.forEachIndexed { index, genreId ->
            userGenreRepository.insertUserGenre(
                userId = user.userId!!,
                genreId = genreId,
                priority = index + 1
            )
        }

        // Insert avoid genres
        avoidGenres.forEach { genreId ->
            userAvoidGenreRepository.insertUserAvoidGenre(
                userId = user.userId!!,
                genreId = genreId
            )
        }

        user.userId!!
    }

    private suspend fun updateGenres(userId: Int, newGenres: List<Int>) {
        val currentGenres = userGenreRepository.findGenreIdsByUserId(userId)
        val genresToAdd = newGenres.filterNot { currentGenres.contains(it) }
        val genresToRemove = currentGenres.filterNot { newGenres.contains(it) }

        genresToRemove.forEach { genreId ->
            userGenreRepository.deleteById(UserGenreId(userId, genreId))
        }

        genresToAdd.forEach { genreId ->
            val priority = userGenreRepository.findMaxPriorityByUserId(userId) + 1
            userGenreRepository.insertUserGenre(userId, genreId, priority)
        }
    }

    private suspend fun updateAvoidGenres(userId: Int, newAvoidGenres: List<Int>) {
        val currentAvoidGenres = userAvoidGenreRepository.findAvoidGenreIdsByUserId(userId)
        val genresToAdd = newAvoidGenres.filterNot { currentAvoidGenres.contains(it) }
        val genresToRemove = currentAvoidGenres.filterNot { newAvoidGenres.contains(it) }

        genresToRemove.forEach { genreId ->
            userAvoidGenreRepository.deleteById(UserAvoidGenreId(userId, genreId))
        }

        genresToAdd.forEach { genreId ->
            userAvoidGenreRepository.insertUserAvoidGenre(userId, genreId)
        }
    }

    @Transactional(readOnly = true)
    suspend fun checkUserCredentials(username: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            userRepository.findDtoByUsernameAndPassword(username, password) != null
        }
    }

    @Transactional
    suspend fun updateRecentLogin(username: String, timestamp: String) {
        withContext(Dispatchers.IO) {
            userRepository.updateRecentLogin(username, timestamp)
        }
    }

    suspend fun getUserPreferences(userId: Int): UserPreferencesDto? {
        return withContext(Dispatchers.IO) {
            val userPreferences = userRepository.findUserPreferencesById(userId)

            userPreferences?.let { preferences ->
                UserPreferencesDto(
                    userId = preferences.userId,
                    language = preferences.language,
                    region = preferences.region,
                    minMovie = preferences.minMovie,
                    maxMovie = preferences.maxMovie,
                    minTV = preferences.minTV,
                    maxTV = preferences.maxTV,
                    oldestDate = preferences.oldestDate,
                    recentDate = preferences.recentDate,
                    subscriptions = userSubscriptionRepository
                        .findProviderIdsByUserIdSortedByPriority(userId),
                    genreIds = userGenreRepository
                        .findGenreIdsByUserIdOrderedByPriority(userId),
                    avoidGenreIds = userAvoidGenreRepository
                        .findAvoidGenreIdsByUserId(userId)
                )
            }
        }
    }

    @Transactional(readOnly = true)
    suspend fun getProvidersByPriority(userId: Int): List<Int> {
        return withContext(Dispatchers.IO) {
            userSubscriptionRepository.findProviderIdsByUserIdSortedByPriority(userId)
        }
    }

    @Transactional
    suspend fun updateUserProperties(userId: Int, updates: UserUpdate): Result<UserEntity?> = withContext(Dispatchers.IO) {
        try {
            // Get current user
            val currentUser = userRepository.findById(userId).orElse(null) ?: return@withContext Result.failure(
                NoSuchElementException("User not found with id: $userId")
            )

            // Apply non-null updates
            updates.language?.let { currentUser.language = it }
            updates.region?.let { currentUser.region = it }
            updates.minMovie?.let { currentUser.minMovie = it }
            updates.maxMovie?.let { currentUser.maxMovie = it }
            updates.minTV?.let { currentUser.minTV = it }
            updates.maxTV?.let { currentUser.maxTV = it }
            updates.name?.let { currentUser.name = it }
            updates.username?.let { currentUser.username = it }
            updates.email?.let { currentUser.email = it }
            updates.oldestDate?.let { currentUser.oldestDate = it }
            updates.recentDate?.let { currentUser.recentDate = it }

            // Save and return updated user
            val updatedUser = userRepository.save(currentUser)
            logger.info("Successfully updated user properties for userId: $userId")
            Result.success(updatedUser)
        } catch (e: Exception) {
            logger.error("Error updating user properties for userId $userId: ${e.message}")
            Result.failure(e)
        }
    }
}