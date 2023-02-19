package io.sellmair.broadheart.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.sellmair.broadheart.User
import io.sellmair.broadheart.UserId

class UserPreviewParameterProvider : PreviewParameterProvider<User> {
    override val values: Sequence<User>
        get() = sequenceOf(
            User(
                isMe = true,
                isAdhoc = false,
                id = UserId(0),
                name = "Sebastian Sellmair"
            )
        )
}


