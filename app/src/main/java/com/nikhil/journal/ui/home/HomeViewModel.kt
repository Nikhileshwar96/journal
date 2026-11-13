package com.nikhil.journal.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.journal.data.EntrySummary
import com.nikhil.journal.data.JournalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File
import java.time.LocalDate

data class EntryListItem(
    val id: Long,
    val title: String,
    val body: String,
    val thumbnail: File?,
)

data class DateGroup(val date: LocalDate, val entries: List<EntryListItem>)

data class HomeUiState(
    val isLoading: Boolean = true,
    val groups: List<DateGroup> = emptyList(),
)

class HomeViewModel(
    private val repository: JournalRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val query: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")
    val isSearching: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_SEARCHING, false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = query
        .flatMapLatest { repository.observeEntries(it) }
        .map { HomeUiState(isLoading = false, groups = it.groupByDate()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onQueryChange(value: String) {
        savedStateHandle[KEY_QUERY] = value
    }

    fun setSearching(searching: Boolean) {
        savedStateHandle[KEY_SEARCHING] = searching
        if (!searching) savedStateHandle[KEY_QUERY] = ""
    }

    private fun List<EntrySummary>.groupByDate(): List<DateGroup> =
        groupBy { it.entry.entryDate } // preserves the DAO's newest-first order
            .map { (date, rows) ->
                DateGroup(
                    date = date,
                    entries = rows.map { row ->
                        EntryListItem(
                            id = row.entry.id,
                            title = row.entry.title,
                            body = row.entry.body,
                            thumbnail = row.thumbnailPath?.let(repository.images::resolve),
                        )
                    },
                )
            }

    private companion object {
        const val KEY_QUERY = "query"
        const val KEY_SEARCHING = "searching"
    }
}
