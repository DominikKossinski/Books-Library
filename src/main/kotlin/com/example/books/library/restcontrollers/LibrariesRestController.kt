package com.example.books.library.restcontrollers

import com.example.books.library.DBConnection
import com.example.books.library.interfaces.LibrariesInterface
import com.example.books.library.models.Library
import com.example.books.library.models.User
import com.google.gson.JsonObject
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class LibrariesRestController {

    companion object {
        val logger = LoggerFactory.getLogger(LibrariesRestController::class.java)!!
        val gson = DBConnection.gson
    }

    val librariesInterface = object : LibrariesInterface {
        override fun getUsersLibrariesByUserId(userId: Long): ArrayList<User> {
            DBConnection.dbConnection!!.beginRequest()
            val sqlString = "SELECT l.USER_ID, NAME, EMAIL, l.LIBRARY_ID FROM users u JOIN libraries l on u.USER_ID = l.USER_ID JOIN members m on l.LIBRARY_ID = m.LIBRARY_ID where m.USER_ID = ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, userId)
            val users = ArrayList<User>()
            val resultSet = prepStmt.executeQuery()
            while (resultSet.next()) {
                val user = User(resultSet.getLong("user_id"), resultSet.getString("name"),
                        resultSet.getString("email"), "", 1, resultSet.getLong("library_id"))
                users.add(user)
            }
            DBConnection.dbConnection!!.commit()
            return users
        }

        override fun getLibraryByUserId(id: Long): Long {
            DBConnection.dbConnection!!.beginRequest()
            val sqlString = "SELECT LIBRARY_ID FROM LIBRARIES WHERE USER_ID = ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, id)
            val resultSet = prepStmt.executeQuery()
            resultSet.first()
            DBConnection.dbConnection!!.commit()
            return resultSet.getLong("library_id")
        }

    }

    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.GET], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/{id}/getLibrary"], method = [RequestMethod.GET],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun getLibraryByUserId(@PathVariable("id") id: Long): ResponseEntity<String> {
        val libraryId = librariesInterface.getLibraryByUserId(id)
        val library = Library(id, libraryId)
        val libraryObject = gson.toJsonTree(library, Library::class.java)
        val response = JsonObject()
        response.addProperty("status", "ok")
        response.add("library", libraryObject)
        return ResponseEntity.ok(response.toString())
    }

    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.GET], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/{id}/getLibraries"], method = [RequestMethod.GET],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun getLibrariesByUserId(@PathVariable("id") userId: Long): ResponseEntity<String> {
        val users = librariesInterface.getUsersLibrariesByUserId(userId)
        val usersArray = gson.toJsonTree(users).asJsonArray
        val response = JsonObject()
        response.addProperty("status", "ok")
        response.add("libraries", usersArray)
        return ResponseEntity.ok(response.toString())
    }
}