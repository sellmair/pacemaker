package io.sellmair.pacemaker

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User

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