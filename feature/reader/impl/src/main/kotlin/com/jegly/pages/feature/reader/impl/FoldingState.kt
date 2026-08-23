package com.jegly.pages.feature.reader.impl

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker

@Composable
fun rememberFoldingFeature(): State<FoldingFeature?> {
    val context = LocalContext.current
    val activity = context as? Activity
    return remember(activity) {
        if (activity != null)
            WindowInfoTracker.getOrCreate(activity)
                .windowLayoutInfo(activity)
                .let { flow ->
                    kotlinx.coroutines.flow.flow {
                        flow.collect { info ->
                            emit(info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull())
                        }
                    }
                }
        else kotlinx.coroutines.flow.flowOf<FoldingFeature?>(null)
    }.collectAsState(initial = null)
}

fun FoldingFeature?.isSeparating(): Boolean =
    this?.isSeparating == true

fun FoldingFeature?.isTableTop(): Boolean =
    this?.state == FoldingFeature.State.HALF_OPENED &&
        this.orientation == FoldingFeature.Orientation.HORIZONTAL

fun FoldingFeature?.isBook(): Boolean =
    this?.state == FoldingFeature.State.HALF_OPENED &&
        this.orientation == FoldingFeature.Orientation.VERTICAL

// Fully-open inner display (Pixel Fold flat on a table) — ideal for dual-page reading.
fun FoldingFeature?.isDualPage(): Boolean =
    this != null &&
        this.state == FoldingFeature.State.FLAT &&
        this.orientation == FoldingFeature.Orientation.VERTICAL
