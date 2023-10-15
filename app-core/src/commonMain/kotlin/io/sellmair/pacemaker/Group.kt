package io.sellmair.pacemaker

data class Group(val members: List<UserState> = emptyList()) : Iterable<UserState> {
    override fun iterator(): Iterator<UserState> {
        return members.listIterator()
    }
}