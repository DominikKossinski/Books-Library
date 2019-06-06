package com.example.books.library.interfaces

import com.example.books.library.models.Lending

interface LendingsInterface {

    fun getItemStatus(itemId: Long): String

    fun getItemHistory(itemId: Long): ArrayList<Lending>

    fun lendItem(lending: Lending, status: String, actLibId: Long): Long

    fun endLending(lending: Lending): Long

}