package com.example.relaychat.data.repository

import com.example.relaychat.data.model.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun createUserProfile(
        uid: String,
        displayName: String,
        email: String
    ): Result<Unit> {
        return runCatching {
            val userData = hashMapOf(
                "uid" to uid,
                "displayName" to displayName.trim(),
                "email" to email.trim(),
                "createdAt" to FieldValue.serverTimestamp()
            )

            firestore
                .collection("users")
                .document(uid)
                .set(userData)
                .await()
        }
    }

    suspend fun ensureUserProfile(
        uid: String,
        email: String
    ): Result<Unit> {
        return runCatching {
            val documentReference = firestore
                .collection("users")
                .document(uid)

            val document = documentReference
                .get()
                .await()

            if (!document.exists()) {
                val fallbackDisplayName =
                    email.substringBefore("@").ifBlank {
                        "RelayChat User"
                    }

                val userData = hashMapOf(
                    "uid" to uid,
                    "displayName" to fallbackDisplayName,
                    "email" to email.trim(),
                    "createdAt" to FieldValue.serverTimestamp()
                )

                documentReference
                    .set(userData)
                    .await()
            }
        }
    }

    suspend fun getUsers(): Result<List<User>> {
        return runCatching {
            firestore
                .collection("users")
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(User::class.java)
                }
        }
    }
}