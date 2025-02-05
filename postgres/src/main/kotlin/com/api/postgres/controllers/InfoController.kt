package com.api.postgres.controllers


import com.api.postgres.ApiResponse
import com.api.postgres.InfoDto
import com.api.postgres.MediaDto
import com.api.postgres.PersonDto
import com.api.postgres.services.Information
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/info")
class InfoController(private val informationService: Information) {

    private val logger: Logger = LoggerFactory.getLogger(InfoController::class.java)

    @PostMapping("/save")
    suspend fun saveInfo(@RequestBody infoDto: InfoDto): ResponseEntity<ApiResponse> {
        return try {
            informationService.insertInfo(infoDto)
            ResponseEntity.ok(ApiResponse(
                success = true,
                message = "Information saved successfully"
            ))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse(
                success = false,
                message = "Error saving information: ${e.message}"
            ))
        }
    }

    @PostMapping("/create")
    fun createDto(
        @RequestParam tmdbId: Int,
        @RequestParam type: String,
        @RequestBody jsonData: String
    ): ResponseEntity<Any> {
        return try {
            val dto = informationService.createDto(tmdbId, type, jsonData)
            when (dto) {
                is PersonDto -> ResponseEntity.ok(dto)
                is MediaDto -> ResponseEntity.ok(dto)
                else -> ResponseEntity.badRequest().body("Invalid DTO type")
            }
        } catch (e: Exception) {
            ResponseEntity.badRequest().body("Error creating DTO: ${e.message}")
        }
    }
    
}