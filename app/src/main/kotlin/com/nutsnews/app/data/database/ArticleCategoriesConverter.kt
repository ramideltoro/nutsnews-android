package com.nutsnews.app.data.database

import androidx.room.TypeConverter
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ArticleCategoriesConverter {
    @TypeConverter
    fun encode(categories: List<String>): String = Json.encodeToString(categories)

    @TypeConverter
    fun decode(rawValue: String): List<String> =
        try {
            Json.decodeFromString(rawValue)
        } catch (_: SerializationException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
}
