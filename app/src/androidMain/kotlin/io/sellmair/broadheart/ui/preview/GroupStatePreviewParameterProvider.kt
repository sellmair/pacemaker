package io.sellmair.broadheart.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.sellmair.broadheart.Group

class GroupStatePreviewParameterProvider : PreviewParameterProvider<Group?> {
    override val values: Sequence<Group?>
        get() = sequenceOf(null)
}

