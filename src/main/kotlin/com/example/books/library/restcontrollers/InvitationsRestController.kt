package com.example.books.library.restcontrollers

import com.example.books.library.DBConnection
import com.example.books.library.NotificationService
import com.example.books.library.interfaces.InvitationsInterface
import com.example.books.library.models.Invitation
import com.example.books.library.models.Member
import com.google.gson.JsonObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.mail.MailException
import org.springframework.web.bind.annotation.*
import java.sql.Statement

@RestController
class InvitationsRestController {

    @Autowired
    private var notificationService: NotificationService? = null

    companion object {
        val logger = LoggerFactory.getLogger(InvitationsRestController::class.java)
        val gson = DBConnection.gson
    }

    val invitationsInterface = object : InvitationsInterface {
        override fun updateStatus(invitationId: Long, status: String) {
            DBConnection.dbConnection!!.beginRequest()
            val sqlString = "UPDATE invitations SET STATUS = ? WHERE INVITATION_ID = ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setString(1, status)
            prepStmt.setLong(2, invitationId)
            prepStmt.executeUpdate()
            DBConnection.dbConnection!!.commit()
        }

        override fun getInvitationById(invitationId: Long): Invitation? {
            DBConnection.dbConnection!!.beginRequest()
            val sqlString = "SELECT INVITATION_ID, USER_ID, EMAIL, STATUS FROM invitations where INVITATION_ID = ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, invitationId)
            val resultSet = prepStmt.executeQuery()
            var count = 0
            while (resultSet.next()) {
                count++
            }
            return if (count == 1) {
                DBConnection.dbConnection!!.commit()
                resultSet.first()
                Invitation(resultSet.getLong("invitation_id"), resultSet.getLong("user_id"),
                        resultSet.getString("email"), resultSet.getString("status"))
            } else {
                DBConnection.dbConnection!!.rollback()
                null
            }
        }

        override fun getStatus(userId: Long, email: String): String {
            DBConnection.dbConnection!!.beginRequest()
            val inviteString = "SELECT USER_ID FROM INVITATIONS WHERE EMAIL = ? AND USER_ID = ?"
            val stmt = DBConnection.dbConnection!!.prepareStatement(inviteString)
            stmt.setString(1, email)
            stmt.setLong(2, userId)
            val set = stmt.executeQuery()
            var count = 0
            while (set.next()) {
                count++
            }
            if (count > 0) {
                DBConnection.dbConnection!!.rollback()
                return "already invited"
            }
            val sqlString = "SELECT USER_ID FROM USERS WHERE EMAIL = ?"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setString(1, email)
            count = 0
            val resultSet = prepStmt.executeQuery()
            while (resultSet.next()) {
                count++
            }
            return if (count == 1) {
                DBConnection.dbConnection!!.commit()
                "user exists"
            } else {
                DBConnection.dbConnection!!.rollback()
                "no user"
            }
        }

        override fun addInvitation(invitation: Invitation): Long {
            DBConnection.dbConnection!!.beginRequest()
            val sqlString = "INSERT INTO invitations(USER_ID, EMAIL, STATUS) VALUES (?, ?, ?)"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString, Statement.RETURN_GENERATED_KEYS)
            prepStmt.setLong(1, invitation.userId)
            prepStmt.setString(2, invitation.email)
            prepStmt.setString(3, invitation.status)
            val count = prepStmt.executeUpdate()
            return if (count == 1) {
                val set = prepStmt.generatedKeys
                set.first()
                DBConnection.dbConnection!!.commit()
                set.getLong(1)
            } else {
                DBConnection.dbConnection!!.rollback()
                -1
            }
        }


    }


    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.POST], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/{id}/invite"], method = [RequestMethod.POST],
            consumes = [MediaType.APPLICATION_JSON_UTF8_VALUE],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun addInvitation(@PathVariable("id") id: Long, @RequestBody invitationData: String): ResponseEntity<String> {
        val invitation = gson.fromJson(invitationData, Invitation::class.java)
        val response = JsonObject()
        val user = UsersRestController().usersInterface.getUserById(invitation.userId)
        if (user == null) {
            response.addProperty("status", "error")
            response.addProperty("description", "no user")
            return ResponseEntity.ok(response.toString())
        }
        if (user.email.contentEquals(invitation.email)) {
            response.addProperty("status", "error")
            response.addProperty("description", "self")
            return ResponseEntity.ok(response.toString())
        }
        if (UsersRestController.emailReqex.matches(invitation.email)) {
            val status = invitationsInterface.getStatus(invitation.userId, invitation.email)
            invitation.status = status
            if (status.contentEquals("already invited") || status.contentEquals("confirmed")) {
                response.addProperty("status", "error")
                response.addProperty("description", "already invited")
                return ResponseEntity.ok(response.toString())
            }
            val result = invitationsInterface.addInvitation(invitation)
            if (result > 0) {
                try {
                    invitation.invitationId = result
                    notificationService!!.sendInvitationMessage(invitation, user.name)
                } catch (e: MailException) {
                    response.addProperty("status", "error")
                    response.addProperty("description", "error by sending email")
                    return ResponseEntity.ok(response.toString())
                }
                response.addProperty("status", "ok")
            } else {
                response.addProperty("status", "error")
                response.addProperty("description", "error by adding")
            }
        } else {
            response.addProperty("status", "error")
            response.addProperty("description", "not an email")
        }
        return ResponseEntity.ok(response.toString())
    }

    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.POST], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/{id}/checkEmail"], method = [RequestMethod.POST],
            consumes = [MediaType.APPLICATION_JSON_UTF8_VALUE],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun checkEmail(@RequestBody email: String, @PathVariable("id") id: Long): ResponseEntity<String> {
        val body = gson.fromJson(email, JsonObject::class.java)
        val response = JsonObject()
        response.addProperty("status", "ok")
        response.addProperty("email", UsersRestController.emailReqex.matches(body.get("email").asString))
        return ResponseEntity.ok(response.toString())
    }


    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.GET], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/invitation/{invitationId}"], method = [RequestMethod.GET],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun confirmInvitation(@PathVariable("invitationId") invitationId: Long): ResponseEntity<String> {
        val invitation = invitationsInterface.getInvitationById(invitationId)
        val response = JsonObject()
        if (invitation != null) {
            if (invitation.status.contentEquals("no user")) {
                val status = UsersRestController().usersInterface.checkFreeEmail(invitation.email)
                if (status) {
                    response.addProperty("status", "error")
                    response.addProperty("description", "no user")
                    return ResponseEntity.ok(response.toString())
                }
            } else if (invitation.status.contentEquals("confirmed")) {
                response.addProperty("status", "error")
                response.addProperty("description", "confirmed")
                return ResponseEntity.ok(response.toString())
            }
            val user = UsersRestController().usersInterface.getUserByEmail(invitation.email)
            val lib = LibrariesRestController().librariesInterface.getLibraryByUserId(invitation.userId)
            val member = Member(lib, user!!)
            val status = MembersRestController().membersInterface.addMember(member)
            if (status == 1.toLong()) {
                invitationsInterface.updateStatus(invitation.invitationId, "confirmed")
                response.addProperty("status", "ok")
            }

        } else {
            response.addProperty("status", "error")
            response.addProperty("description", "no invitation")
        }
        return ResponseEntity.ok(response.toString())
    }


}