package com.example.books.library.interfaces

import com.example.books.library.models.Loan

interface LoansInterface {

    fun getItemStatus(itemId: Long): String

    fun getItemHistory(itemId: Long): ArrayList<Loan>

    fun lendItem(loan: Loan, status: String, actLibId: Long): Long

    fun endLending(loan: Loan): Long

}