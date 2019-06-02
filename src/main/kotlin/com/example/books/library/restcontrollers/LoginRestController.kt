package com.example.books.library.restcontrollers

import com.example.books.library.DBConnection
import com.example.books.library.models.User
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
class LoginRestController {

    companion object {
        val logger = LoggerFactory.getLogger(LoginRestController::class.java)
        val gson = GsonBuilder().setDateFormat(DBConnection.dateFormat).create()
    }


    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.POST], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/login"], method = [RequestMethod.POST],
            consumes = [MediaType.APPLICATION_JSON_UTF8_VALUE],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun logIn(@RequestBody userData: String): ResponseEntity<String> {
        var user = gson.fromJson(userData, User::class.java)
        val inMemoryUserDetails = DBConnection.inMemoryUserDetailsManager
        val response = JsonObject()
        if (inMemoryUserDetails.userExists(user.name)) {
            val userDetails = inMemoryUserDetails.loadUserByUsername(user.name)
            if (user.password.contentEquals(userDetails.password)) {
                val authentication = UsernamePasswordAuthenticationToken(
                        userDetails.username, userDetails.password, userDetails.authorities)
                SecurityContextHolder.getContext().authentication = authentication
                user = UsersRestController().usersInterface.getUserByName(user.name)
                val userObject = gson.toJsonTree(user, User::class.java)
                logger.info("User ${user.name} logged")
                response.addProperty("status", "ok")
                response.add("user", userObject)
            } else {
                response.addProperty("status", "error")
                response.addProperty("description", "wrong password")
            }
        } else {
            response.addProperty("status", "error")
            response.addProperty("description", "no user")
        }
        return ResponseEntity.ok().body(response.toString())
    }
}
