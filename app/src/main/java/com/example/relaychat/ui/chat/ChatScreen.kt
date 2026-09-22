package com.example.relaychat.ui.chat

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.relaychat.data.model.Message
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import com.example.relaychat.ui.conversations.AvatarCircle

@Composable
fun ChatScreen(
    myUid: String,
    otherUid: String,
    otherName: String,
    onBack: () -> Unit
) {
    val viewModel: ChatViewModel = viewModel(
        key = "chat_${myUid}_$otherUid",
        factory = ChatViewModelFactory(myUid, otherUid)
    )
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Scroll to the newest message whenever the list grows.
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(4.dp))
            AvatarCircle(name = otherName, size = 32.dp)
            Spacer(Modifier.width(8.dp))
            Text(text = otherName, style = MaterialTheme.typography.titleMedium)
        }
        HorizontalDivider()

        // Message area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.messages.isEmpty() -> Text(
                    text = "No messages yet. Say hello!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        val isMine = message.senderId == myUid
                        val isLastMine = isMine && message.id == state.messages.lastOrNull { it.senderId == myUid }?.id
                        val isSeen = isLastMine &&
                                (message.createdAt?.toDate()?.time ?: 0L) <= state.otherLastReadAt

                        MessageBubble(
                            message = message,
                            isMine = isMine,
                            onReply = { viewModel.setReplyTarget(message) },
                            showSeen = isSeen
                        )
                    }
                }
            }
        }

        // Typing indicator
        if (state.isOtherTyping) {
            Text(
                text = "$otherName is typing…",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )
        }

        // Error banner
        state.errorMessage?.let { error ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = viewModel::clearError) { Text("Dismiss") }
            }
        }

        state.replyingTo?.let { replyTarget ->
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (replyTarget.senderId == myUid) "Replying to yourself" else "Replying to $otherName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = replyTarget.text,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = viewModel::clearReplyTarget) {
                    Text("✕")
                }
            }
        }

        // Input row
        Surface(
            tonalElevation = 2.dp,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.inputText,
                    onValueChange = viewModel::onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message") },
                    maxLines = 4
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = viewModel::sendMessage,
                    enabled = state.inputText.isNotBlank() && !state.isSending
                ) {
                    if (state.isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Send")
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    isMine: Boolean,
    onReply: () -> Unit,
    showSeen: Boolean
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val time = message.createdAt?.toDate()?.let { timeFormat.format(it) } ?: ""

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isMine) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = onReply
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (message.replyToMessageId != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = message.replyToText.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Text(text = message.text)
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.End)
                )
                if (showSeen) {
                    Text(
                        text = "Seen",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}