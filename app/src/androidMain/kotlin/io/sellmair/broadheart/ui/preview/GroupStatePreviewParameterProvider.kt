package io.sellmair.broadheart.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.sellmair.broadheart.service.GroupState

class GroupStatePreviewParameterProvider : PreviewParameterProvider<GroupState?> {
    override val values: Sequence<GroupState?>
        get() = sequenceOf(null)
}

