package com.example.books.library.restcontrollers

import com.example.books.library.DBConnection
import com.example.books.library.interfaces.LendingsInterface
import com.example.books.library.models.Lending
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.*
import kotlin.collections.ArrayList

@RestController
class LendingsRestController {


    companion object {
        val logger = LoggerFactory.getLogger(LendingsRestController::class.java)
        val gson = GsonBuilder().setDateFormat(DBConnection.dateFormat).create()
    }

    val lendingsInterface = object : LendingsInterface {
        override fun getItemStatus(itemId: Long): String {
            val sqlString = "SELECT BOOK_STATUS FROM items WHERE ITEM_ID = ?"
            DBConnection.dbConnection!!.beginRequest()
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, itemId)
            val resultSet = prepStmt.executeQuery()
            var count = 0
            while (resultSet.next()) {
                count++
            }
            DBConnection.dbConnection!!.commit()
            return when (count) {
                0 -> "no item"
                1 -> {
                    resultSet.first()
                    resultSet.getString("book_status")
                }
                else -> "error"
            }
        }


        override fun getItemHistory(itemId: Long): ArrayList<Lending> {
            val sqlString = "SELECT LENDING_ID, USER_ID, ITEM_ID, START_DATE, END_DATE FROM lendings WHERE ITEM_ID = ?"
            DBConnection.dbConnection!!.beginRequest()
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, itemId)
            val lendings = ArrayList<Lending>()
            val resultSet = prepStmt.executeQuery()
            while (resultSet.next()) {
                val startDate = Date(resultSet.getDate("start_date").time)
                var endDate: Date? = null
                if (resultSet.getDate("end_date") != null) {
                    endDate = Date(resultSet.getDate("end_date").time)
                }
                val lending = Lending(resultSet.getLong("lendingId"), resultSet.getLong("user_id"),
                        resultSet.getLong("item_id"), startDate, endDate)
                lendings.add(lending)
            }
            DBConnection.dbConnection!!.commit()
            return lendings
        }

        override fun lendItem(lending: Lending, status: String, actLibId: Long): Long {
            if (!status.contentEquals("outside")) {
                var sqlString = "INSERT INTO lendings(USER_ID, ITEM_ID, START_DATE) VALUES (?, ?, CURDATE())"
                var prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
                prepStmt.setLong(1, lending.userId)
                prepStmt.setLong(2, lending.itemId)
                DBConnection.dbConnection!!.beginRequest()
                var count = prepStmt.executeUpdate()
                if (count == 1) {
                    sqlString = "UPDATE ITEMS SET BOOK_STATUS = ?, ACT_LIB_ID = ? WHERE ITEM_ID = ?"
                    prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
                    prepStmt.setString(1, status)
                    prepStmt.setLong(2, actLibId)
                    prepStmt.setLong(3, lending.itemId)
                    count = prepStmt.executeUpdate()
                    return if (count == 1) {
                        DBConnection.dbConnection!!.commit()
                        0
                    } else {
                        DBConnection.dbConnection!!.rollback()
                        -2
                    }
                } else {
                    DBConnection.dbConnection!!.rollback()
                    -2
                }
            }
            DBConnection.dbConnection!!.beginRequest()
            val sqlString = "UPDATE ITEMS SET BOOK_STATUS = ? WHERE ITEM_ID = ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setString(1, status)
            prepStmt.setLong(2, lending.itemId)
            val count = prepStmt.executeUpdate()
            if (count == 1) {
                DBConnection.dbConnection!!.commit()
                return 0
            }
            DBConnection.dbConnection!!.rollback()
            return -1
        }

        override fun endLending(lending: Lending): Long {
            if (lending.userId != (-1).toLong()) {
                var sqlString = "UPDATE lendings SET END_DATE = CURDATE() WHERE ITEM_ID = ? AND END_DATE IS NULL"
                var prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
                DBConnection.dbConnection!!.beginRequest()
                prepStmt.setLong(1, lending.itemId)
                var count = prepStmt.executeUpdate()
                if (count == 1) {
                    sqlString = "UPDATE ITEMS SET BOOK_STATUS = '', ACT_LIB_ID = NULL WHERE ITEM_ID = ?"
                    prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
                    prepStmt.setLong(1, lending.itemId)
                    count = prepStmt.executeUpdate()
                    return if (count == 1) {
                        DBConnection.dbConnection!!.commit()
                        0
                    } else {
                        DBConnection.dbConnection!!.rollback()
                        -2
                    }
                }
            }
            val sqlString = "UPDATE ITEMS SET BOOK_STATUS = '', ACT_LIB_ID = NULL WHERE ITEM_ID = ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, lending.itemId)
            val count = prepStmt.executeUpdate()
            if (count == 1) {
                DBConnection.dbConnection!!.commit()
                return 0
            }
            DBConnection.dbConnection!!.rollback()
            return -1
        }

    }


    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.POST], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/lendItem"], method = [RequestMethod.POST],
            consumes = [MediaType.APPLICATION_JSON_UTF8_VALUE],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun lendItem(@RequestBody lendingData: String): ResponseEntity<String> {
        val lending = gson.fromJson(lendingData, Lending::class.java)
        val itemStatus = lendingsInterface.getItemStatus(lending.itemId)
        val response = JsonObject()
        if (itemStatus.contentEquals("no item")) {
            response.addProperty("status", "error")
            response.addProperty("description", "no item")
            return ResponseEntity.ok(response.toString())
        }
        val authentication = SecurityContextHolder.getContext().authentication
        val user = UsersRestController().usersInterface.getUserByName(authentication.name)
        var bookStatus = "outside"
        var libId = (-1).toLong()
        if (user != null) {
            bookStatus = "Lend to " + user.name
            libId = user.libId
        }
        logger.info("Name: ${authentication.name}, Lending: $lending, Status: $bookStatus, LibId: $libId")
        val lend = lendingsInterface.lendItem(lending, bookStatus, libId)
        if (lend == 0.toLong()) {
            response.addProperty("status", "ok")
        } else {
            response.addProperty("status", "error")
            response.addProperty("description", "error by lending")
        }
        return ResponseEntity.ok(response.toString())
    }

    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.POST], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/endLending"], method = [RequestMethod.POST],
            consumes = [MediaType.APPLICATION_JSON_UTF8_VALUE],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun endLending(@RequestBody lendingData: String): ResponseEntity<String> {
        val lending = gson.fromJson(lendingData, Lending::class.java)
        val authentication = SecurityContextHolder.getContext().authentication
        val user = UsersRestController().usersInterface.getUserByName(authentication.name)
        if (user == null) {
            lending.userId = -1
        }
        val status = lendingsInterface.endLending(lending)
        val response = JsonObject()
        if (status == 0.toLong()) {
            response.addProperty("status", "ok")
        } else {
            response.addProperty("status", "error")
            response.addProperty("description", "error by ending lending")
        }
        return ResponseEntity.ok(response.toString())
    }


}