package io.sellmair.pacemaker.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.sellmair.evas.compose.composeValue
import io.sellmair.pacemaker.ApplicationFeature

@Composable
fun PageRouter(
    content: @Composable (page: Page) -> Unit
) {
    val pages = getEnabledPages()

    var previousPage by remember { mutableStateOf(pages.first()) }
    var currentPage by remember { mutableStateOf(pages.first()) }

    BackHandlerIfAny(enabled = currentPage != pages.first()) {
        previousPage = currentPage
        currentPage = pages.first()
    }

    Scaffold(bottomBar = {
        NavigationBar(
            modifier = Modifier.fillMaxWidth()
        ) {
            pages.forEach { page ->
                key(page) {
                    NavigationBarItem(
                        page = page,
                        currentPage = currentPage,
                        onSelected = {
                            previousPage = currentPage
                            currentPage = page
                        }
                    )
                }
            }
        }
    }

    ) { paddingValues ->
        Column(Modifier.fillMaxSize()) {
            Spacer(
                Modifier.windowInsetsBottomHeight(WindowInsets.systemBars)
            )
            Spacer(Modifier.height(24.dp))

            Box(modifier = Modifier.weight(1f)) {
                pages.forEach { page ->
                    key(page) {
                        PageWithAnimation(
                            page = page,
                            previousPage = previousPage,
                            currentPage = currentPage
                        ) {
                            content(page)
                        }
                    }
                }
            }
            Spacer(Modifier.height(paddingValues.calculateBottomPadding()))
        }
    }
}

@Composable
private fun RowScope.NavigationBarItem(
    currentPage: Page,
    page: Page,
    onSelected: () -> Unit = {}
) {
    NavigationBarItem(
        selected = currentPage == page,
        onClick = onSelected,
        icon = {
            Icon(
                when (page) {
                    Page.MainPage -> Icons.Outlined.FavoriteBorder
                    Page.TimelinePage -> Icons.Default.Timeline
                    Page.SettingsPage -> Icons.Default.Settings
                }, contentDescription = null
            )
        }
    )
}

@Composable
private fun PageWithAnimation(
    page: Page,
    previousPage: Page,
    currentPage: Page,
    content: @Composable () -> Unit
) {
    val leftEnterTransition = slideInHorizontally(tween(250, 250)) + fadeIn(tween(500, 250))
    val leftExitTransition = slideOutHorizontally(tween(500)) + fadeOut(tween(250))

    val rightEnterTransition = slideInHorizontally(tween(250, 250), initialOffsetX = { it }) + fadeIn(tween(500, 250))
    val rightExitTransition = slideOutHorizontally(tween(500), targetOffsetX = { it }) + fadeOut(tween(250))

    val isLeftEnter = page.ordinal < previousPage.ordinal
    val isLeftExit = page.ordinal < currentPage.ordinal

    AnimatedVisibility(
        label = "Page: ${page.name}",
        visible = page == currentPage,
        enter = if (isLeftEnter) leftEnterTransition else rightEnterTransition,
        exit = if (isLeftExit) leftExitTransition else rightExitTransition
    ) {
        content()
    }
}

@Composable
fun getEnabledPages(): List<Page> {
    val sessionsEnabled = ApplicationFeature.Sessions.state.composeValue().enabled
    val pages = Page.entries.toMutableList()
    if (!sessionsEnabled) pages.remove(Page.TimelinePage)
    return pages
}