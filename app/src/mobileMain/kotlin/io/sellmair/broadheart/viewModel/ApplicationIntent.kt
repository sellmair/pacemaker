package io.sellmair.broadheart.viewModel

import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.HeartRateSensorId
import io.sellmair.broadheart.model.User

sealed interface ApplicationIntent {
    sealed interface MainPageIntent : ApplicationIntent {
        data class UpdateHeartRateLimit(val heartRateLimit: HeartRate): MainPageIntent
    }

    sealed interface SettingsPageIntent : ApplicationIntent {
        data class UpdateMe(val user: User) : SettingsPageIntent
        data class LinkSensor(val user: User, val sensor: HeartRateSensorId) : SettingsPageIntent
        data class UnlinkSensor(val sensor: HeartRateSensorId) : SettingsPageIntent
        data class CreateAdhocUser(val sensor: HeartRateSensorId) : SettingsPageIntent
        data class UpdateAdhocUser(val user: User) : SettingsPageIntent
        data class DeleteAdhocUser(val user: User) : SettingsPageIntent
        data class UpdateAdhocUserLimit(val user: User, val limit: HeartRate) : SettingsPageIntent
    }
}