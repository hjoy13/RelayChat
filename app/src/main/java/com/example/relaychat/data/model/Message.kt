package com.example.relaychat.data.model

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null,
    val type: String = "text"
)