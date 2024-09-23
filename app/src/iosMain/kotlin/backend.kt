    package io.sellmair.pacemaker

    import kotlinx.coroutines.CoroutineScope
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.SupervisorJob

    internal val backend by lazy {
        IosApplicationBackend().also { backend ->
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + backend.events + backend.states)
            scope.launchFrontendServices()
        }
    }
