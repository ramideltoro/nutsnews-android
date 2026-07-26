package com.nutsnews.app.feature.feed

import androidx.compose.runtime.Immutable
import com.nutsnews.app.core.model.Article

@Immutable
data class ArticleFeedUiState(
    val articles: List<Article> = emptyList(),
    val availableCategories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val nextPage: Int? = null,
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isPaginating: Boolean = false,
    val isStale: Boolean = false,
    val errorMessage: String? = null,
) {
    val isLoading: Boolean
        get() = isInitialLoading || isRefreshing || isPaginating

    val isEmpty: Boolean
        get() = articles.isEmpty() && !isLoading

    val canLoadMore: Boolean
        get() = nextPage != null && !isLoading
}
