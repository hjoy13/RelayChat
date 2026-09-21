package com.example.relaychat

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.relaychat.navigation.AppNavigation
import com.example.relaychat.ui.auth.AuthViewModel
import com.example.relaychat.ui.theme.RelayChatTheme

class MainActivity : ComponentActivity() {

    private val pendingChat = mutableStateOf<Pair<String, String>?>(null)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()
        requestNotificationPermissionIfNeeded()
        handleNotificationIntent(intent)

        setContent {
            RelayChatTheme {
                val authViewModel: AuthViewModel = viewModel()

                AppNavigation(
                    authViewModel = authViewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val uid = intent?.getStringExtra("senderId")
        val name = intent?.getStringExtra("senderName")
        if (uid != null && name != null) {
            pendingChat.value = uid to name
            Log.d("RelayChat", "Notification tap -> chat with $uid ($name)")
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "messages",
            "Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "New message notifications"
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}