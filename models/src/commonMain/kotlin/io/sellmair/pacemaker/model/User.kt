package io.sellmair.pacemaker.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val isMe: Boolean,
    val id: UserId,
    val name: String,
    val isAdhoc: Boolean = false,
)

val User.nameAbbreviation: String
    get() = name.split(Regex("\\s")).joinToString("") { it.firstOrNull()?.uppercase() ?: "" }
