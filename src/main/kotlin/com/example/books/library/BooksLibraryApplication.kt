package com.example.books.library

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class BooksLibraryApplication


fun main(args: Array<String>) {
    DBConnection.connect()
    SpringApplication.run(BooksLibraryApplication::class.java)
    //runApplication<BooksLibraryApplication>(*args)
}

