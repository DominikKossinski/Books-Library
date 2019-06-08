package com.example.books.library.restcontrollers

import com.example.books.library.DBConnection
import com.example.books.library.models.User
import com.google.gson.JsonObject
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
class LogoutRestController {

    companion object {
        val logger = LoggerFactory.getLogger(LogoutRestController::class.java)!!
        val gson = DBConnection.gson
    }

    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.POST], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/logout"], method = [RequestMethod.POST],
            consumes = [MediaType.APPLICATION_JSON_UTF8_VALUE],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun logOut(@RequestBody userData: String): ResponseEntity<String> {
        val user = gson.fromJson(userData, User::class.java)
        logger.info("User ${user.name} logged out")
        SecurityContextHolder.getContext().authentication = null
        val response = JsonObject()
        response.addProperty("status", "ok")
        return ResponseEntity.ok().body(response.toString())
    }
}
