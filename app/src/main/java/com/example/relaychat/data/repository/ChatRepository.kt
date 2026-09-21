package com.example.relaychat.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.relaychat.data.model.Message
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.example.relaychat.data.model.Conversation


class ChatRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val conversations = firestore.collection("conversations")

    // Same two users always produce the same ID, regardless of who taps whom.
    fun conversationIdFor(uidA: String, uidB: String): String =
        listOf(uidA, uidB).sorted().joinToString("_")

    // Returns the conversation ID, creating the document first if it doesn't exist yet.
    suspend fun openOrCreateConversation(myUid: String, otherUid: String): String {
        val conversationId = conversationIdFor(myUid, otherUid)
        val ref = conversations.document(conversationId)

        val snapshot = ref.get().await()
        if (!snapshot.exists()) {
            ref.set(
                mapOf(
                    "id" to conversationId,
                    "memberIds" to listOf(myUid, otherUid).sorted(),
                    "lastMessage" to "",
                    "lastSenderId" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        return conversationId
    }

    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        receiverId: String,
        text: String
    ) {
        val trimmed = text.trim()
        require(trimmed.isNotEmpty()) { "Message cannot be empty" }

        val conversationRef = conversations.document(conversationId)
        val messageRef = conversationRef.collection("messages").document()

        val batch = firestore.batch()

        batch.set(
            messageRef,
            mapOf(
                "id" to messageRef.id,
                "conversationId" to conversationId,
                "senderId" to senderId,
                "receiverId" to receiverId,
                "text" to trimmed,
                "createdAt" to FieldValue.serverTimestamp(),
                "type" to "text"
            )
        )

        batch.update(
            conversationRef,
            mapOf(
                "lastMessage" to trimmed,
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "lastSenderId" to senderId,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        batch.commit().await()
    }

    fun observeMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val registration = conversations
            .document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(
                            Message::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }
                    trySend(messages)
                }
            }

        awaitClose { registration.remove() }
    }

    fun observeConversations(myUid: String): Flow<List<Conversation>> = callbackFlow {
        val registration = conversations
            .whereArrayContains("memberIds", myUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents
                        .mapNotNull { doc ->
                            doc.toObject(
                                Conversation::class.java,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            )
                        }
                        .filter { it.lastMessage.isNotEmpty() }
                        .sortedByDescending { it.lastMessageAt?.toDate()?.time ?: 0L }
                    trySend(list)
                }
            }

        awaitClose { registration.remove() }
    }
}