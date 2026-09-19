package com.example.relaychat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.relaychat.navigation.AppNavigation
import com.example.relaychat.ui.auth.AuthViewModel
import com.example.relaychat.ui.theme.RelayChatTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RelayChatTheme {
                val authViewModel: AuthViewModel = viewModel()

                AppNavigation(
                    authViewModel = authViewModel
                )
            }
        }
    }
}