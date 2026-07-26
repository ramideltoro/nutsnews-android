package com.nutsnews.app.core.model

@JvmInline
value class StoryId(val value: String) {
    init {
        require(value.isNotBlank()) { "Story ID cannot be blank." }
    }
}
