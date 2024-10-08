package io.sellmair.pacemaker.ui.settingsPage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.sellmair.pacemaker.UpdateMeIntent
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.nameAbbreviation
import io.sellmair.pacemaker.ui.MeColor
import io.sellmair.pacemaker.ui.widget.*
import io.sellmair.evas.emit

@Composable
internal fun SettingsPageHeader(me: User) {
    var userName by remember { mutableStateOf(me.name) }

    Column(modifier = Modifier.padding(24.dp)) {
        Row(Modifier.fillMaxWidth()) {
            val focusManager = LocalFocusManager.current

            BasicTextField(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                value = userName,
                textStyle = TextStyle.Headline,
                singleLine = true,
                keyboardActions = KeyboardActions(onDone = {
                    this.defaultKeyboardAction(ImeAction.Done)
                    focusManager.clearFocus()
                }),
                keyboardOptions = KeyboardOptions(
                    autoCorrect = false,
                    imeAction = ImeAction.Done
                ),
                onValueChange = Launching { newName ->
                    userName = newName
                    UpdateMeIntent.UpdateMe(me.copy(name = newName)).emit()
                })

            UserHead(
                abbreviation = me.nameAbbreviation,
                color = MeColor(),
                size = 32.dp,
                modifier = Modifier
                    .padding(4.dp)
                    .experimentalFeatureToggle()
            )
        }

        ColorHueSlider(
            modifier = Modifier.fillMaxWidth()
        )


    }
}
