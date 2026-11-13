package com.nikhil.journal.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.LocalDate

@Database(
    entities = [JournalEntry::class, EntryImage::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao

    companion object {
        fun create(context: Context): JournalDatabase =
            Room.databaseBuilder(context, JournalDatabase::class.java, "journal.db").build()
    }
}

class Converters {
    @TypeConverter
    fun fromEpochDay(value: Long): LocalDate = LocalDate.ofEpochDay(value)

    @TypeConverter
    fun toEpochDay(date: LocalDate): Long = date.toEpochDay()
}
