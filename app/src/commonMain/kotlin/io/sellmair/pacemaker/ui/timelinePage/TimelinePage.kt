package io.sellmair.pacemaker.ui.timelinePage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.sellmair.evas.compose.composeValue
import io.sellmair.pacemaker.SessionsState
import io.sellmair.pacemaker.model.Session
import io.sellmair.pacemaker.ui.widget.Headline
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun TimelinePage() {
    val sessions = SessionsState.composeValue().sessions
    TimelinePage(sessions)
}

@Composable
fun TimelinePage(sessions: List<Session>) {
    LazyColumn(Modifier.fillMaxWidth()) {
        item {
            Headline("History", Modifier.padding(24.dp))
        }

        if (sessions.isEmpty()) {
            item {
                EmptyTimelinePlaceholder()
            }
        } else {
            sessions.forEach { session ->
                item(session.id.value) {
                    SessionItem(session)
                }
            }
        }
    }
}


@Composable
private fun EmptyTimelinePlaceholder() {
    Text(
        "No run, yet",
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxSize()
            .defaultMinSize(minHeight = 150.dp)
    )
}

@Composable
private fun SessionItem(session: Session) {
    val startTime = session.startTime.toLocalDateTime(TimeZone.currentSystemDefault())
    Column(Modifier.padding(24.dp)) {
        Text(
            startTime.dateString(),
            fontWeight = FontWeight.Bold
        )
        Text(
            startTime.clockString(),
            fontWeight = FontWeight.Light
        )
        Spacer(Modifier.height(12.dp))
    }
}

private fun LocalDateTime.dateString(): String = buildString {
    append(dayOfMonth.twoDigitString())
    append(".")
    append(monthNumber.twoDigitString())
    if (year != Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year) {
        append(".${year}")
    }
}

private fun LocalDateTime.clockString(): String = "${hour.twoDigitString()}:${minute.twoDigitString()}"

private fun Int.twoDigitString(): String {
    return if (this >= 10) this.toString()
    else "0$this"
}

