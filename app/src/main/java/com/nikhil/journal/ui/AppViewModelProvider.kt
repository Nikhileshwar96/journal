package com.nikhil.journal.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nikhil.journal.AppContainer
import com.nikhil.journal.JournalApp
import com.nikhil.journal.ui.detail.EntryDetailViewModel
import com.nikhil.journal.ui.edit.EditEntryViewModel
import com.nikhil.journal.ui.home.HomeViewModel

/** Creates every ViewModel from the manual DI [AppContainer]. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            HomeViewModel(container().repository, createSavedStateHandle())
        }
        initializer {
            EntryDetailViewModel(container().repository, createSavedStateHandle())
        }
        initializer {
            val container = container()
            EditEntryViewModel(container.repository, container.appScope, createSavedStateHandle())
        }
    }

    private fun CreationExtras.container(): AppContainer =
        (this[APPLICATION_KEY] as JournalApp).container
}
