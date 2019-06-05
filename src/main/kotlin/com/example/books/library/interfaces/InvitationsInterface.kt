package com.example.books.library.interfaces

import com.example.books.library.models.Invitation

interface InvitationsInterface {

    fun addInvitation(invitation: Invitation): Long

    fun getStatus(userId: Long, email: String): String
    fun getInvitationById(invitationId: Long): Invitation?
    fun updateStatus(invitationId: Long, status: String)
}