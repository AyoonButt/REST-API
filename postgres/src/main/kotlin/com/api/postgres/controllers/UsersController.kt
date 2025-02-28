package com.api.postgres.controllers


import com.api.postgres.ApiResponse
import com.api.postgres.UserPreferencesDto
import com.api.postgres.UserRequest
import com.api.postgres.UserUpdate
import com.api.postgres.models.UserEntity
import com.api.postgres.services.UsersService
import com.google.gson.Gson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

@RestController
@RequestMapping("/api/users")
class UsersController(private val usersService: UsersService) {
    private val logger: Logger = LoggerFactory.getLogger(UsersController::class.java)

    @GetMapping("/username")
    suspend fun getUserByUsername(@RequestParam username: String): ResponseEntity<UserEntity> {
        logger.info("Fetching user for username: $username")
        return try {
            val userResult = usersService.getUserByUsername(username)
            userResult.fold(
                onSuccess = { user ->
                    if (user != null) {
                        logger.info("Successfully found user for username: $username")
                        val gson = Gson()
                        val jsonString = gson.toJson(user)
                        logger.info("Raw JSON response: $jsonString")
                        ResponseEntity.ok(user)
                    } else {
                        logger.info("No user found for username: $username")
                        ResponseEntity.notFound().build()
                    }
                },
                onFailure = { e ->
                    logger.error("Error fetching user by username $username: ${e.message}")
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                }
            )
        } catch (e: Exception) {
            logger.error("Unexpected error fetching user by username $username: ${e.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PostMapping("/add")
    suspend fun addUser(@RequestBody request: UserRequest): ResponseEntity<Int> {
        logger.info("Adding new user: ${request.userDto}")
        return try {
            val userId = usersService.addUser(
                request.userDto,
                request.subscriptions,
                request.genres,
                request.avoidGenres
            )
            ResponseEntity.status(HttpStatus.CREATED).body(userId)
        } catch (e: Exception) {
            logger.error("Error adding user: ${e.message}", e)
            ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
    }
    @PutMapping("/{userId}")
    suspend fun updateUser(
        @PathVariable userId: Int,
        @RequestBody updates: UserUpdate
    ): ResponseEntity<UserEntity> {
        return usersService.updateUserProperties(userId, updates).fold(
            onSuccess = { user ->
                user?.let { ResponseEntity.ok(it) }
                    ?: ResponseEntity.notFound().build()
            },
            onFailure = { exception ->
                when (exception) {
                    is NoSuchElementException -> ResponseEntity.notFound().build()
                    else -> ResponseEntity.internalServerError().build()
                }
            }
        )
    }


    @PostMapping("/check-credentials")
    suspend fun checkCredentials(
        @RequestParam username: String,
        @RequestParam password: String
    ): ResponseEntity<Boolean> {
        val isValid = usersService.checkUserCredentials(username, password)
        return if (isValid) {
            ResponseEntity.ok(true)
        } else {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false)
        }
    }

    @PutMapping("/{username}/login")
    suspend fun updateLogin(
        @PathVariable username: String,
        @RequestParam timestamp: String
    ): ResponseEntity<ApiResponse> {
        return try {
            logger.info("Updating login for user: $username with timestamp: $timestamp")
            usersService.updateRecentLogin(username, timestamp)
            ResponseEntity.ok(ApiResponse(
                success = true,
                message = "Login timestamp updated"
            ))
        } catch (e: Exception) {
            logger.error("Failed to update login timestamp", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse(
                    success = false,
                    message = "Error updating login timestamp"
                ))
        }
    }

    @GetMapping("/{userId}/preferences")
    suspend fun getUserPreferences(@PathVariable userId: Int): ResponseEntity<UserPreferencesDto> {
        return usersService.getUserPreferences(userId)?.let {
            ResponseEntity.ok(it)
        } ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/{userId}/providers")
    suspend fun getProvidersByPriority(@PathVariable userId: Int): ResponseEntity<List<Int>> {
        return try {
            val providers = usersService.getProvidersByPriority(userId)
            if (providers.isNotEmpty()) {
                ResponseEntity.ok(providers)
            } else {
                ResponseEntity.noContent().build()
            }
        } catch (e: Exception) {
            logger.error("Error fetching providers: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}