package com.example.books.library.interfaces

import com.example.books.library.models.User

interface LibrariesInterface {

    fun getLibraryByUserId(id: Long): Long
    fun getUsersLibrariesByUserId(userId: Long): ArrayList<User>
}