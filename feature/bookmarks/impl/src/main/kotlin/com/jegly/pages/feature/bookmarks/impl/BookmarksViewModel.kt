package com.jegly.pages.feature.bookmarks.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jegly.pages.core.data.repository.PagesRepository
import com.jegly.pages.core.model.Bookmark
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface BookmarksUiState {
    data object Loading : BookmarksUiState
    data class Success(val bookmarks: List<Bookmark>) : BookmarksUiState
}

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    repository: PagesRepository,
) : ViewModel() {

    val uiState: StateFlow<BookmarksUiState> = repository.getAllBookmarks()
        .map { BookmarksUiState.Success(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BookmarksUiState.Loading,
        )
}
