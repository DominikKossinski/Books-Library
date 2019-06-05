package com.example.books.library.interfaces

import com.example.books.library.models.Member

interface MembersInterface {

    fun getMembersByLibId(libId: Long): ArrayList<Member>

    fun addMember(member: Member): Long

}