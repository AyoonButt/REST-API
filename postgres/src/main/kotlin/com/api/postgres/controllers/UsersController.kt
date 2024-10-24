package com.api.postgres.controllers

import com.api.postgres.UserInfo
import com.api.postgres.UserParams
import com.api.postgres.UserRequest
import com.api.postgres.services.UsersService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/users")
class UsersController @Autowired constructor(
    private val usersService: UsersService
) {

    // Endpoint to create a new user with subscriptions and genres
    @PostMapping("/add")
    fun addUser(
        @RequestBody userRequest: UserRequest
    ): ResponseEntity<Int> {
        return try {
            val userId = usersService.addUser(userRequest.user, userRequest.subscriptions, userRequest.genres,userRequest.avoidGenres)
            ResponseEntity(userId, HttpStatus.CREATED)
        } catch (ex: Exception) {
            ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    // Endpoint to check user credentials
    @PostMapping("/check-credentials")
    fun checkUserCredentials(
        @RequestParam username: String,
        @RequestParam password: String
    ): ResponseEntity<Boolean> {
        val isValid = usersService.checkUserCredentials(username, password)
        return ResponseEntity(isValid, if (isValid) HttpStatus.OK else HttpStatus.UNAUTHORIZED)
    }

    // Endpoint to update the recent login timestamp
    @PutMapping("/update-login")
    fun updateRecentLogin(
        @RequestParam username: String
    ): ResponseEntity<Void> {
        return try {
            usersService.updateRecentLogin(username, LocalDateTime.now())
            ResponseEntity(HttpStatus.OK)
        } catch (ex: Exception) {
            ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    // Endpoint to fetch user parameters (settings)
    @GetMapping("/params/{userId}")
    fun fetchUserParams(@PathVariable userId: Int): ResponseEntity<UserParams?> {
        val userParams = usersService.fetchUserParams(userId)
        return ResponseEntity(userParams, if (userParams != null) HttpStatus.OK else HttpStatus.NOT_FOUND)
    }

    // Endpoint to fetch user info along with subscriptions and genres
    @GetMapping("/info/{userId}")
    fun getUserInfo(@PathVariable userId: Int): ResponseEntity<UserInfo?> {
        val userInfo = usersService.getUserInfo(userId)
        return ResponseEntity(userInfo, if (userInfo != null) HttpStatus.OK else HttpStatus.NOT_FOUND)
    }
}