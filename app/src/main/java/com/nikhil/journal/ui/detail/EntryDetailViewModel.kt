package com.nikhil.journal.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.journal.data.JournalEntry
import com.nikhil.journal.data.JournalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed interface DetailUiState {
    data object Loading : DetailUiState
    /** The entry was deleted (here or elsewhere) or never existed. */
    data object Gone : DetailUiState
    data class Loaded(val entry: JournalEntry, val images: List<File>) : DetailUiState
}

class EntryDetailViewModel(
    private val repository: JournalRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val entryId: Long = checkNotNull(savedStateHandle.get<Long>(ARG_ENTRY_ID))

    val uiState: StateFlow<DetailUiState> = repository.observeEntry(entryId)
        .map { row ->
            if (row == null) {
                DetailUiState.Gone
            } else {
                DetailUiState.Loaded(
                    entry = row.entry,
                    images = row.sortedImages.map { repository.images.resolve(it.filePath) },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState.Loading)

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    fun delete() {
        if (_isDeleting.value) return
        _isDeleting.value = true
        viewModelScope.launch { repository.deleteEntry(entryId) }
    }

    companion object {
        const val ARG_ENTRY_ID = "entryId"
    }
}
