package com.example.books.library.restcontrollers

import com.example.books.library.DBConnection
import com.example.books.library.interfaces.LibrariesInterface
import com.example.books.library.models.Library
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class LibrariesRestController {

    companion object {
        val logger = LoggerFactory.getLogger(LibrariesRestController::class.java)
        val gson = GsonBuilder().setDateFormat(DBConnection.dateFormat).create()!!
    }

    val librariesInterface = object : LibrariesInterface {
        override fun getLibraryByUserId(id: Long): Long {
            val sqlString = "SELECT LIBRARY_ID FROM LIBRARIES WHERE USER_ID = ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, id)
            val resultSet = prepStmt.executeQuery()
            resultSet.first()
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
}