package com.inkr8.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inkr8.presentation.state.SessionUiState

@Composable
fun LoginScreen(state: SessionUiState, login: () -> Unit, retry: () -> Unit, logout: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Inkr8", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
            Text("Escribe, practica y compite.", style = MaterialTheme.typography.titleLarge)
            if (state.loading) CircularProgressIndicator()
            ErrorMessage(state.error, if (state.signedIn) retry else null)
            if (state.signedIn) TextButton(onClick = logout) { Text("Cerrar sesión") }
            else Button(onClick = login, enabled = !state.loading) { Text("Continuar con Google") }
        }
    }
}
@Composable
fun ConfigurationScreen() {
    Inkr8Theme { Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("Inkr8", style = MaterialTheme.typography.displayLarge)
            Text("El servicio todavía no está configurado. Contacta al administrador para activar la aplicación.")
        }
    } }
}
