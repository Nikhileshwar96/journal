package com.nikhil.journal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class JournalDao {

    /**
     * Entries newest first, optionally filtered by [pattern] (a LIKE pattern built with
     * [likePattern]; `%` matches everything).
     */
    @Query(
        """
        SELECT e.*,
            (SELECT i.filePath FROM entry_images i
             WHERE i.entryId = e.id ORDER BY i.position LIMIT 1) AS thumbnailPath
        FROM journal_entries e
        WHERE e.title LIKE :pattern ESCAPE '\' OR e.body LIKE :pattern ESCAPE '\'
        ORDER BY e.entryDate DESC, e.createdAt DESC
        """,
    )
    abstract fun observeEntries(pattern: String): Flow<List<EntrySummary>>

    @Transaction
    @Query("SELECT * FROM journal_entries WHERE id = :id")
    abstract fun observeEntry(id: Long): Flow<EntryWithImages?>

    @Transaction
    @Query("SELECT * FROM journal_entries WHERE id = :id")
    abstract suspend fun getEntry(id: Long): EntryWithImages?

    @Query("SELECT filePath FROM entry_images WHERE entryId = :entryId")
    abstract suspend fun getImagePaths(entryId: Long): List<String>

    @Insert
    protected abstract suspend fun insertEntry(entry: JournalEntry): Long

    @Update
    protected abstract suspend fun updateEntry(entry: JournalEntry)

    @Insert
    protected abstract suspend fun insertImages(images: List<EntryImage>)

    @Query("DELETE FROM entry_images WHERE entryId = :entryId")
    protected abstract suspend fun deleteImagesForEntry(entryId: Long)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    abstract suspend fun deleteEntry(id: Long)

    /** Inserts or updates [entry] and replaces its images with [imagePaths] (in order). Returns the entry id. */
    @Transaction
    open suspend fun upsertEntryWithImages(entry: JournalEntry, imagePaths: List<String>): Long {
        val id = if (entry.id == 0L) {
            insertEntry(entry)
        } else {
            updateEntry(entry)
            deleteImagesForEntry(entry.id)
            entry.id
        }
        if (imagePaths.isNotEmpty()) {
            insertImages(
                imagePaths.mapIndexed { index, path ->
                    EntryImage(entryId = id, filePath = path, position = index)
                },
            )
        }
        return id
    }

    companion object {
        /** Builds a `LIKE ... ESCAPE '\'` pattern that matches [query] literally anywhere in the text. */
        fun likePattern(query: String): String {
            val escaped = query.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
            return "%$escaped%"
        }
    }
}
