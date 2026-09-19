package com.example.relaychat.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.relaychat.ui.auth.AuthViewModel
import com.example.relaychat.ui.auth.LoginScreen
import com.example.relaychat.ui.auth.RegisterScreen

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel
) {
    val navController = rememberNavController()

    val uiState by authViewModel.uiState.collectAsState()

    val startDestination = if (uiState.isLoggedIn) {
        "home"
    } else {
        "login"
    }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            navController.navigate("home") {
                popUpTo("login") {
                    inclusive = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        composable("login") {
            LoginScreen(
                uiState = uiState,
                onLogin = { email, password ->
                    authViewModel.login(email, password)
                },
                onGoToRegister = {
                    authViewModel.clearError()
                    navController.navigate("register")
                }
            )
        }

        composable("register") {
            RegisterScreen(
                uiState = uiState,
                onRegister = { email, password ->
                    authViewModel.register(email, password)
                },
                onGoToLogin = {
                    authViewModel.clearError()
                    navController.popBackStack()
                }
            )
        }

        composable("home") {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Logged in to RelayChat")

                Button(
                    onClick = {
                        authViewModel.logout()

                        navController.navigate("login") {
                            popUpTo("home") {
                                inclusive = true
                            }
                        }
                    }
                ) {
                    Text("Logout")
                }
            }
        }
    }
}