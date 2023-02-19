package io.sellmair.broadheart.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.sellmair.broadheart.hrSensor.HeartRate
import io.sellmair.broadheart.hrSensor.HrSensorId
import io.sellmair.broadheart.hrSensor.HrSensorInfo
import io.sellmair.broadheart.service.GroupMemberState

class GroupMemberStateParameterProvider : PreviewParameterProvider<GroupMemberState> {
    override val values: Sequence<GroupMemberState>
        get() = sequenceOf(
            GroupMemberState(
                user = UserPreviewParameterProvider().values.first(),
                currentHeartRate = HeartRate(112f),
                upperHeartRateLimit = HeartRate(120f),
                sensorInfo = HrSensorInfo(
                    id = HrSensorId("aa:bb:cc:dd"),
                    address = "dd:ee:ff:gg:ee",
                    vendor = HrSensorInfo.Vendor.Polar,
                    rssi = 45
                )
            )
        )

}