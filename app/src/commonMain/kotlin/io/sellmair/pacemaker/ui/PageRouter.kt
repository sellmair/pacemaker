package io.sellmair.pacemaker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import pacemaker.app.generated.resources.Res
import pacemaker.app.generated.resources.navigation_devices
import pacemaker.app.generated.resources.navigation_live
import pacemaker.app.generated.resources.navigation_profile

@Composable
fun PageRouter(
    content: @Composable (page: Page) -> Unit
) {
    var selectedPageIndex by rememberSaveable { mutableIntStateOf(Page.Live.ordinal) }
    val currentPage = Page.entries[selectedPageIndex]
    val stateHolder = rememberSaveableStateHolder()
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    BackHandlerIfAny(enabled = !imeVisible && currentPage != Page.Live) {
        selectedPageIndex = Page.Live.ordinal
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing.exclude(WindowInsets.ime),
        topBar = { PacemakerAppHeader(onProfileClick = { selectedPageIndex = Page.Profile.ordinal }) },
        bottomBar = {
            PacemakerBottomNavigation(currentPage) { selectedPageIndex = it.ordinal }
        }
    ) { innerPadding ->
        Box(
            Modifier.fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        ) {
            stateHolder.SaveableStateProvider(currentPage.name) {
                content(currentPage)
            }
        }
    }
}

@Composable
private fun PacemakerAppHeader(onProfileClick: () -> Unit) {
    val colors = SharedRhythmColors()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                )
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = PacemakerIcons.Mark,
                contentDescription = null,
                tint = if (MaterialTheme.colorScheme.background == SharedRhythmColors.Dark.background) {
                    SharedRhythmColors.Acid
                } else {
                    SharedRhythmColors.Charcoal
                },
                modifier = Modifier.size(34.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = "PACEMAKER",
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 1.5.sp,
                    color = colors.text
                )
            }
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.size(48.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(colors.surface)
                    .border(1.dp, colors.border, MaterialTheme.shapes.small)
            ) {
                Icon(PacemakerIcons.Settings, contentDescription = stringResource(Res.string.navigation_profile), tint = colors.text)
            }
        }
    }
}

@Composable
private fun PacemakerBottomNavigation(selectedPage: Page, onSelected: (Page) -> Unit) {
    val colors = SharedRhythmColors()
    Surface(color = colors.surface) {
        Column {
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
            Row(
                Modifier.fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Page.entries.forEach { page ->
                    val selected = selectedPage == page
                    Column(
                        modifier = Modifier.weight(1f)
                            .heightIn(min = 64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) colors.raised else Color.Transparent)
                            .semantics {
                                this.selected = selected
                            }
                            .clickable(role = Role.Tab, onClick = { onSelected(page) }),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
                    ) {
                        Icon(
                            imageVector = when (page) {
                                Page.Live -> PacemakerIcons.Rhythm
                                Page.Devices -> PacemakerIcons.Sensor
                                Page.Profile -> PacemakerIcons.Settings
                            },
                            contentDescription = null,
                            tint = if (selected) colors.accentText else colors.muted,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            stringResource(
                                when (page) {
                                    Page.Live -> Res.string.navigation_live
                                    Page.Devices -> Res.string.navigation_devices
                                    Page.Profile -> Res.string.navigation_profile
                                }
                            ),
                            color = if (selected) colors.accentText else colors.muted,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}
