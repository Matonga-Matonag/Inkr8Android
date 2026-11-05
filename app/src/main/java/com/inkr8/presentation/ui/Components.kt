package com.inkr8.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inkr8.domain.model.Season
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun ErrorMessage(message: String?, retry: (() -> Unit)? = null) {
    if (message != null) {
        Text(message, color = MaterialTheme.colorScheme.error)
        if (retry != null) TextButton(onClick = retry) { Text("Reintentar") }
    }
}
@Composable
fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content) }
}
fun seasonDates(season: Season): String {
    val format = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.forLanguageTag("es-PE")).apply { timeZone = TimeZone.getTimeZone("UTC") }
    return "${format.format(Date(season.start))} — ${format.format(Date(season.end))} UTC"
}
