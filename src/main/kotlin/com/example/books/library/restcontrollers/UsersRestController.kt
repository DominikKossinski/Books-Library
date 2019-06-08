package com.example.books.library.restcontrollers

import com.example.books.library.DBConnection
import com.example.books.library.NoConfirmationThread
import com.example.books.library.NotificationService
import com.example.books.library.interfaces.UsersInterface
import com.example.books.library.models.Book
import com.example.books.library.models.Item
import com.example.books.library.models.User
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.mail.MailException
import org.springframework.web.bind.annotation.*
import java.sql.Statement

@RestController
class UsersRestController {

    @Autowired
    private var notificationService: NotificationService? = null

    companion object {
        val logger = LoggerFactory.getLogger(UsersRestController::class.java)!!
        val gson = DBConnection.gson
        val passwordRegex = "^[a-zA-Z0-9]+$".toRegex()
        val nameRegex = "^[a-zA-Z0-9]+$".toRegex()
        val emailReqex = "[a-zA-Z0-9._%\\-+]{1,256}@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+".toRegex()
    }

    var usersInterface = object : UsersInterface {
        override fun getUserByLibId(libId: Long): User? {
            val sqlString = "SELECT u.USER_ID, NAME, EMAIL, STATUS FROM USERS u JOIN libraries l on u.USER_ID = l.USER_ID WHERE LIBRARY_ID = ?"
            DBConnection.dbConnection!!.beginRequest()
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, libId)
            val resultSet = prepStmt.executeQuery()
            var count = 0
            while (resultSet.next()) {
                count++
            }
            return if (count == 1) {
                resultSet.first()
                DBConnection.dbConnection!!.commit()
                User(resultSet.getLong("user_id"), resultSet.getString("name"), resultSet.getString("email"), "", resultSet.getInt("status"))
            } else {
                DBConnection.dbConnection!!.rollback()
                null
            }
        }

        override fun getAvgBooks(id: Long): Float {
            val sqlString = "SELECT COUNT(PAGES_COUNT) FROM books b JOIN items i ON b.BOOK_ID = i.ITEM_ID JOIN loans l on i.ITEM_ID = l.ITEM_ID where USER_ID = ? AND l.END_DATE IS NOT NULL AND l.END_DATE > DATE_ADD(CURDATE(), INTERVAL -1 YEAR)"
            DBConnection.dbConnection!!.beginRequest()
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, id)
            val resultSet = prepStmt.executeQuery()
            var count = 0
            while (resultSet.next()) {
                count++
            }
            logger.info("Average: count = $count")
            return if (count == 1) {
                resultSet.first()
                DBConnection.dbConnection!!.commit()
                resultSet.getFloat(1) / 12
            } else {
                DBConnection.dbConnection!!.rollback()
                0.0f
            }
        }

        override fun getAvg(id: Long): Float {
            val sqlString = "SELECT SUM(PAGES_COUNT) FROM books b JOIN items i ON b.BOOK_ID = i.ITEM_ID JOIN loans l on i.ITEM_ID = l.ITEM_ID where l.USER_ID = ? AND l.END_DATE IS NOT NULL AND l.END_DATE > DATE_ADD(CURDATE(), INTERVAL -1 YEAR)"
            DBConnection.dbConnection!!.beginRequest()
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, id)
            val resultSet = prepStmt.executeQuery()
            var count = 0
            while (resultSet.next()) {
                count++
            }
            logger.info("Average: count = $count")
            return if (count == 1) {
                resultSet.first()
                DBConnection.dbConnection!!.commit()
                resultSet.getFloat(1) / 12
            } else {
                DBConnection.dbConnection!!.rollback()
                0.0f
            }
        }

        override fun getLastBook(id: Long): Item? {
            val sqlString = "SELECT ITEM_ID, i.LIBRARY_ID, i.BOOK_ID, BOOK_STATUS, ACT_LIB_ID, COMMENT, START_DATE, END_DATE, TITLE, ISBN, AUTHOR, PAGES_COUNT FROM items i JOIN books b on i.BOOK_ID = b.BOOK_ID JOIN libraries l on i.LIBRARY_ID = l.LIBRARY_ID WHERE l.USER_ID = ? ORDER BY END_DATE DESC LIMIT 1"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, id)
            val resultSet = prepStmt.executeQuery()
            var count = 0
            while (resultSet.next()) {
                count++
            }
            return if (count == 1) {
                resultSet.first()
                val book = Book(resultSet.getLong("book_id"), resultSet.getString("title"),
                        resultSet.getString("author"), resultSet.getString("isbn"), resultSet.getInt("pages_count"))
                val item = Item(resultSet.getLong("item_id"), resultSet.getLong("library_id"),
                        resultSet.getLong("book_id"), resultSet.getString("book_status"),
                        resultSet.getLong("act_lib_id"), resultSet.getString("comment"),
                        resultSet.getDate("start_date"), resultSet.getDate("end_date"), book)
                DBConnection.dbConnection!!.commit()
                item
            } else {
                DBConnection.dbConnection!!.rollback()
                null
            }
        }

        override fun getReadingBooks(id: Long): ArrayList<Item> {
            val sqlString = "SELECT ITEM_ID, i.LIBRARY_ID, i.BOOK_ID, BOOK_STATUS, ACT_LIB_ID, COMMENT, START_DATE, END_DATE, TITLE, ISBN, AUTHOR, PAGES_COUNT FROM items i JOIN books b on i.BOOK_ID = b.BOOK_ID JOIN libraries l on i.LIBRARY_ID = l.LIBRARY_ID WHERE l.USER_ID = ? AND i.START_DATE IS NOT NULL AND i.END_DATE IS NULL"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, id)
            val resultSet = prepStmt.executeQuery()
            val items = ArrayList<Item>()
            while (resultSet.next()) {
                val book = Book(resultSet.getLong("book_id"), resultSet.getString("title"),
                        resultSet.getString("author"), resultSet.getString("isbn"), resultSet.getInt("pages_count"))
                val item = Item(resultSet.getLong("item_id"), resultSet.getLong("library_id"),
                        resultSet.getLong("book_id"), resultSet.getString("book_status"),
                        resultSet.getLong("act_lib_id"), resultSet.getString("comment"),
                        resultSet.getDate("start_date"), resultSet.getDate("end_date"), book)
                DBConnection.dbConnection!!.commit()
                items.add(item)
            }
            return items
        }

        override fun getUserByEmail(email: String): User? {
            val sqlString = "SELECT USER_ID, NAME, EMAIL, STATUS FROM USERS WHERE EMAIL = ?"
            DBConnection.dbConnection!!.beginRequest()
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setString(1, email)
            val resultSet = prepStmt.executeQuery()
            var count = 0
            while (resultSet.next()) {
                count++
            }
            return if (count == 1) {
                resultSet.first()
                DBConnection.dbConnection!!.commit()
                User(resultSet.getLong("user_id"), resultSet.getString("name"), resultSet.getString("email"), "", resultSet.getInt("status"))
            } else {
                DBConnection.dbConnection!!.rollback()
                null
            }
        }

        override fun getUserById(id: Long): User? {
            val sqlString = "SELECT USER_ID, NAME, EMAIL, STATUS FROM USERS WHERE USER_ID = ?"
            DBConnection.dbConnection!!.beginRequest()
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, id)
            val resultSet = prepStmt.executeQuery()
            var count = 0
            while (resultSet.next()) {
                count++
            }
            return if (count == 1) {
                resultSet.first()
                DBConnection.dbConnection!!.commit()
                User(id, resultSet.getString("name"), resultSet.getString("email"), "", resultSet.getInt("status"))
            } else {
                DBConnection.dbConnection!!.rollback()
                null
            }

        }

        override fun getUserPassword(id: Long): String {
            val sqlString = "SELECT PASSWORD FROM users WHERE USER_ID = ?"
            DBConnection.dbConnection!!.beginRequest()
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, id)
            val resultSet = prepStmt.executeQuery()
            resultSet.first()
            DBConnection.dbConnection!!.commit()
            return resultSet.getString("password")
        }

        override fun checkConfirmed(user: User): Boolean {
            DBConnection.dbConnection!!.beginRequest()
            val sqlString = "SELECT STATUS FROM users WHERE USER_ID = ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, user.id)
            val resultSet = prepStmt.executeQuery()
            var count = 0
            while (resultSet.next()) {
                count++
            }
            return if (count == 1) {
                resultSet.first()
                DBConnection.dbConnection!!.commit()
                resultSet.getInt(1) == 1
            } else {
                DBConnection.dbConnection!!.rollback()
                false
            }
        }

        override fun confirmAccount(user: User): Boolean {
            DBConnection.dbConnection!!.beginRequest()
            val sqlString = "UPDATE USERS SET  STATUS = 1 WHERE NAME = ? AND USER_ID = ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setString(1, user.name)
            prepStmt.setLong(2, user.id)
            val count = prepStmt.executeUpdate()

            return if (count == 1) {
                DBConnection.dbConnection!!.commit()
                true
            } else {
                DBConnection.dbConnection!!.rollback()
                false
            }
        }

        override fun createUser(user: User): Long {
            val sqlString = "INSERT INTO users(name, email, password, status) values (?, ?, ?, 0)"
            DBConnection.dbConnection!!.beginRequest()
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString, Statement.RETURN_GENERATED_KEYS)
            prepStmt.setString(1, user.name)
            prepStmt.setString(2, user.email)
            prepStmt.setString(3, user.password)
            val count = prepStmt.executeUpdate()
            if (count == 1) {
                val set = prepStmt.generatedKeys
                set.first()
                val id = set.getLong(1)
                val libString = "INSERT INTO libraries(USER_ID) VALUES(?)"
                val stmt = DBConnection.dbConnection!!.prepareStatement(libString)
                stmt.setLong(1, id)
                val libs = stmt.executeUpdate()
                if (libs == 1) {
                    DBConnection.dbConnection!!.commit()
                    return id
                } else {
                    DBConnection.dbConnection!!.rollback()
                    return -2
                }
            }
            DBConnection.dbConnection!!.rollback()
            return -1
        }


        override fun checkFreeName(name: String): Boolean {
            DBConnection.dbConnection!!.beginRequest()
            val sqlString = "SELECT NAME FROM USERS WHERE NAME = ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setString(1, name)
            val resultSet = prepStmt.executeQuery()
            var count = 0
            while (resultSet.next()) {
                count++
            }
            DBConnection.dbConnection!!.commit()
            return count == 0
        }

        override fun checkFreeEmail(email: String): Boolean {
            DBConnection.dbConnection!!.beginRequest()
            val sqlString = "SELECT EMAIL FROM USERS WHERE EMAIL = ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setString(1, email)
            val resultSet = prepStmt.executeQuery()
            var count = 0
            while (resultSet.next()) {
                count++
            }
            DBConnection.dbConnection!!.commit()
            return count == 0
        }

        override fun getUserByName(name: String): User? {
            DBConnection.dbConnection!!.beginRequest()
            val sqlString = "SELECT users.USER_ID, NAME, EMAIL, LIBRARY_ID FROM USERS JOIN LIBRARIES ON users.USER_ID = libraries.USER_ID WHERE STATUS = 1 AND NAME = ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setString(1, name)
            val resultSet = prepStmt.executeQuery()
            var count = 0
            while (resultSet.next()) {
                count++
            }
            DBConnection.dbConnection!!.commit()
            return if (count == 1) {
                resultSet.first()
                User(resultSet.getLong("USER_ID"), resultSet.getString("NAME"),
                        resultSet.getString("EMAIL"), "", 1, resultSet.getLong("library_id"))
            } else {
                null
            }
        }
    }

    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.POST], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/addUser"], method = [RequestMethod.POST],
            consumes = [MediaType.APPLICATION_JSON_UTF8_VALUE],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun addUser(@RequestBody userData: String): ResponseEntity<String> {
        val user = gson.fromJson(userData, User::class.java)
        logger.info("Creating user: $user")
        val check = checkNameString(user.name).contentEquals("name free") && checkEmailString(user.email).contentEquals("email free")
                && checkPasswordString(user.password).contentEquals("true")
        val response = JsonObject()
        if (check) {
            val insert = usersInterface.createUser(user)
            logger.info("Insert resule $insert")
            if (insert > 0.toLong()) {
                user.id = insert
                user.password = ""
                try {
                    notificationService!!.sendCreateNotification(user.name, user.email, user.id.toString())
                } catch (e: MailException) {
                    e.printStackTrace()
                    response.addProperty("status", "error")
                    response.addProperty("description", "no email")
                    return ResponseEntity.ok(response.toString())
                }
                val thread = NoConfirmationThread(user.id)
                thread.start()
                response.addProperty("status", "ok")
                val userObject = gson.toJsonTree(user, User::class.java)
                response.add("user", userObject)
            } else if (insert == (-1).toLong()) {
                response.addProperty("status", "error")
                response.addProperty("description", "check user data")
            } else {
                response.addProperty("status", "error")
                response.addProperty("description", "error by adding lib")
            }
        } else {
            response.addProperty("status", "error")
            response.addProperty("description", "check user data")
        }
        return ResponseEntity.ok(response.toString())
    }

    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.POST], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/checkName"], method = [RequestMethod.POST],
            consumes = [MediaType.APPLICATION_JSON_UTF8_VALUE],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun checkFreeUserName(@RequestBody requestBody: String): ResponseEntity<String> {
        val nameObject = JsonParser().parse(requestBody).asJsonObject
        val response = JsonObject()
        val name = nameObject.get("name").asString
        response.addProperty("status", "ok")
        val checked = checkNameString(name)
        response.addProperty("name", checked)
        return ResponseEntity.ok(response.toString())
    }

    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.POST], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/confirmAccount"], method = [RequestMethod.POST],
            consumes = [MediaType.APPLICATION_JSON_UTF8_VALUE],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun confirmAccount(@RequestBody userData: String): ResponseEntity<String> {
        val user = gson.fromJson(userData, User::class.java)
        logger.info("Confirming accout $user")
        val confirmed = usersInterface.checkConfirmed(user)
        val response = JsonObject()
        if (confirmed) {
            response.addProperty("status", "already confirmed")
            return ResponseEntity.ok(response.toString())
        }
        val result = usersInterface.confirmAccount(user)
        if (result) {
            addUserToInMemory(user)
            response.addProperty("status", "ok")
        } else {
            response.addProperty("status", "error")
        }
        return ResponseEntity.ok(response.toString())
    }

    private fun addUserToInMemory(user: User) {
        val password = usersInterface.getUserPassword(user.id)
        val userDetails = org.springframework.security.core.userdetails.User.withUsername(user.name).password(password).roles("USER").build()
        DBConnection.inMemoryUserDetailsManager.createUser(userDetails)
    }

    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.POST], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/checkEmail"], method = [RequestMethod.POST],
            consumes = [MediaType.APPLICATION_JSON_UTF8_VALUE],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun checkEmailFree(@RequestBody requestBody: String): ResponseEntity<String> {
        val emailObject = JsonParser().parse(requestBody).asJsonObject
        val email = emailObject.get("email").asString
        val response = JsonObject()
        response.addProperty("status", "ok")
        val checked = checkEmailString(email)
        response.addProperty("email", checked)
        return ResponseEntity.ok(response.toString())
    }

    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.POST], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/checkPassword"], method = [RequestMethod.POST],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun checkPassword(@RequestBody requestBody: String): ResponseEntity<String> {
        val passwordObject = JsonParser().parse(requestBody).asJsonObject
        val password = passwordObject.get("password").asString
        val response = JsonObject()
        response.addProperty("status", "ok")
        val checked = checkPasswordString(password)
        response.addProperty("password", checked)
        return ResponseEntity.ok(response.toString())

    }

    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.GET], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/{userId}/getStats"], method = [RequestMethod.GET],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun getUserStats(@PathVariable("userId") userId: Long): ResponseEntity<String> {
        val user = usersInterface.getUserById(userId)
        val response = JsonObject()
        if (user == null) {
            response.addProperty("status", "error")
            response.addProperty("description", "no user")
        } else {
            val avg = usersInterface.getAvg(user.id)
            val avgBooks = usersInterface.getAvgBooks(user.id)
            val lastBook = usersInterface.getLastBook(user.id)
            val readingBooks = usersInterface.getReadingBooks(user.id)
            val lastBookObject = gson.toJsonTree(lastBook, Item::class.java)
            val readingArray = gson.toJsonTree(readingBooks).asJsonArray
            response.addProperty("status", "ok")
            response.addProperty("average", avg)
            response.addProperty("averageBooks", avgBooks)
            response.add("lastItem", lastBookObject)
            response.add("readingBooks", readingArray)
        }
        return ResponseEntity.ok(response.toString())
    }

    fun checkPasswordString(password: String): String {
        return when {
            password.isEmpty() -> "empty password"
            password.length < 8 -> "to short password"
            password.length > 20 -> "to long password"
            passwordRegex.matches(password) -> "true"
            else -> "false"
        }
    }

    fun checkNameString(name: String): String {
        return when {
            name.isEmpty() -> "empty name"
            name.length > 20 -> "to long name"
            nameRegex.matches(name) -> {
                if (usersInterface.checkFreeName(name)) {
                    "name free"
                } else {
                    "name not free"
                }
            }
            else -> "invalid character"
        }
    }

    fun checkEmailString(email: String): String {
        return when {
            email.isEmpty() -> "empty email"
            emailReqex.matches(email) -> {
                if (usersInterface.checkFreeEmail(email)) {
                    "email free"
                } else {
                    "email not free"
                }
            }
            else -> "false"
        }
    }
}