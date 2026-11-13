package com.nikhil.journal.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class JournalRepositoryTest {

    private lateinit var context: Context
    private lateinit var db: JournalDatabase
    private lateinit var dao: JournalDao
    private lateinit var storage: ImageStorage
    private lateinit var repository: JournalRepository
    private var now = 1_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, JournalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.journalDao()
        storage = ImageStorage(context)
        repository = JournalRepository(dao, storage) { now++ }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun fakeImage(name: String): String {
        val path = "${ImageStorage.IMAGES_DIR}/$name.jpg"
        storage.resolve(path).apply { parentFile?.mkdirs(); writeText("x") }
        return path
    }

    @Test
    fun entriesAreReturnedNewestFirstWithFirstImageAsThumbnail() = runTest {
        val old = repository.saveEntry(null, "Old", "a", LocalDate.of(2026, 1, 1), emptyList())
        val img1 = fakeImage("one")
        val img2 = fakeImage("two")
        val new = repository.saveEntry(null, "", "untitled", LocalDate.of(2026, 3, 1), listOf(img1, img2))

        val entries = repository.observeEntries("").first()

        assertEquals(listOf(new, old), entries.map { it.entry.id })
        assertEquals(img1, entries[0].thumbnailPath)
        assertNull(entries[1].thumbnailPath)
    }

    @Test
    fun searchMatchesTitleOrBodyAndTreatsWildcardsLiterally() = runTest {
        repository.saveEntry(null, "Beach day", "sunny", LocalDate.of(2026, 5, 1), emptyList())
        repository.saveEntry(null, "", "Went to the BEACH again", LocalDate.of(2026, 5, 2), emptyList())
        repository.saveEntry(null, "Budget", "saved 100% of it", LocalDate.of(2026, 5, 3), emptyList())

        assertEquals(2, repository.observeEntries("beach").first().size)
        assertEquals(1, repository.observeEntries("100%").first().size)
        assertEquals(0, repository.observeEntries("_").first().size)
        assertEquals(3, repository.observeEntries("   ").first().size)
    }

    @Test
    fun updatingEntryReplacesImagesAndDeletesRemovedFiles() = runTest {
        val keep = fakeImage("keep")
        val drop = fakeImage("drop")
        val id = repository.saveEntry(null, "t", "b", LocalDate.of(2026, 2, 2), listOf(keep, drop))
        val created = repository.getEntry(id)!!.entry

        val added = fakeImage("added")
        repository.saveEntry(id, "t2", "b2", LocalDate.of(2026, 2, 3), listOf(added, keep))

        val updated = repository.getEntry(id)!!
        assertEquals("t2", updated.entry.title)
        assertEquals(created.createdAt, updated.entry.createdAt)
        assertTrue(updated.entry.updatedAt > created.updatedAt)
        assertEquals(listOf(added, keep), updated.sortedImages.map { it.filePath })
        assertFalse(storage.resolve(drop).exists())
        assertTrue(storage.resolve(keep).exists())
    }

    @Test
    fun deletingEntryCascadesImageRowsAndRemovesFiles() = runTest {
        val img = fakeImage("gone")
        val id = repository.saveEntry(null, "t", "b", LocalDate.of(2026, 4, 4), listOf(img))

        repository.deleteEntry(id)

        assertNull(repository.getEntry(id))
        assertTrue(dao.getImagePaths(id).isEmpty())
        assertFalse(storage.resolve(img).exists())
    }
}
