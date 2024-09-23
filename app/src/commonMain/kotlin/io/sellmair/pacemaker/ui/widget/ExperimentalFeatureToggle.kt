package io.sellmair.pacemaker.ui.widget

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.sellmair.evas.compose.eventsOrNull
import io.sellmair.pacemaker.ApplicationFeature
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration.Companion.seconds


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.experimentalFeatureToggle(): Modifier {
    val eventBus = eventsOrNull()
    val coroutineScope = rememberCoroutineScope { eventBus ?: EmptyCoroutineContext }
    val experimentalFeatures = ApplicationFeature.entries.filter { !it.default }

    var doubleClicked = false

    return combinedClickable(
        onLongClick = {
            if (!doubleClicked) return@combinedClickable
            coroutineScope.launch {
                experimentalFeatures.forEach { feature ->
                    feature.toggle()
                }
            }
        },
        onDoubleClick = {
            doubleClicked = true
            coroutineScope.launch {
                delay(2.seconds)
                doubleClicked = false
            }
        },
        onClick = {},
        indication = null,
        interactionSource = remember { MutableInteractionSource() }
    )
}