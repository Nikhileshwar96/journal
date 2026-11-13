package com.nikhil.journal

import android.app.Application
import com.nikhil.journal.data.ImageStorage
import com.nikhil.journal.data.JournalDatabase
import com.nikhil.journal.data.JournalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class JournalApp : Application() {
    /** Manual DI: a single app-wide container, created lazily. */
    val container: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(app: Application) {
    /** Outlives any screen; used for cleanup work that must finish after a ViewModel is cleared. */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob())

    private val database: JournalDatabase by lazy { JournalDatabase.create(app) }
    val imageStorage: ImageStorage by lazy { ImageStorage(app) }
    val repository: JournalRepository by lazy {
        JournalRepository(database.journalDao(), imageStorage)
    }
}
