package com.nutsnews.app.core.model

import java.time.Instant

enum class StoryReflectionReaction(
    val id: String,
    val title: String,
    val shortTitle: String,
    val savedTitle: String,
) {
    Smile(
        id = "smile",
        title = "Made me smile",
        shortTitle = "Smile",
        savedTitle = "This one made you smile",
    ),
    Hope(
        id = "hope",
        title = "Gave me hope",
        shortTitle = "Hope",
        savedTitle = "This one gave you hope",
    ),
    Revisit(
        id = "revisit",
        title = "Worth revisiting",
        shortTitle = "Revisit",
        savedTitle = "Saved as worth revisiting",
    ),
    ;

    companion object {
        fun fromId(id: String): StoryReflectionReaction? =
            entries.firstOrNull { reaction -> reaction.id == id }
    }
}

data class StoryReflection(
    val articleId: StoryId,
    val articleTitle: String,
    val articleSource: String,
    val reaction: StoryReflectionReaction,
    val createdAt: Instant,
)
