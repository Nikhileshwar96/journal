package com.nikhil.journal

import android.app.Application
import com.nikhil.journal.data.ImageStorage
import com.nikhil.journal.data.JournalDatabase
import com.nikhil.journal.data.JournalRepository

class JournalApp : Application() {
    /** Manual DI: a single app-wide container, created lazily. */
    val container: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(app: Application) {
    private val database: JournalDatabase by lazy { JournalDatabase.create(app) }
    val imageStorage: ImageStorage by lazy { ImageStorage(app) }
    val repository: JournalRepository by lazy {
        JournalRepository(database.journalDao(), imageStorage)
    }
}
