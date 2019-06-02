package com.example.books.library

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class NotificationService @Autowired constructor(javaMailSender: JavaMailSender) {

    private var javaMailSender: JavaMailSender? = javaMailSender

    fun sendCreateNotification(userName: String, email: String, userId: String) {
        val message = SimpleMailMessage()
        message.setTo(email)
        message.setFrom(System.getenv("mailUserName"))
        message.setSubject("Account Confirmation")
        message.setText("Welcome $userName!\n"
                + "Please click the ling below to confirm account\n" +
                "http://localhost:3000/confirmAccount/$userName/$userId")
        javaMailSender!!.send(message)

    }
}