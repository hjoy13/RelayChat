package com.example.relaychat.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun RegisterScreen(
    uiState: AuthUiState,
    onRegister: (String, String, String) -> Unit,
    onGoToLogin: () -> Unit
) {
    var displayName by rememberSaveable {
        mutableStateOf("")
    }

    var email by rememberSaveable {
        mutableStateOf("")
    }

    var password by rememberSaveable {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Create RelayChat account"
        )

        OutlinedTextField(
            value = displayName,
            onValueChange = {
                displayName = it
            },
            label = {
                Text("Display name")
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
            },
            label = {
                Text("Email")
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
            },
            label = {
                Text("Password")
            },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )

        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(top = 20.dp)
            )
        } else {
            Button(
                onClick = {
                    onRegister(
                        displayName,
                        email,
                        password
                    )
                },
                enabled = displayName.isNotBlank()
                        && email.isNotBlank()
                        && password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            ) {
                Text("Register")
            }
        }

        TextButton(
            onClick = onGoToLogin,
            enabled = !uiState.isLoading
        ) {
            Text("Already have an account? Log in")
        }
    }
}