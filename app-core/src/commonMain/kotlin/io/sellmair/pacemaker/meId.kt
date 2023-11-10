package io.sellmair.pacemaker

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import io.sellmair.pacemaker.model.UserId
import kotlin.random.Random

internal val Settings.meId: UserId
    get() {
        val key = "me.id"
        getLongOrNull(key)?.let { return UserId(it) }
        val newId = Random.nextLong()
        set(key, newId)
        return UserId(newId)
    }
