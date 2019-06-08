package com.example.books.library.restcontrollers

import com.example.books.library.DBConnection
import com.example.books.library.interfaces.ItemsInterface
import com.example.books.library.models.Book
import com.example.books.library.models.Item
import com.example.books.library.models.User
import com.google.gson.JsonObject
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.sql.Statement

@RestController
class ItemsRestController {

    companion object {
        val logger = LoggerFactory.getLogger(ItemsRestController::class.java)
        val gson = DBConnection.gson
    }

    var itemsInterface = object : ItemsInterface {
        override fun searchItemsByPattern(libId: Long, pattern: String): ArrayList<Item> {
            DBConnection.dbConnection!!.beginRequest()
            val sqlString = "SELECT ITEM_ID, LIBRARY_ID, COMMENT, START_DATE, END_DATE, I.BOOK_ID, BOOK_STATUS, ACT_LIB_ID, TITLE, ISBN, AUTHOR, PAGES_COUNT FROM ITEMS I JOIN BOOKS B ON I.BOOK_ID = B.BOOK_ID WHERE ((LIBRARY_ID = ? AND ACT_LIB_ID IS NULL) OR ACT_LIB_ID = ?) AND (TITLE LIKE ? OR AUTHOR LIKE ?)"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, libId)
            prepStmt.setLong(2, libId)
            prepStmt.setString(3, "%$pattern%")
            prepStmt.setString(4, "%$pattern%")
            val resultSet = prepStmt.executeQuery()
            val items = ArrayList<Item>()
            while (resultSet.next()) {
                val book = Book(resultSet.getLong("book_id"), resultSet.getString("title"),
                        resultSet.getString("author"), resultSet.getString("isbn"), resultSet.getInt("pages_count"))
                val item = Item(resultSet.getLong("item_id"), resultSet.getLong("library_id"),
                        resultSet.getLong("book_id"), resultSet.getString("book_status"),
                        resultSet.getLong("act_lib_id"), resultSet.getString("comment"),
                        resultSet.getDate("start_date"), resultSet.getDate("end_date"), book)
                items.add(item)
            }
            DBConnection.dbConnection!!.commit()
            return items
        }

        override fun getBorrowedItems(libId: Long): ArrayList<Item> {
            DBConnection.dbConnection!!.beginRequest()
            val sqlString = "SELECT ITEM_ID, LIBRARY_ID, COMMENT, START_DATE, END_DATE, I.BOOK_ID, BOOK_STATUS, ACT_LIB_ID, TITLE, ISBN, AUTHOR, PAGES_COUNT FROM ITEMS I JOIN BOOKS B ON I.BOOK_ID = B.BOOK_ID WHERE LIBRARY_ID = ? and ACT_LIB_ID IS NOT NULL"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, libId)
            val resultSet = prepStmt.executeQuery()
            val items = ArrayList<Item>()
            while (resultSet.next()) {
                val book = Book(resultSet.getLong("book_id"), resultSet.getString("title"),
                        resultSet.getString("author"), resultSet.getString("isbn"), resultSet.getInt("pages_count"))
                val item = Item(resultSet.getLong("item_id"), resultSet.getLong("library_id"),
                        resultSet.getLong("book_id"), resultSet.getString("book_status"),
                        resultSet.getLong("act_lib_id"), resultSet.getString("comment"),
                        resultSet.getDate("start_date"), resultSet.getDate("end_date"), book)
                items.add(item)
            }
            DBConnection.dbConnection!!.commit()
            return items
        }

        override fun endReading(item: Item): Boolean {
            val sqlString = "UPDATE ITEMS SET END_DATE = CURDATE() WHERE ITEM_ID = ?"
            DBConnection.dbConnection!!.beginRequest()
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, item.itemId)
            val count = prepStmt.executeUpdate()
            return if (count == 1) {
                DBConnection.dbConnection!!.commit()
                true
            } else {
                DBConnection.dbConnection!!.rollback()
                false
            }
        }

        override fun startReading(item: Item): Boolean {
            val sqlString = "UPDATE ITEMS SET START_DATE = CURDATE(), END_DATE = NULL WHERE ITEM_ID = ?"
            DBConnection.dbConnection!!.beginRequest()
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, item.itemId)
            val count = prepStmt.executeUpdate()
            return if (count == 1) {
                DBConnection.dbConnection!!.commit()
                true
            } else {
                DBConnection.dbConnection!!.rollback()
                false
            }
        }

        override fun addComment(item: Item): Boolean {
            val sqlString = "UPDATE ITEMS SET COMMENT = ? WHERE ITEM_ID = ?"
            DBConnection.dbConnection!!.beginRequest()
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setString(1, item.comment)
            prepStmt.setLong(2, item.itemId)
            val count = prepStmt.executeUpdate()
            return if (count == 1) {
                DBConnection.dbConnection!!.commit()
                true
            } else {
                DBConnection.dbConnection!!.rollback()
                false
            }
        }

        override fun existsItem(item: Item): Boolean {
            val sqlString = "SELECT ITEM_ID FROM items WHERE BOOK_ID = ? AND LIBRARY_ID = ?"
            DBConnection.dbConnection!!.beginRequest()
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, item.bookId)
            prepStmt.setLong(2, item.libraryId)
            val resultSet = prepStmt.executeQuery()
            var count = 0
            while (resultSet.next()) {
                count++
            }
            DBConnection.dbConnection!!.commit()
            logger.info("existsItem($item) count = $count")
            return count > 0
        }

        override fun getItemsByBookId(bookId: Long): ArrayList<Item> {
            val sqlString = "SELECT ITEM_ID, LIBRARY_ID, COMMENT, START_DATE, END_DATE, I.BOOK_ID, BOOK_STATUS, ACT_LIB_ID FROM items i JOIN books b on i.BOOK_ID = b.BOOK_ID WHERE b.BOOK_ID = ?"
            DBConnection.dbConnection!!.beginRequest()
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, bookId)
            val items = ArrayList<Item>()
            val resultSet = prepStmt.executeQuery()
            while (resultSet.next()) {
                val item = Item(resultSet.getLong("item_id"), resultSet.getLong("library_id"),
                        resultSet.getLong("book_id"), resultSet.getString("book_status"),
                        resultSet.getLong("act_lib_id"), resultSet.getString("comment"),
                        resultSet.getDate("start_date"), resultSet.getDate("end_date"), null)
                items.add(item)
            }
            DBConnection.dbConnection!!.commit()
            return items
        }

        override fun getItemById(id: Long): Item? {
            DBConnection.dbConnection!!.beginRequest()
            val sqlString = "SELECT ITEM_ID, LIBRARY_ID, COMMENT, START_DATE, END_DATE, I.BOOK_ID, BOOK_STATUS, ACT_LIB_ID, TITLE, ISBN, AUTHOR, PAGES_COUNT FROM ITEMS I JOIN BOOKS B ON I.BOOK_ID = B.BOOK_ID WHERE ITEM_ID = ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, id)
            val resultSet = prepStmt.executeQuery()
            resultSet.next()
            return if (resultSet.isFirst) {
                resultSet.first()
                DBConnection.dbConnection!!.commit()
                val book = Book(resultSet.getLong("book_id"), resultSet.getString("title"),
                        resultSet.getString("author"), resultSet.getString("isbn"), resultSet.getInt("pages_count"))
                Item(resultSet.getLong("item_id"), resultSet.getLong("library_id"),
                        resultSet.getLong("book_id"), resultSet.getString("book_status"),
                        resultSet.getLong("act_lib_id"), resultSet.getString("comment"),
                        resultSet.getDate("start_date"), resultSet.getDate("end_date"), book)
            } else {
                DBConnection.dbConnection!!.commit()
                null
            }
        }

        override fun getItemByLibrary(libId: Long): ArrayList<Item> {
            DBConnection.dbConnection!!.beginRequest()
            val sqlString = "SELECT ITEM_ID, LIBRARY_ID, COMMENT, START_DATE, END_DATE, I.BOOK_ID, BOOK_STATUS, ACT_LIB_ID, TITLE, ISBN, AUTHOR, PAGES_COUNT FROM ITEMS I JOIN BOOKS B ON I.BOOK_ID = B.BOOK_ID WHERE (LIBRARY_ID = ? and ACT_LIB_ID IS NULL) OR ACT_LIB_ID = ?"
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
                        resultSet.getLong("act_lib_id"), resultSet.getString("comment"),
                        resultSet.getDate("start_date"), resultSet.getDate("end_date"), book)
                items.add(item)
            }
            DBConnection.dbConnection!!.commit()
            return items
        }

        override fun addItem(item: Item): Long {
            val sqlString = "INSERT INTO items(LIBRARY_ID, BOOK_ID, BOOK_STATUS, ACT_LIB_ID) VALUES (?, ?, '', NULL)"
            DBConnection.dbConnection!!.beginRequest()
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString, Statement.RETURN_GENERATED_KEYS)
            prepStmt.setLong(1, item.libraryId)
            prepStmt.setLong(2, item.bookId)
            val count = prepStmt.executeUpdate()
            return if (count == 1) {
                val keys = prepStmt.generatedKeys
                keys.first()
                DBConnection.dbConnection!!.commit()
                keys.getLong(1)
            } else {
                DBConnection.dbConnection!!.rollback()
                -10
            }

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
        val user = UsersRestController().usersInterface.getUserByLibId(libId)
        val userObject = gson.toJsonTree(user, User::class.java)
        for (item in items) {
            val loans = LoansRestController().loansInterface.getItemHistory(item.itemId)
            item.loans = loans
        }
        val itemsArray = gson.toJsonTree(items)
        val response = JsonObject()
        response.addProperty("status", "ok")
        response.add("items", itemsArray)
        response.add("owner", userObject)
        return ResponseEntity.ok(response.toString())
    }

    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.GET], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/library/{libId}/getBorrowedItems"], method = [RequestMethod.GET],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun getBorrowed(@PathVariable("libId") libId: Long): ResponseEntity<String> {
        val items = itemsInterface.getBorrowedItems(libId)
        val itemsArray = gson.toJsonTree(items)
        val response = JsonObject()
        response.addProperty("status", "ok")
        response.add("items", itemsArray)
        return ResponseEntity.ok(response.toString())
    }

    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.GET], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/getItemsByBookId"], method = [RequestMethod.GET],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun getItemsByBookId(@RequestParam("bookId") bookId: Long): ResponseEntity<String> {
        val book = BooksRestController().booksInterface.getBookById(bookId)
        val response = JsonObject()
        if (book == null) {
            response.addProperty("status", "error")
            response.addProperty("descriptin", "no book")
        }
        val items = itemsInterface.getItemsByBookId(bookId)
        val itemsArray = gson.toJsonTree(items)
        val bookObject = gson.toJsonTree(book, Book::class.java)
        response.addProperty("status", "ok")
        response.add("book", bookObject)
        response.add("items", itemsArray)
        return ResponseEntity.ok(response.toString())
    }

    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.POST], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/library/{libId}/addItem"], method = [RequestMethod.POST],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun addItem(@RequestBody itemData: String, @PathVariable("libId") libId: Long): ResponseEntity<String> {
        val requestObject = gson.fromJson(itemData, JsonObject::class.java)
        val book = BooksRestController().booksInterface.getBookByISBN(requestObject.get("isbn").asString)
        val response = JsonObject()
        if (book == null) {
            response.addProperty("status", "error")
            response.addProperty("description", "no book")
            return ResponseEntity.ok(response.toString())
        }
        val item = Item(0, libId, book.bookId, "", null, "", null, null, book)
        val exists = itemsInterface.existsItem(item)
        if (exists) {
            response.addProperty("status", "error")
            response.addProperty("description", "item exists")
        } else {
            val status = itemsInterface.addItem(item)
            if (status > 0) {
                response.addProperty("status", "ok")
            } else {
                response.addProperty("status", "error")
                response.addProperty("description", "error by adding item")
            }
        }
        return ResponseEntity.ok(response.toString())
    }

    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.GET], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/library/{libId}/search"], method = [RequestMethod.GET],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun searchItemsByLibrary(@PathVariable("libId") libId: Long, @RequestParam("pattern") pattern: String): ResponseEntity<String> {
        val items = itemsInterface.searchItemsByPattern(libId, pattern)
        val response = JsonObject()
        for (item in items) {
            item.loans = LoansRestController().loansInterface.getItemHistory(item.itemId)
        }
        val itemsArray = gson.toJsonTree(items).asJsonArray
        response.addProperty("status", "ok")
        response.add("items", itemsArray)
        return ResponseEntity.ok(response.toString())
    }

    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.POST], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/{userId}/addComment"], method = [RequestMethod.POST],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun addComment(@PathVariable("userId") userId: Long, @RequestBody itemData: String): ResponseEntity<String> {
        val item = gson.fromJson(itemData, Item::class.java)
        val foundItem = itemsInterface.getItemById(item.itemId)
        val libId = LibrariesRestController().librariesInterface.getLibraryByUserId(userId)
        val response = JsonObject()
        if (foundItem == null) {
            response.addProperty("status", "errror")
            response.addProperty("description", "no item")
        } else {
            if (libId == foundItem.libraryId) {
                val status = itemsInterface.addComment(item)
                if (status) {
                    response.addProperty("status", "ok")
                } else {
                    response.addProperty("status", "error")
                    response.addProperty("description", "error by adding comment")
                }
            } else {
                response.addProperty("status", "error")
                response.addProperty("description", "wrong user")
            }
        }
        return ResponseEntity.ok(response.toString())
    }


    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.POST], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/{userId}/reading"], method = [RequestMethod.POST],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun markReading(@PathVariable("userId") userId: Long, @RequestBody itemData: String): ResponseEntity<String> {
        val item = gson.fromJson(itemData, Item::class.java)
        val foundItem = itemsInterface.getItemById(item.itemId)
        val libId = LibrariesRestController().librariesInterface.getLibraryByUserId(userId)
        val response = JsonObject()
        if (foundItem == null) {
            response.addProperty("status", "error")
            response.addProperty("description", "no item")
        } else {
            if (foundItem.libraryId == libId) {
                val status = if (foundItem.endDate == null) {
                    itemsInterface.endReading(foundItem)
                } else {
                    itemsInterface.startReading(foundItem)
                }
                if (status) {
                    response.addProperty("status", "ok")
                } else {
                    response.addProperty("status", "error")
                    response.addProperty("description", "error by reading")
                }
            } else {
                response.addProperty("status", "error")
                response.addProperty("description", "wrong user")
            }
        }
        return ResponseEntity.ok(response.toString())
    }


}