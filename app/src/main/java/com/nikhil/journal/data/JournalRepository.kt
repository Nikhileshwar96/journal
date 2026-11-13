package com.nikhil.journal.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class JournalRepository(
    private val dao: JournalDao,
    val images: ImageStorage,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun observeEntries(query: String): Flow<List<EntrySummary>> =
        dao.observeEntries(JournalDao.likePattern(query))

    fun observeEntry(id: Long): Flow<EntryWithImages?> = dao.observeEntry(id)

    suspend fun getEntry(id: Long): EntryWithImages? = dao.getEntry(id)

    /**
     * Creates (when [id] is null) or updates an entry. Image files that were attached before
     * but are no longer in [imagePaths] are deleted from disk after the database commit.
     */
    suspend fun saveEntry(
        id: Long?,
        title: String,
        body: String,
        entryDate: LocalDate,
        imagePaths: List<String>,
    ): Long {
        val now = clock()
        val existing = id?.let { dao.getEntry(it) }
        val entry = existing?.entry?.copy(
            title = title,
            body = body,
            entryDate = entryDate,
            updatedAt = now,
        ) ?: JournalEntry(
            title = title,
            body = body,
            entryDate = entryDate,
            createdAt = now,
            updatedAt = now,
        )
        val savedId = dao.upsertEntryWithImages(entry, imagePaths)
        val removed = existing?.images.orEmpty().map { it.filePath } - imagePaths.toSet()
        if (removed.isNotEmpty()) images.delete(removed)
        return savedId
    }

    /** Deletes the entry (images rows cascade) and its image files. */
    suspend fun deleteEntry(id: Long) {
        val paths = dao.getImagePaths(id)
        dao.deleteEntry(id)
        images.delete(paths)
    }
}
