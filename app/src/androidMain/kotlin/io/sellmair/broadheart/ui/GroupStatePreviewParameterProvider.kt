package io.sellmair.broadheart.ui

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.sellmair.broadheart.GroupState
import io.sellmair.broadheart.dummyGroupState

class GroupStatePreviewParameterProvider : PreviewParameterProvider<GroupState?> {
    override val values: Sequence<GroupState?>
        get() = sequenceOf(dummyGroupState)
}