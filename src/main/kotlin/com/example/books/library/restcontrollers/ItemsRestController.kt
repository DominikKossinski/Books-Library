package com.example.books.library.restcontrollers

import com.example.books.library.DBConnection
import com.example.books.library.interfaces.ItemsInterface
import com.example.books.library.models.Book
import com.example.books.library.models.Item
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class ItemsRestController {

    companion object {
        val logger = LoggerFactory.getLogger(ItemsRestController::class.java)
        val gson = GsonBuilder().setDateFormat(DBConnection.dateFormat).create()!!
    }

    var itemsInterface = object : ItemsInterface {
        override fun getItemById(id: Long): Item? {
            val sqlString = "SELECT ITEM_ID, LIBRARY_ID, I.BOOK_ID, BOOK_STATUS, ACT_LIB_ID, TITLE, ISBN, AUTHOR, PAGES_COUNT FROM ITEMS I JOIN BOOKS B ON I.BOOK_ID = B.BOOK_ID WHERE ITEM_ID = ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, id)
            val resultSet = prepStmt.executeQuery()
            resultSet.next()
            if (resultSet.isFirst) {
                resultSet.first()
                val book = Book(resultSet.getLong("book_id"), resultSet.getString("title"),
                        resultSet.getString("author"), resultSet.getString("isbn"), resultSet.getInt("pages_count"))
                return Item(resultSet.getLong("item_id"), resultSet.getLong("library_id"),
                        resultSet.getLong("book_id"), resultSet.getString("book_status"),
                        resultSet.getLong("act_lib_id"), book)
            } else {
                return null
            }
        }

        override fun getItemByLibrary(libId: Long): ArrayList<Item> {
            val sqlString = "SELECT ITEM_ID, LIBRARY_ID, I.BOOK_ID, BOOK_STATUS, ACT_LIB_ID, TITLE, ISBN, AUTHOR, PAGES_COUNT FROM ITEMS I JOIN BOOKS B ON I.BOOK_ID = B.BOOK_ID WHERE (LIBRARY_ID = ? and ACT_LIB_ID IS NULL) OR ACT_LIB_ID = ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, libId)
            prepStmt.setLong(2, libId)
            val resultSet = prepStmt.executeQuery()
            val items = ArrayList<Item>()
            while (resultSet.next()) {
                val book = Book(resultSet.getLong("book_id"), resultSet.getString("title"),
                        resultSet.getString("author"), resultSet.getString("isbn"), resultSet.getInt("pages_count"))
                val item = Item(resultSet.getLong("item_id"), resultSet.getLong("library_id"),
                        resultSet.getLong("book_id"), resultSet.getString("book_status"),
                        resultSet.getLong("act_lib_id"), book)
                items.add(item)
            }
            return items
        }

        override fun addItem(item: Item): Long {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }


    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.GET], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/getItem"], method = [RequestMethod.GET],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun getItemById(@RequestParam("id") id: Long): ResponseEntity<String> {
        val item = itemsInterface.getItemById(id)
        val itemObject = gson.toJsonTree(item, Item::class.java)
        val response = JsonObject()
        if (item != null) {
            response.addProperty("status", "ok")
            response.add("item", itemObject)
        } else {
            response.addProperty("status", "error")
            response.addProperty("description", "no item")
        }
        return ResponseEntity.ok(response.toString())
    }

    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.GET], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/library/{libId}/getItems"], method = [RequestMethod.GET],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun getItemsByLibrary(@PathVariable("libId") libId: Long): ResponseEntity<String> {
        val items = itemsInterface.getItemByLibrary(libId)
        val itemsArray = gson.toJsonTree(items)
        val response = JsonObject()
        response.addProperty("status", "ok")
        response.add("items", itemsArray)
        return ResponseEntity.ok(response.toString())
    }

}