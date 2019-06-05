package com.example.books.library

import com.example.books.library.models.Invitation
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

    fun sendInvitationMessage(invitation: Invitation, name: String) {
        val message = SimpleMailMessage()
        message.setTo(invitation.email)
        message.setFrom(System.getenv("mailUserName"))
        message.setSubject("Invitation from $name")
        if (invitation.status.contentEquals("user exists")) {
            message.setText("Hello \n" +
                    "User $name send you invitation to library. Click the link below to accept invitation\n" +
                    "http://localhost:3000/invitation/${invitation.invitationId}")
        } else {
            message.setText("Hello \n" +
                    "User $name send you invitation to library.\n" +
                    "You need to create account with this email ${invitation.email} on our service:\n" +
                    "http://localhost:3000/login\n" +
                    "Then click the link below to accept invitation\n" +
                    "http://localhost:3000/invitation/${invitation.invitationId} \n")
        }
        javaMailSender!!.send(message)
    }
}