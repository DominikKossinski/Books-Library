package com.example.books.library

import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import java.sql.Connection

class DBConnection {

    companion object {
        val dateFormat = "yyyy-MM-dd"
        var dbConnection: Connection? = null
        val inMemoryUserDetailsManager = InMemoryUserDetailsManager()

        fun connect() {
            val dataSource = DriverManagerDataSource()
            dataSource.setDriverClassName("com.mysql.jdbc.Driver")
            dataSource.url = System.getenv("dbUrl")
            dataSource.username = System.getenv("dbUserName")
            dataSource.password = System.getenv("dbPassword")
            dbConnection = dataSource.connection
            dbConnection!!.autoCommit = false
        }


    }
}