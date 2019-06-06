package com.example.books.library.restcontrollers

import com.example.books.library.DBConnection
import com.example.books.library.interfaces.BooksInterface
import com.example.books.library.models.Book
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
        val gson = DBConnection.gson
    }

    val booksInterface = object : BooksInterface {
        override fun getBooksByPatterns(book: Book): ArrayList<Book> {
            val sqlString = "SELECT BOOK_ID, TITLE, ISBN, AUTHOR, PAGES_COUNT FROM books where LOWER(TITLE) LIKE ? AND LOWER(AUTHOR) LIKE ? AND LOWER(ISBN) LIKE ?"
            DBConnection.dbConnection!!.beginRequest()
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setString(1, "%${book.title.toLowerCase()}%")
            prepStmt.setString(2, "%${book.author.toLowerCase()}%")
            prepStmt.setString(3, "%${book.isbn.toLowerCase()}%")
            val resultSet = prepStmt.executeQuery()
            val books = ArrayList<Book>()
            while (resultSet.next()) {
                val bookFromSet = Book(resultSet.getLong("book_id"), resultSet.getString("title"),
                        resultSet.getString("author"), resultSet.getString("isbn"),
                        resultSet.getInt("pages_count"))
                books.add(bookFromSet)
            }
            DBConnection.dbConnection!!.commit()
            return books
        }

        override fun getBooksByPattern(pattern: String): ArrayList<Book> {
            val sqlString = "SELECT BOOK_ID, TITLE, ISBN, AUTHOR, PAGES_COUNT FROM books where LOWER(TITLE) LIKE ? OR LOWER(AUTHOR) LIKE ? OR LOWER(ISBN) LIKE ?"
            DBConnection.dbConnection!!.beginRequest()
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            for (i in 1..3) {
                prepStmt.setString(i, "%${pattern.toLowerCase()}%")
            }
            val resultSet = prepStmt.executeQuery()
            val books = ArrayList<Book>()
            while (resultSet.next()) {
                val book = Book(resultSet.getLong("book_id"), resultSet.getString("title"),
                        resultSet.getString("author"), resultSet.getString("isbn"),
                        resultSet.getInt("pages_count"))
                books.add(book)
            }
            DBConnection.dbConnection!!.commit()
            return books
        }

        override fun getBookById(id: Long): Book? {
            val sqlString = "SELECT BOOK_ID, TITLE, ISBN, AUTHOR, PAGES_COUNT FROM books where BOOK_ID = ?"
            DBConnection.dbConnection!!.beginRequest()
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, id)
            val resultSet = prepStmt.executeQuery()
            var count = 0
            while (resultSet.next()) {
                count++
            }
            DBConnection.dbConnection!!.commit()
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
            DBConnection.dbConnection!!.beginRequest()
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setString(1, isbn)
            val resultSet = prepStmt.executeQuery()
            var count = 0
            while (resultSet.next()) {
                count++
            }
            DBConnection.dbConnection!!.commit()
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
            DBConnection.dbConnection!!.beginRequest()
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString, Statement.RETURN_GENERATED_KEYS)
            prepStmt.setString(1, book.title)
            prepStmt.setString(2, book.isbn)
            prepStmt.setString(3, book.author)
            prepStmt.setInt(4, book.pagesCount)
            val count = prepStmt.executeUpdate()
            return if (count == 1) {
                val keys = prepStmt.generatedKeys
                keys.first()
                DBConnection.dbConnection!!.commit()
                return keys.getLong(1)
            } else {
                DBConnection.dbConnection!!.rollback()
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

    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.POST], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/getBooksByPatterns"], method = [RequestMethod.POST],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun getBooksByPatterns(@RequestBody bookData: String): ResponseEntity<String> {
        val book = gson.fromJson(bookData, Book::class.java)
        val books = booksInterface.getBooksByPatterns(book)
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