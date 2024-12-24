package com.api.postgres.controllers

import com.api.postgres.UserInfo
import com.api.postgres.UserParams
import com.api.postgres.UserRequest
import com.api.postgres.UserUpdateRequest
import com.api.postgres.models.UserEntity
import com.api.postgres.services.UsersService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/users")
class UsersController constructor(
    private val usersService: UsersService
) {
    private val logger: Logger = LoggerFactory.getLogger(UsersController::class.java)

    @GetMapping("/username")
    fun getUserByUsername(@RequestParam username: String): ResponseEntity<UserEntity?> {
        logger.info("Received request to get user by username: $username")
        val userEntity = usersService.getUserByUsername(username)
        logger.info("User retrieval result: ${if (userEntity != null) "Found" else "Not found"}")
        return ResponseEntity(userEntity, if (userEntity != null) HttpStatus.OK else HttpStatus.NOT_FOUND)
    }

    @PostMapping("/add")
    fun addUser(
        @RequestBody userRequest: UserRequest
    ): ResponseEntity<Any> {
        logger.info("Received request to add user: $userRequest")

        return try {
            val userId = usersService.addUser(
                userRequest.user,
                userRequest.subscriptions,
                userRequest.genres,
                userRequest.avoidGenres
            )
            logger.info("User added successfully with ID: $userId")
            ResponseEntity(userId, HttpStatus.CREATED)
        } catch (ex: IllegalArgumentException) {
            logger.warn("Validation error when adding user: ${ex.message}")
            ResponseEntity("Invalid input: ${ex.message}", HttpStatus.BAD_REQUEST)
        } catch (ex: Exception) {
            logger.error("Unexpected error when adding user: ${ex.message}", ex)
            ResponseEntity("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @PostMapping("/check-credentials")
    fun checkUserCredentials(
        @RequestParam username: String,
        @RequestParam password: String
    ): ResponseEntity<Boolean> {
        logger.info("Checking credentials for username: $username")
        val isValid = usersService.checkUserCredentials(username, password)
        logger.info("Credentials validation result for $username: ${if (isValid) "Valid" else "Invalid"}")
        return ResponseEntity(isValid, if (isValid) HttpStatus.OK else HttpStatus.UNAUTHORIZED)
    }

    @PutMapping("/update-login")
    fun updateRecentLogin(
        @RequestParam username: String
    ): ResponseEntity<Void> {
        logger.info("Updating recent login timestamp for username: $username")
        return try {
            usersService.updateRecentLogin(username, LocalDateTime.now())
            logger.info("Recent login timestamp updated for username: $username")
            ResponseEntity(HttpStatus.OK)
        } catch (ex: Exception) {
            logger.error("Error updating recent login for username: $username", ex)
            ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @GetMapping("/params/{userId}")
    fun fetchUserParams(@PathVariable userId: Int): ResponseEntity<UserParams?> {
        logger.info("Fetching user parameters for userId: $userId")
        val userParams = usersService.fetchUserParams(userId)
        logger.info("User parameters retrieval result for userId $userId: ${if (userParams != null) "Found" else "Not found"}")
        return ResponseEntity(userParams, if (userParams != null) HttpStatus.OK else HttpStatus.NOT_FOUND)
    }

    @GetMapping("/info/{userId}")
    fun getUserInfo(@PathVariable userId: Int): ResponseEntity<UserInfo?> {
        logger.info("Fetching user info for userId: $userId")
        val userInfo = usersService.getUserInfo(userId)
        logger.info("User info retrieval result for userId $userId: ${if (userInfo != null) "Found" else "Not found"}")
        return ResponseEntity(userInfo, if (userInfo != null) HttpStatus.OK else HttpStatus.NOT_FOUND)
    }

    @PutMapping("/{userId}")
    fun updateUser(
        @PathVariable userId: Int,
        @RequestBody userUpdateRequest: UserUpdateRequest
    ): ResponseEntity<String> {
        logger.info("Received request to update user info for userId: $userId")

        return try {
            usersService.updateUser(
                userId,
                userUpdateRequest.userData,
                userUpdateRequest.subscriptions,
                userUpdateRequest.genres,
                userUpdateRequest.avoidGenres
            )
            logger.info("User information updated successfully for userId: $userId")
            ResponseEntity.ok("User information updated successfully")
        } catch (e: Exception) {
            logger.error("Error updating user information for userId: $userId", e)
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }
    }

    @GetMapping("/{userId}/providers")
    fun getProvidersByPriority(@PathVariable userId: Int): List<Int> {
        logger.info("Fetching providers by priority for userId: $userId")
        val providers = usersService.getProvidersByPriority(userId)
        logger.info("Providers retrieved for userId $userId: $providers")
        return providers
    }
}
