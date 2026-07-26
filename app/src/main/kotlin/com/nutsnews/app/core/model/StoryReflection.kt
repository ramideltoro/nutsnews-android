package com.nutsnews.app.core.model

import java.time.Instant

enum class StoryReflectionReaction(
    val id: String,
) {
    Smile("smile"),
    Hope("hope"),
    Revisit("revisit"),
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
