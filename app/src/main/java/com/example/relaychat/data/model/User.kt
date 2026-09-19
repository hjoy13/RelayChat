package com.example.relaychat.data.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val createdAt: Timestamp? = null
)