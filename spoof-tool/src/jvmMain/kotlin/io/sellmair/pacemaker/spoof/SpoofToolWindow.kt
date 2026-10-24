package io.sellmair.pacemaker.spoof

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.sellmair.evas.compose.LocalEvents
import io.sellmair.evas.compose.composeValue
import kotlinx.coroutines.launch

private val Background = Color(0xFF0D1117)
private val Panel = Color(0xFF171E27)
private val Accent = Color(0xFF63D9C4)

@Composable
internal fun SpoofToolWindow() {
    val state = SpoofToolState.composeValue()
    val eventBus = LocalEvents.current
    val coroutineScope = rememberCoroutineScope()
    val shouldStop = state.status == SpoofToolState.Status.Advertising ||
        state.status == SpoofToolState.Status.Starting ||
        state.status == SpoofToolState.Status.Stopping

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            onPrimary = Color(0xFF071512),
            background = Background,
            surface = Panel,
            onSurface = Color(0xFFE5EDF5),
            secondary = Color(0xFF89A8C3),
        ),
    ) {
        Surface(Modifier.fillMaxSize(), color = Background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("SPOOF TOOL", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Bluetooth test broadcaster", fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Send controlled heart-rate data to Pacemaker during local testing.",
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }

                AdvertiserStatus(state)

                Card(
                    colors = CardDefaults.cardColors(containerColor = Panel),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text("Test identity", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        OutlinedTextField(
                            value = state.userName,
                            onValueChange = { value ->
                                coroutineScope.launch { eventBus?.emit(SpoofToolIntent.SetUserName(value)) }
                            },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = state.userId,
                            onValueChange = { value ->
                                if (value.all(Char::isDigit)) {
                                    coroutineScope.launch { eventBus?.emit(SpoofToolIntent.SetUserId(value)) }
                                }
                            },
                            label = { Text("User ID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Panel),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(15.dp),
                    ) {
                        Text("Broadcast values", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        HeartRateSlider(
                            title = "Heart rate",
                            value = state.heartRate,
                            onValueChange = { value ->
                                coroutineScope.launch { eventBus?.emit(SpoofToolIntent.SetHeartRate(value)) }
                            },
                        )
                        HeartRateSlider(
                            title = "Heart-rate limit",
                            value = state.heartRateLimit,
                            onValueChange = { value ->
                                coroutineScope.launch { eventBus?.emit(SpoofToolIntent.SetHeartRateLimit(value)) }
                            },
                        )
                    }
                }

                Button(
                    onClick = {
                        if (shouldStop) {
                            coroutineScope.launch { eventBus?.emit(SpoofToolIntent.Stop) }
                        } else {
                            coroutineScope.launch { eventBus?.emit(SpoofToolIntent.Start) }
                        }
                    },
                    enabled = state.status != SpoofToolState.Status.Stopping,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (shouldStop) Color(0xFF633C48) else Accent,
                    ),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        if (shouldStop) "Stop advertising" else "Start advertising",
                        fontWeight = FontWeight.Bold,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(state.message, color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
                    state.recentOutput.takeLast(3).forEach { line ->
                        Text(line, color = Color(0xFF708297), fontSize = 11.sp)
                    }
                }

                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun AdvertiserStatus(state: SpoofToolState) {
    val isAdvertising = state.status == SpoofToolState.Status.Advertising
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("ADVERTISER", color = Color(0xFF8494A7), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(if (isAdvertising) "Running" else state.status.name, fontWeight = FontWeight.SemiBold)
        }
        Text(
            if (isAdvertising) "● ON AIR" else "● OFF AIR",
            color = if (isAdvertising) Accent else Color(0xFF748093),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HeartRateSlider(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = Color(0xFFB9C7D5))
            Text("${sliderValue.toInt()} bpm", fontWeight = FontWeight.Bold, color = Accent)
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onValueChange(sliderValue.toInt()) },
            valueRange = 30f..220f,
            steps = 189,
        )
    }
}
