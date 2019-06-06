package com.example.books.library.models

import java.util.*

data class Lending(var lengindId: Long, var userId: Long, var itemId: Long, var startDate: Date, var endDate: Date?)