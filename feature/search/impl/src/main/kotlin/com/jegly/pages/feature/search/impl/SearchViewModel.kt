package com.jegly.pages.feature.search.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jegly.pages.core.data.repository.PagesRepository
import com.jegly.pages.core.model.Book
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data class Success(val results: List<Book>) : SearchUiState
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel @Inject constructor(
    private val repository: PagesRepository,
) : ViewModel() {

    val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<SearchUiState> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(SearchUiState.Idle)
            else repository.searchBooks(query).map { SearchUiState.Success(it) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SearchUiState.Idle,
        )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }
}
