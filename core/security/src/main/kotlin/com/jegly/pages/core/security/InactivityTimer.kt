package com.jegly.pages.core.security

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val INACTIVITY_TIMEOUT_MS = 5 * 60 * 1000L

@Singleton
class InactivityTimer @Inject constructor() {

    private var job: Job? = null
    private var onTimeout: (() -> Unit)? = null

    fun start(scope: CoroutineScope, onTimeout: () -> Unit) {
        this.onTimeout = onTimeout
        reset(scope)
    }

    fun reset(scope: CoroutineScope) {
        job?.cancel()
        job = scope.launch {
            delay(INACTIVITY_TIMEOUT_MS)
            onTimeout?.invoke()
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
