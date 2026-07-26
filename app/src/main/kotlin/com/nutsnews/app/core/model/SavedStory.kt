package com.nutsnews.app.core.model

import java.time.Instant

data class SavedStory(
    val article: Article,
    val savedAt: Instant,
) {
    val id: StoryId
        get() = article.stableId
}
