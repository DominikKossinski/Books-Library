package com.example.books.library.models

data class Item(var itemId: Long, var libraryId: Long, var bookId: Long, var bookStatus: String, var actLibId: Long?,
                var book: Book?)