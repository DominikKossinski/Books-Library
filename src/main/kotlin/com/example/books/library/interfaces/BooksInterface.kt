package com.example.books.library.interfaces

import com.example.books.library.models.Book

interface BooksInterface {

    fun getBooksByPattern(pattern: String): ArrayList<Book>

    fun getBookById(id: Long): Book?

    fun getBookByISBN(isbn: String): Book?

    fun addBook(book: Book): Long
}