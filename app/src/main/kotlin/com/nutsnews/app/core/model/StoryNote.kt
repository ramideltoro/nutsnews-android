package com.nutsnews.app.core.model

import java.time.Instant

data class StoryNote(
    val articleId: StoryId,
    val articleTitle: String,
    val text: String,
    val updatedAt: Instant,
)
