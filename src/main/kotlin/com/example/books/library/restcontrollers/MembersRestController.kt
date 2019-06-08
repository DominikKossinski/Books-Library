package com.example.books.library.restcontrollers

import com.example.books.library.DBConnection
import com.example.books.library.interfaces.MembersInterface
import com.example.books.library.models.Member
import com.example.books.library.models.User
import com.google.gson.JsonObject
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class MembersRestController {

    companion object {
        val logger = LoggerFactory.getLogger(MembersRestController::class.java)!!
        val gson = DBConnection.gson
    }

    var membersInterface = object : MembersInterface {
        override fun getMembersByLibId(libId: Long): ArrayList<Member> {
            DBConnection.dbConnection!!.beginRequest()
            val sqlString = "SELECT M.USER_ID, LIBRARY_ID, NAME, EMAIL FROM MEMBERS M JOIN USERS U ON M.USER_ID = U.USER_ID WHERE LIBRARY_ID = ? AND STATUS = 1"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, libId)
            val resultSet = prepStmt.executeQuery()
            val members = ArrayList<Member>()
            while (resultSet.next()) {
                val user = User(resultSet.getLong("user_id"), resultSet.getString("name"), resultSet.getString("email"), "", 1)
                val member = Member(resultSet.getLong("library_id"), user)
                members.add(member)
            }
            DBConnection.dbConnection!!.commit()
            return members
        }

        override fun addMember(member: Member): Long {
            DBConnection.dbConnection!!.beginRequest()
            val sqlString = "INSERT INTO MEMBERS(LIBRARY_ID, USER_ID) VALUES (?, ?)"
            val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
            prepStmt.setLong(1, member.libId)
            prepStmt.setLong(2, member.user.id)
            val count = prepStmt.executeUpdate()
            DBConnection.dbConnection!!.commit()
            return count.toLong()
        }
    }

    @CrossOrigin(origins = ["http://localhost:3000"], methods = [RequestMethod.GET], allowCredentials = "true",
            allowedHeaders = ["*"])
    @RequestMapping(value = ["/api/library/{libId}/getMembers"], method = [RequestMethod.GET],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun getMembersByLibraryId(@PathVariable("libId") libId: Long): ResponseEntity<String> {
        val members = membersInterface.getMembersByLibId(libId)
        val membersArray = gson.toJsonTree(members)
        val response = JsonObject()
        response.addProperty("status", "ok")
        response.add("members", membersArray)
        return ResponseEntity.ok(response.toString())
    }

}