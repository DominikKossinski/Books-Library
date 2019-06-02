package com.example.books.library.interfaces

import com.example.books.library.models.User

interface UsersInterface {

    fun getUserByName(name: String): User?

    fun createUser(user: User): Long

    fun checkFreeName(name: String): Boolean

    fun checkFreeEmail(email: String): Boolean

    fun confirmAccount(user: User): Boolean

    fun checkConfirmed(user: User): Boolean

    fun getUserPassword(id: Long): String


}