package com.example.books.library.restcontrollers

import com.example.books.library.DBConnection
import com.example.books.library.NoConfirmationThread
import com.example.books.library.NotificationService
import com.example.books.library.interfaces.UsersInterface
import com.example.books.library.models.User
import com.google.gson.GsonBuilder
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
        val logger = LoggerFactory.getLogger(UsersRestController::class.java)
        val gson = GsonBuilder().setDateFormat(DBConnection.dateFormat).create()
        val passwordRegex = "^[a-zA-Z0-9]+$".toRegex()
        val nameRegex = "^[a-zA-Z0-9]+$".toRegex()
        val emailReqex = "[a-zA-Z0-9._%\\-+]{1,256}@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+".toRegex()
    }

    var usersInterface = object : UsersInterface {
        override fun getUserByEmail(email: String): User? {
            val sqlString = "SELECT USER_ID, NAME, EMAIL, STATUS FROM USERS WHERE EMAIL = ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setString(1, email)
            val resultSet = prepStmt.executeQuery()
            var count = 0
            while (resultSet.next()) {
                count++
            }
            return if (count == 1) {
                resultSet.first()
                User(resultSet.getLong("user_id"), resultSet.getString("name"), resultSet.getString("email"), "", resultSet.getInt("status"))
            } else {
                null
            }
        }

        override fun getUserById(id: Long): User? {
            val sqlString = "SELECT USER_ID, NAME, EMAIL, STATUS FROM USERS WHERE USER_ID = ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, id)
            val resultSet = prepStmt.executeQuery()
            var count = 0
            while (resultSet.next()) {
                count++
            }
            return if (count == 1) {
                resultSet.first()
                User(id, resultSet.getString("name"), resultSet.getString("email"), "", resultSet.getInt("status"))
            } else {
                null
            }

        }

        override fun getUserPassword(id: Long): String {
            val sqlString = "SELECT PASSWORD FROM users WHERE USER_ID = ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, id)
            val resultSet = prepStmt.executeQuery()
            resultSet.first()
            return resultSet.getString("password")
        }

        override fun checkConfirmed(user: User): Boolean {
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
                resultSet.getInt(1) == 1
            } else {
                false
            }
        }

        override fun confirmAccount(user: User): Boolean {
            val sqlString = "UPDATE USERS SET  STATUS = 1 WHERE NAME = ? AND USER_ID = ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setString(1, user.name)
            prepStmt.setLong(2, user.id)
            val count = prepStmt.executeUpdate()
            return count == 1
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
            val sqlString = "SELECT NAME FROM USERS WHERE NAME = ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setString(1, name)
            val resultSet = prepStmt.executeQuery()
            var count = 0
            while (resultSet.next()) {
                count++
            }
            return count == 0
        }

        override fun checkFreeEmail(email: String): Boolean {
            val sqlString = "SELECT EMAIL FROM USERS WHERE EMAIL = ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setString(1, email)
            val resultSet = prepStmt.executeQuery()
            var count = 0
            while (resultSet.next()) {
                count++
            }
            return count == 0
        }

        override fun getUserByName(name: String): User? {
            val sqlString = "SELECT users.USER_ID, NAME, EMAIL, LIBRARY_ID FROM USERS JOIN LIBRARIES ON users.USER_ID = libraries.USER_ID WHERE STATUS = 1 AND NAME = ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setString(1, name)
            val resultSet = prepStmt.executeQuery()
            var count = 0
            while (resultSet.next()) {
                count++
            }
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