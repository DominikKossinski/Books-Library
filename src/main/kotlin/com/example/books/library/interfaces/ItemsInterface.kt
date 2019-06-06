package com.example.books.library.interfaces

import com.example.books.library.models.Item

interface ItemsInterface {

    fun getItemById(id: Long): Item?

    fun getItemByLibrary(libId: Long): ArrayList<Item>

    fun addItem(item: Item): Long

    fun getItemsByBookId(bookId: Long): ArrayList<Item>

    fun existsItem(item: Item): Boolean
}