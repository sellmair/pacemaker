package io.sellmair.pacemaker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PageRouter(
    content: @Composable (page: Page) -> Unit
) {
    var previousPage by remember { mutableStateOf(Page.entries.first()) }
    var currentPage by remember { mutableStateOf(Page.entries.first()) }
    
    BackHandlerIfAny(enabled = currentPage != Page.entries.first()) {
        previousPage = currentPage
        currentPage = Page.entries.first()
    }
    

    Scaffold(bottomBar = {
        NavigationBar(
            modifier = Modifier.fillMaxWidth()
        ) {
            Page.entries.forEach { page ->
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

    ) { paddingValues ->
        Column(Modifier.fillMaxSize()) {
            Spacer(
                Modifier.windowInsetsBottomHeight(WindowInsets.systemBars)
            )
            Spacer(Modifier.height(24.dp))
            
            Box(modifier = Modifier.weight(1f)) {
                Page.entries.forEach { page ->
                    PageWithAnimation(
                        page = page,
                        previousPage = previousPage,
                        currentPage = currentPage
                    ) {
                        content(page)
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
        visible = page == currentPage,
        enter = if(isLeftEnter) leftEnterTransition else rightEnterTransition,
        exit = if(isLeftExit) leftExitTransition else rightExitTransition
    ) {
        content()
    }
}