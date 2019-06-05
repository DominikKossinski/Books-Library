package com.example.books.library.models


data class User(var id: Long, var name: String, var email: String, var password: String, var status: Int, var libId: Long = -1)