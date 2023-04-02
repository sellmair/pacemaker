package io.sellmair.pacemaker.ble

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext


@OptIn(ExperimentalCoroutinesApi::class)
private val bleDispatcher: CoroutineDispatcher = newFixedThreadPoolContext(1, "Ble Background Thread")

val Dispatchers.ble get() = bleDispatcher