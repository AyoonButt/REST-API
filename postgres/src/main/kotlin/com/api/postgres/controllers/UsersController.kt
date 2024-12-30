package com.api.postgres.controllers


import com.api.postgres.UserPreferencesDto
import com.api.postgres.UserRequest
import com.api.postgres.UserUpdateRequest
import com.api.postgres.services.UsersService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/users")
class UsersController(private val usersService: UsersService) {
    private val logger: Logger = LoggerFactory.getLogger(UsersController::class.java)

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
        @RequestBody request: UserUpdateRequest
    ): ResponseEntity<String> {
        logger.info("Updating user: $userId")
        return try {
            usersService.updateUser(
                userId,
                request.userDto,
                request.subscriptions,
                request.genres,
                request.avoidGenres
            )
            ResponseEntity.ok("User updated successfully")
        } catch (e: Exception) {
            logger.error("Error updating user: ${e.message}", e)
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error updating user: ${e.message}")
        }
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
    suspend fun updateLogin(@PathVariable username: String): ResponseEntity<String> {
        return try {
            usersService.updateRecentLogin(username, LocalDateTime.now())
            ResponseEntity.ok("Login timestamp updated")
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating login timestamp")
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