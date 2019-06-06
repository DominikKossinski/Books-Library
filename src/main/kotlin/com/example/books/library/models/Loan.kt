package com.example.books.library.models

import java.util.*

data class Loan(var loanId: Long, var userId: Long, var itemId: Long, var startDate: Date, var endDate: Date?)