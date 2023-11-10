package io.sellmair.pacemaker.model

import kotlin.math.absoluteValue

data class User(
    val id: UserId,
    val name: String,
    val isAdhoc: Boolean = false,
)

val User.nameAbbreviation: String
    get() {
        if (name.isBlank()) return ""
        val parts = name.split(Regex("\\s"))
            .filter { it.isNotBlank() }

        if (parts.size >= 2) {
            return (parts.first().firstOrNull() ?: "").toString().uppercase() +
                    (parts.last().firstOrNull() ?: "").toString().uppercase()
        }

        return name.take(2)
    }


fun newUser(id: UserId): User {
    return User(id = id, name = "Anonymous ${(id.value % 100).absoluteValue}")
}