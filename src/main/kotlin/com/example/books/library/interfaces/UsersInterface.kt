package com.example.books.library.interfaces

import com.example.books.library.models.Item
import com.example.books.library.models.User

interface UsersInterface {

    fun getUserByName(name: String): User?

    fun createUser(user: User): Long

    fun checkFreeName(name: String): Boolean

    fun checkFreeEmail(email: String): Boolean

    fun confirmAccount(user: User): Boolean

    fun checkConfirmed(user: User): Boolean

    fun getUserPassword(id: Long): String

    fun getUserById(id: Long): User?

    fun getUserByEmail(email: String): User?

    fun getAvg(id: Long): Float

    fun getLastBook(id: Long): Item?

    fun getReadingBooks(id: Long): ArrayList<Item>

    fun getAvgBooks(id: Long): Float
    fun getUserByLibId(libId: Long): User?


}