package com.nutsnews.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration

@Database(
    entities = [
        SavedStoryEntity::class,
        StoryNoteEntity::class,
        StoryReflectionEntity::class,
        ReadingStoryOpenEntity::class,
        OriginalStoryOpenEntity::class,
    ],
    version = NutsNewsDatabase.SchemaVersion,
    exportSchema = true,
)
@TypeConverters(ArticleCategoriesConverter::class)
abstract class NutsNewsDatabase : RoomDatabase() {
    abstract fun savedStoryDao(): SavedStoryDao

    abstract fun storyNoteDao(): StoryNoteDao

    abstract fun storyReflectionDao(): StoryReflectionDao

    abstract fun readingActivityDao(): ReadingActivityDao

    companion object {
        const val DatabaseName = "nutsnews.db"
        const val SchemaVersion = 1

        fun create(
            context: Context,
            name: String = DatabaseName,
        ): NutsNewsDatabase =
            Room
                .databaseBuilder(
                    context.applicationContext,
                    NutsNewsDatabase::class.java,
                    name,
                ).addMigrations(*NutsNewsDatabaseMigrations.all.toTypedArray())
                .build()
    }
}

/**
 * The append-only migration registry. Every schema bump adds its explicit
 * migration here; destructive fallback is intentionally never enabled.
 */
object NutsNewsDatabaseMigrations {
    val all: List<Migration> = emptyList()
}
