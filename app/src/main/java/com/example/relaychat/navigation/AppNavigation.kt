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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.relaychat.ui.users.UserListScreen
import com.example.relaychat.ui.users.UsersViewModel
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import android.net.Uri
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.relaychat.ui.chat.ChatScreen
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.example.relaychat.ui.conversations.ConversationListScreen
import com.example.relaychat.ui.conversations.ConversationsViewModel
import com.example.relaychat.ui.conversations.ConversationsViewModelFactory
import android.util.Log
import com.example.relaychat.data.repository.UserRepository


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

            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                UserRepository().saveDeviceToken(uid)
                    .onSuccess { Log.d("RelayChat", "Device token saved") }
                    .onFailure { Log.e("RelayChat", "Device token save failed", it) }
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
                onRegister = { displayName, email, password ->
                    authViewModel.register(
                        displayName = displayName,
                        email = email,
                        password = password
                    )
                },
                onGoToLogin = {
                    authViewModel.clearError()
                    navController.popBackStack()
                }
            )
        }

        composable("home") {
            val myUid = FirebaseAuth.getInstance().currentUser?.uid

            if (myUid != null) {
                val conversationsViewModel: ConversationsViewModel = viewModel(
                    key = "conversations_$myUid",
                    factory = ConversationsViewModelFactory(myUid)
                )
                val conversationsUiState by conversationsViewModel.uiState.collectAsState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = { navController.navigate("users") }) {
                            Text("New chat")
                        }
                        Button(
                            onClick = {
                                authViewModel.logout()
                                navController.navigate("login") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        ) {
                            Text("Logout")
                        }
                    }

                    ConversationListScreen(
                        uiState = conversationsUiState,
                        myUid = myUid,
                        onConversationClick = { item ->
                            navController.navigate(
                                "chat/${item.otherUid}/${Uri.encode(item.otherName)}"
                            )
                        }
                    )
                }
            }
        }

        composable("users") {
            val usersViewModel: UsersViewModel = viewModel()
            val usersUiState by usersViewModel.uiState.collectAsState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text("Back")
                }

                UserListScreen(
                    uiState = usersUiState,
                    onUserClick = { user ->
                        navController.navigate(
                            "chat/${user.uid}/${Uri.encode(user.displayName)}"
                        ) {
                            popUpTo("users") { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(
            route = "chat/{otherUid}/{otherName}",
            arguments = listOf(
                navArgument("otherUid") { type = NavType.StringType },
                navArgument("otherName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val otherUid = backStackEntry.arguments?.getString("otherUid").orEmpty()
            val otherName = backStackEntry.arguments?.getString("otherName").orEmpty()
            val myUid = FirebaseAuth.getInstance().currentUser?.uid

            if (myUid == null || otherUid.isEmpty()) {
                Text("Something went wrong. Please go back and try again.")
            } else {
                ChatScreen(
                    myUid = myUid,
                    otherUid = otherUid,
                    otherName = otherName,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}