package com.example.books.library.interfaces

import com.example.books.library.models.OutsideBook

interface OutsideBooksInterface {

    fun addBookFromOudside(book: OutsideBook): Long

    fun getBooksFromOutside(libId: Long): ArrayList<OutsideBook>
}