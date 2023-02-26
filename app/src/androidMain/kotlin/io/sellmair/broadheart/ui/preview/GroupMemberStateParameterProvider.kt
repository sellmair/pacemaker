package io.sellmair.broadheart.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.HeartRateSensorId
import io.sellmair.broadheart.model.HeartRateSensorInfo
import io.sellmair.broadheart.service.GroupMemberState

class GroupMemberStateParameterProvider : PreviewParameterProvider<GroupMemberState> {
    override val values: Sequence<GroupMemberState>
        get() = sequenceOf(
            GroupMemberState(
                user = UserPreviewParameterProvider().values.first(),
                currentHeartRate = HeartRate(112f),
                upperHeartRateLimit = HeartRate(120f),
                sensorInfo = HeartRateSensorInfo(
                    id = HeartRateSensorId("aa:bb:cc:dd"),
                    address = "dd:ee:ff:gg:ee",
                    vendor = HeartRateSensorInfo.Vendor.Polar,
                    rssi = 45
                )
            )
        )

}