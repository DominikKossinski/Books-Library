package com.example.books.library

import org.slf4j.LoggerFactory

class NoConfirmationThread(private val userId: Long) : Thread() {

    companion object {
        val logger = LoggerFactory.getLogger(NoConfirmationThread::class.java)!!
        //TODO zmiana czasu
        const val waitTime = 100000.toLong()
    }

    override fun run() {
        logger.info("Waiting for confirmation userId: $userId")
        sleep(waitTime)
        logger.info("End of waiting for confirmation: $userId")
        val sqlString = "DELETE FROM USERS WHERE USER_ID = ? AND STATUS = 0;"
        val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
        prepStmt.setLong(1, userId)
        val count = prepStmt.executeUpdate()
        if (count == 0) {
            logger.info("User $userId confirmed")
        } else {
            logger.warn("USer $userId deleted (count = $count)")
        }
    }
}