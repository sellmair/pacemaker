package io.sellmair.pacemaker

/** Accept only a newer Stop generation for this run, or for an unspecified run target. */
fun shouldStopWatchMeasurement(
    activeRunId: String?, lastStopGeneration: Long, targetRunId: String?, generation: Long
): Boolean = generation > lastStopGeneration && (targetRunId == null || targetRunId == activeRunId)
