package com.api.postgres.controllers


import com.api.postgres.services.UsersService
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UsersController(private val usersService: UsersService) {

    // Endpoint to add a new user
    @PostMapping("/add")
    fun addUser(@RequestBody userData: UserData): ResponseEntity<Int> = runBlocking {
        val userId = usersService.addUser(userData)
        ResponseEntity.ok(userId)
    }

    // Endpoint to check user credentials
    @PostMapping("/check-credentials")
    fun checkUserCredentials(
        @RequestParam username: String,
        @RequestParam password: String
    ): ResponseEntity<Boolean> = runBlocking {
        val isValid = usersService.checkUserCredentials(username, password)
        ResponseEntity.ok(isValid)
    }

    // Endpoint to update recent login timestamp
    @PutMapping("/update-login")
    fun updateRecentLogin(@RequestParam username: String): ResponseEntity<String> = runBlocking {
        usersService.updateRecentLogin(username)
        ResponseEntity.ok("Recent login timestamp updated for user: $username")
    }

    // Endpoint to fetch user parameters (settings)
    @GetMapping("/{userId}/params")
    fun fetchUserParams(@PathVariable userId: Int): ResponseEntity<UserParams?> = runBlocking {
        val userParams = usersService.fetchUserParams(userId)
        ResponseEntity.ok(userParams)
    }

    // Endpoint to get the providers by priority for a specific user
    @GetMapping("/{userId}/providers")
    fun getProvidersByPriority(@PathVariable userId: Int): ResponseEntity<List<Int>> = runBlocking {
        val providers = usersService.getProvidersByPriority(userId)
        ResponseEntity.ok(providers)
    }

    // Endpoint to fetch all user information by userId
    @GetMapping("/{userId}/info")
    fun getUserInfo(@PathVariable userId: Int): ResponseEntity<UserInfo?> = runBlocking {
        val userInfo = usersService.getUserInfo(userId)
        ResponseEntity.ok(userInfo)
    }
}
