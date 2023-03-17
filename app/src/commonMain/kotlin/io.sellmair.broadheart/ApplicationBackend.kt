package io.sellmair.broadheart

interface ApplicationBackend {
    val userService: UserService
    val groupService: GroupService
}