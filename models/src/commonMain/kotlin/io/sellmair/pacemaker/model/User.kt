package io.sellmair.pacemaker.model

import kotlin.math.absoluteValue
import kotlin.random.Random

data class User(
    val id: UserId,
    val name: String,
    val isAdhoc: Boolean = false,
)

val User.nameAbbreviation: String
    get() = name.split(Regex("\\s")).joinToString("") { it.firstOrNull()?.uppercase() ?: "" }


fun randomNewUser(): User {
    val id = Random.nextLong()
    return User(id = UserId(id), name = "Anonymous ${(id % 100).absoluteValue}")
}