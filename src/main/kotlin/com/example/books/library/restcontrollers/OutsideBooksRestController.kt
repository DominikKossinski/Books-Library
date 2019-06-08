package com.example.books.library.restcontrollers

import com.example.books.library.DBConnection
import com.example.books.library.interfaces.OutsideBooksInterface
import com.example.books.library.models.OutsideBook
import com.google.gson.JsonObject
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.sql.Statement

@RestController
class OutsideBooksRestController {

    companion object {
        val logger = LoggerFactory.getLogger(OutsideBooksRestController::class.java)!!
        val gson = DBConnection.gson
        val titleRegex = "^[0-9a-zA-Z ,.]{1,50}$".toRegex()
        val isbnRegex = "^[0-9]{13}$".toRegex()
    }

    val outsideBooksInterface = object : OutsideBooksInterface {
        override fun addBookFromOudside(book: OutsideBook): Long {
            DBConnection.dbConnection!!.beginRequest()
            val sqlString = "INSERT INTO books_from_outside(LIBRARY_ID, TITLE, ISBN, AUTHOR) VALUES (?, ? , ?, ?)"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString, Statement.RETURN_GENERATED_KEYS)
            prepStmt.setLong(1, book.libraryId)
            prepStmt.setString(2, book.title)
            prepStmt.setString(3, book.isbn)
            prepStmt.setString(4, book.author)
            val count = prepStmt.executeUpdate()
            return if (count == 1) {
                DBConnection.dbConnection!!.commit()
                val keys = prepStmt.generatedKeys
                keys.first()
                keys.getLong(1)
            } else {
                DBConnection.dbConnection!!.rollback()
                -1
            }
        }

        override fun getBooksFromOutside(libId: Long): ArrayList<OutsideBook> {
            DBConnection.dbConnection!!.beginRequest()
            val sqlString = "SELECT LIBRARY_ID, BOOK_ID, TITLE, ISBN, AUTHOR FROM books_from_outside where LIBRARY_ID = ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, libId)
            val books = ArrayList<OutsideBook>()
            val resultSet = prepStmt.executeQuery()
            while (resultSet.next()) {
                val book = OutsideBook(resultSet.getLong("library_id"), resultSet.getLong("book_id"),
                        resultSet.getString("title"), resultSet.getString("isbn"), resultSet.getString("author"))
                books.add(book)
            }
            return books
        }

    }


    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.POST], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/library/{libId}/addOutsideBook"], method = [RequestMethod.POST],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun addOutsideBook(@PathVariable("libId") libId: Long, @RequestBody bookData: String): ResponseEntity<String> {
        val book = gson.fromJson(bookData, OutsideBook::class.java)
        book.libraryId = libId
        val response = JsonObject()
        if (titleRegex.matches(book.title) && titleRegex.matches(book.author) && isbnRegex.matches(book.isbn)) {
            val status = outsideBooksInterface.addBookFromOudside(book)
            if (status > 0) {
                response.addProperty("status", "ok")
            } else {
                response.addProperty("status", "error")
                response.addProperty("description", "error by adding outside book")
            }
        } else {
            response.addProperty("status", "error")
            response.addProperty("description", "wrong data")
        }
        return ResponseEntity.ok(response.toString())
    }


    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.GET], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/library/{libId}/getOutsideBooks"], method = [RequestMethod.GET],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun getOutsideBooks(@PathVariable("libId") libId: Long): ResponseEntity<String> {
        val books = outsideBooksInterface.getBooksFromOutside(libId)
        val response = JsonObject()
        val booksArray = gson.toJsonTree(books).asJsonArray
        response.addProperty("status", "ok")
        response.add("books", booksArray)
        return ResponseEntity.ok(response.toString())
    }
}