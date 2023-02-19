package io.sellmair.broadheart.ui.settingsPage

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.sellmair.broadheart.User
import io.sellmair.broadheart.displayColor
import io.sellmair.broadheart.nameAbbreviation
import io.sellmair.broadheart.ui.toColor
import io.sellmair.broadheart.ui.widget.UserHead

@Composable
fun SettingsPageHeader(
    me: User,
    onEvent: (SettingsPageEvent) -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        IconButton(onClick = { onEvent(SettingsPageEvent.Back) }) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = me.displayColor.toColor(),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(horizontal = 8.dp)
            )
        }

        BasicTextField(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
            value = me.name,
            textStyle = TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            ),
            singleLine = true,
            onValueChange = { newName ->
                onEvent(SettingsPageEvent.UpdateMe(me.copy(name = newName)))
            })

        UserHead(
            abbreviation = me.nameAbbreviation,
            color = me.displayColor.toColor(),
            size = 32.dp,
            modifier = Modifier
                .padding(4.dp)
        )
    }
}