package com.example.relaychat.data.model

import com.google.firebase.Timestamp

data class Conversation(
    val id: String = "",
    val memberIds: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageAt: Timestamp? = null,
    val lastSenderId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val lastReadAt: Map<String, Timestamp>? = null
)