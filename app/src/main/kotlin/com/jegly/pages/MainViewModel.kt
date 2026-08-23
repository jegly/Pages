package com.jegly.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jegly.pages.core.data.repository.PagesRepository
import com.jegly.pages.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: PagesRepository,
) : ViewModel() {
    val preferences: StateFlow<UserPreferences> = repository.getUserPreferences()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    /**
     * Raw preferences flow without a placeholder initial value. Use this when the
     * first emission must reflect the actual persisted state (e.g. the launch-time
     * biometric lock check), since [preferences].first() would return the default.
     */
    val persistedPreferences: Flow<UserPreferences> = repository.getUserPreferences()

    fun createCollection(name: String) {
        viewModelScope.launch { repository.createCollection(name) }
    }
}
