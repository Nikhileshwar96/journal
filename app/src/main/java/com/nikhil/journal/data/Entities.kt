package com.nikhil.journal.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.LocalDate

@Entity(
    tableName = "journal_entries",
    indices = [Index("entryDate")],
)
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val entryDate: LocalDate,
    /** Epoch millis. */
    val createdAt: Long,
    /** Epoch millis. */
    val updatedAt: Long,
)

@Entity(
    tableName = "entry_images",
    foreignKeys = [
        ForeignKey(
            entity = JournalEntry::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("entryId")],
)
data class EntryImage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    /** Path relative to `filesDir`, e.g. `images/<uuid>.jpg`. Resolve with [ImageStorage.resolve]. */
    val filePath: String,
    val position: Int,
)

/** Row for the home list: the entry plus the path of its first image, if any. */
data class EntrySummary(
    @Embedded val entry: JournalEntry,
    @ColumnInfo(name = "thumbnailPath") val thumbnailPath: String?,
)

data class EntryWithImages(
    @Embedded val entry: JournalEntry,
    @Relation(parentColumn = "id", entityColumn = "entryId")
    val images: List<EntryImage>,
) {
    val sortedImages: List<EntryImage> get() = images.sortedBy { it.position }
}
