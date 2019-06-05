package com.example.books.library.restcontrollers

import com.example.books.library.DBConnection
import com.example.books.library.interfaces.BooksInterface
import com.example.books.library.models.Book
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.sql.Statement

@RestController
class BooksRestController {

    companion object {
        val logger = LoggerFactory.getLogger(BooksRestController::class.java)
        val gson = GsonBuilder().setDateFormat(DBConnection.dateFormat).create()
    }

    val booksInterface = object : BooksInterface {
        override fun getBooksByPattern(pattern: String): ArrayList<Book> {
            val sqlString = "SELECT BOOK_ID, TITLE, ISBN, AUTHOR, PAGES_COUNT FROM books where TITLE LIKE ? OR AUTHOR LIKE ? OR ISBN LIKE ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            for (i in 1..3) {
                prepStmt.setString(i, "%$pattern%")
            }
            val resultSet = prepStmt.executeQuery()
            val books = ArrayList<Book>()
            while (resultSet.next()) {
                val book = Book(resultSet.getLong("book_id"), resultSet.getString("title"),
                        resultSet.getString("author"), resultSet.getString("isbn"),
                        resultSet.getInt("pages_count"))
                books.add(book)
            }
            return books
        }

        override fun getBookById(id: Long): Book? {
            val sqlString = "SELECT BOOK_ID, TITLE, ISBN, AUTHOR, PAGES_COUNT FROM books where BOOK_ID = ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, id)
            val resultSet = prepStmt.executeQuery()
            var count = 0
            while (resultSet.next()) {
                count++
            }
            return if (count == 1) {
                resultSet.first()
                Book(resultSet.getLong("book_id"), resultSet.getString("title"),
                        resultSet.getString("author"), resultSet.getString("isbn"),
                        resultSet.getInt("pages_count"))
            } else {
                null
            }
        }

        override fun getBookByISBN(isbn: String): Book? {
            val sqlString = "SELECT BOOK_ID, TITLE, ISBN, AUTHOR, PAGES_COUNT FROM books where ISBN = ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setString(1, isbn)
            val resultSet = prepStmt.executeQuery()
            var count = 0
            while (resultSet.next()) {
                count++
            }
            return if (count == 1) {
                resultSet.first()
                Book(resultSet.getLong("book_id"), resultSet.getString("title"),
                        resultSet.getString("author"), resultSet.getString("isbn"),
                        resultSet.getInt("pages_count"))
            } else {
                null
            }
        }

        override fun addBook(book: Book): Long {
            val sqlString = "INSERT INTO books(TITLE, ISBN, AUTHOR, PAGES_COUNT) VALUES (?, ?, ?, ?)"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString, Statement.RETURN_GENERATED_KEYS)
            prepStmt.setString(1, book.title)
            prepStmt.setString(2, book.isbn)
            prepStmt.setString(3, book.author)
            prepStmt.setInt(4, book.pagesCount)
            var count = prepStmt.executeUpdate()
            return if (count == 1) {
                val keys = prepStmt.generatedKeys
                keys.first()
                return keys.getLong(1)
            } else {
                -1
            }
        }

    }

    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.GET], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/getBooksByPattern"], method = [RequestMethod.GET],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun getBooksByPattern(@RequestParam("pattern") pattern: String): ResponseEntity<String> {
        val books = booksInterface.getBooksByPattern(pattern)
        val booksArray = gson.toJsonTree(books).asJsonArray
        val response = JsonObject()
        response.addProperty("status", "ok")
        response.add("books", booksArray)
        return ResponseEntity.ok(response.toString())
    }


    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.GET], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/getBooksById"], method = [RequestMethod.GET],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun getBookById(@RequestParam("id") bookId: Long): ResponseEntity<String> {
        val book = booksInterface.getBookById(bookId)
        val response = JsonObject()
        if (book != null) {
            val bookObject = gson.toJsonTree(book, Book::class.java)
            response.addProperty("status", "ok")
            response.add("book", bookObject)
        } else {
            response.addProperty("status", "error")
            response.addProperty("description", "no book")
        }
        return ResponseEntity.ok(response.toString())
    }


    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.GET], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/getBooksByISBN"], method = [RequestMethod.GET],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun getBookByISBN(@RequestParam("isbn") isbn: String): ResponseEntity<String> {
        val book = booksInterface.getBookByISBN(isbn)
        val response = JsonObject()
        if (book != null) {
            val bookObject = gson.toJsonTree(book, Book::class.java)
            response.addProperty("status", "ok")
            response.add("book", bookObject)
        } else {
            response.addProperty("status", "error")
            response.addProperty("description", "no book")
        }
        return ResponseEntity.ok(response.toString())
    }


}