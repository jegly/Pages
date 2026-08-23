package com.jegly.pages.feature.reader.api.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class ReaderNavKey(val bookUri: String, val startPage: Int = -1) : NavKey
