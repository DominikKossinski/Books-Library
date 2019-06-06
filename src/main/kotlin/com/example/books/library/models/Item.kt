package com.example.books.library.models

import java.util.*

data class Item(var itemId: Long, var libraryId: Long, var bookId: Long, var bookStatus: String, var actLibId: Long?,
                var comment: String, var startDate: Date?, var endDate: Date?,
                var book: Book?, var loans: ArrayList<Loan>? = null)