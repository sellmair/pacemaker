package io.sellmair.broadheart.backend

import io.sellmair.broadheart.service.GroupService
import io.sellmair.broadheart.service.UserService

interface ApplicationBackend {
    val userService: UserService
    val groupService: GroupService
}