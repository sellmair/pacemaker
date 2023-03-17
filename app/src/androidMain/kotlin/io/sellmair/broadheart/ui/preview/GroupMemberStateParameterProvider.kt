package io.sellmair.broadheart.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.HeartRateSensorId
import io.sellmair.broadheart.model.HeartRateSensorInfo
import io.sellmair.broadheart.GroupMember

class GroupMemberStateParameterProvider : PreviewParameterProvider<GroupMember> {
    override val values: Sequence<GroupMember>
        get() = sequenceOf(
            GroupMember(
                user = UserPreviewParameterProvider().values.first(),
                currentHeartRate = HeartRate(112f),
                heartRateLimit = HeartRate(120f),
                sensorInfo = HeartRateSensorInfo(
                    id = HeartRateSensorId("aa:bb:cc:dd"),
                    address = "dd:ee:ff:gg:ee",
                    vendor = HeartRateSensorInfo.Vendor.Polar,
                    rssi = 45
                )
            )
        )

}