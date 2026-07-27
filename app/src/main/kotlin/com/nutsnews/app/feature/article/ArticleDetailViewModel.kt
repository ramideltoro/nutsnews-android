package com.nutsnews.app.feature.article

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.ReadingStats
import com.nutsnews.app.core.model.StoryId
import com.nutsnews.app.data.story.ReadingStatsRepository
import com.nutsnews.app.data.story.SavedStoryRepository
import com.nutsnews.app.data.story.StoryNoteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Immutable
data class ArticleDetailUiState(
    val likedStoryIds: Set<StoryId> = emptySet(),
    val readingStats: ReadingStats? = null,
    val noteArticleId: StoryId? = null,
    val noteDraft: String = "",
    val hasSavedNote: Boolean = false,
    val isNoteLoading: Boolean = false,
    val noteStatusMessage: String? = null,
)

class ArticleDetailViewModel(
    private val savedStoryRepository: SavedStoryRepository,
    private val readingStatsRepository: ReadingStatsRepository,
    private val storyNoteRepository: StoryNoteRepository,
) : ViewModel() {
    private val likeMutex = Mutex()
    private val noteMutex = Mutex()
    private val noteEditorState = MutableStateFlow(ArticleNoteEditorState())
    private var activeArticle: Article? = null
    private var noteObservationJob: Job? = null
    private var noteStatusJob: Job? = null

    val uiState: StateFlow<ArticleDetailUiState> =
        combine(
            savedStoryRepository.stories,
            readingStatsRepository.observeStats(),
            noteEditorState,
        ) { savedStories, stats, noteEditor ->
            ArticleDetailUiState(
                likedStoryIds = savedStories.mapTo(linkedSetOf()) { story -> story.id },
                readingStats = stats,
                noteArticleId = noteEditor.articleId,
                noteDraft = noteEditor.draft,
                hasSavedNote = noteEditor.hasSavedNote,
                isNoteLoading = noteEditor.isLoading,
                noteStatusMessage = noteEditor.statusMessage,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ArticleDetailUiState(),
        )

    fun onArticleShown(article: Article) {
        loadNoteEditor(article)
        viewModelScope.launch {
            readingStatsRepository.recordStoryOpen(article)
        }
    }

    fun toggleLiked(article: Article) {
        viewModelScope.launch {
            likeMutex.withLock {
                savedStoryRepository.setLiked(
                    article = article,
                    isLiked = !savedStoryRepository.isLiked(article.stableId),
                )
            }
        }
    }

    fun onOriginalStoryOpened() {
        viewModelScope.launch {
            readingStatsRepository.recordOriginalStoryOpen()
        }
    }

    fun onNoteDraftChanged(
        article: Article,
        draft: String,
    ) {
        noteEditorState.update { current ->
            if (current.articleId != article.stableId) {
                current
            } else {
                current.copy(
                    draft = draft,
                    isDirty = true,
                    statusMessage = null,
                )
            }
        }
    }

    fun saveNote(article: Article) {
        viewModelScope.launch {
            noteMutex.withLock {
                val current = noteEditorState.value
                if (
                    current.articleId != article.stableId ||
                    activeArticle?.stableId != article.stableId
                ) {
                    return@withLock
                }
                val submittedDraft = current.draft
                val cleanedDraft = submittedDraft.trim()
                storyNoteRepository.setNote(article, cleanedDraft)
                noteEditorState.update { latest ->
                    if (latest.articleId != article.stableId) {
                        latest
                    } else {
                        latest.copy(
                            draft =
                                if (latest.draft == submittedDraft) {
                                    cleanedDraft
                                } else {
                                    latest.draft
                                },
                            hasSavedNote = cleanedDraft.isNotEmpty(),
                            isDirty = latest.draft != submittedDraft,
                            isLoading = false,
                        )
                    }
                }
                showNoteStatus(
                    articleId = article.stableId,
                    message =
                        if (cleanedDraft.isEmpty()) {
                            NoteClearedMessage
                        } else {
                            NoteSavedMessage
                        },
                )
            }
        }
    }

    fun clearNote(article: Article) {
        viewModelScope.launch {
            noteMutex.withLock {
                if (
                    noteEditorState.value.articleId != article.stableId ||
                    activeArticle?.stableId != article.stableId
                ) {
                    return@withLock
                }
                storyNoteRepository.clearNote(article)
                noteEditorState.update { latest ->
                    if (latest.articleId != article.stableId) {
                        latest
                    } else {
                        latest.copy(
                            draft = "",
                            hasSavedNote = false,
                            isDirty = false,
                            isLoading = false,
                        )
                    }
                }
                showNoteStatus(
                    articleId = article.stableId,
                    message = NoteClearedMessage,
                )
            }
        }
    }

    private fun loadNoteEditor(article: Article) {
        activeArticle = article
        noteObservationJob?.cancel()
        noteStatusJob?.cancel()
        noteEditorState.value =
            ArticleNoteEditorState(
                articleId = article.stableId,
                isLoading = true,
            )
        noteObservationJob =
            viewModelScope.launch {
                storyNoteRepository.observeNote(article).collect { note ->
                    noteEditorState.update { current ->
                        if (current.articleId != article.stableId) {
                            current
                        } else {
                            current.copy(
                                draft =
                                    if (current.isDirty) {
                                        current.draft
                                    } else {
                                        note?.text.orEmpty()
                                    },
                                hasSavedNote = !note?.text.isNullOrBlank(),
                                isLoading = false,
                            )
                        }
                    }
                }
            }
    }

    private fun showNoteStatus(
        articleId: StoryId,
        message: String,
    ) {
        noteStatusJob?.cancel()
        noteEditorState.update { current ->
            if (current.articleId == articleId) {
                current.copy(statusMessage = message)
            } else {
                current
            }
        }
        noteStatusJob =
            viewModelScope.launch {
                delay(NoteStatusDurationMillis)
                noteEditorState.update { current ->
                    if (
                        current.articleId == articleId &&
                        current.statusMessage == message
                    ) {
                        current.copy(statusMessage = null)
                    } else {
                        current
                    }
                }
            }
    }

    class Factory(
        private val savedStoryRepository: SavedStoryRepository,
        private val readingStatsRepository: ReadingStatsRepository,
        private val storyNoteRepository: StoryNoteRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ArticleDetailViewModel::class.java))
            return ArticleDetailViewModel(
                savedStoryRepository = savedStoryRepository,
                readingStatsRepository = readingStatsRepository,
                storyNoteRepository = storyNoteRepository,
            ) as T
        }
    }
}

@Immutable
private data class ArticleNoteEditorState(
    val articleId: StoryId? = null,
    val draft: String = "",
    val hasSavedNote: Boolean = false,
    val isDirty: Boolean = false,
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
)

private const val NoteSavedMessage = "Note saved on this device"
private const val NoteClearedMessage = "Note cleared"
private const val NoteStatusDurationMillis = 1_800L
