package com.example.relaychat.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    suspend fun register(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return runCatching {
            val result = auth
                .createUserWithEmailAndPassword(email.trim(), password)
                .await()

            result.user
                ?: throw IllegalStateException("Registration succeeded but user was unavailable.")
        }
    }

    suspend fun login(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return runCatching {
            val result = auth
                .signInWithEmailAndPassword(email.trim(), password)
                .await()

            result.user
                ?: throw IllegalStateException("Login succeeded but user was unavailable.")
        }
    }

    fun logout() {
        auth.signOut()
    }
}