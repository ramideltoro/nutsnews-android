package com.nutsnews.app.feature.article

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.ArticlesResponse
import com.nutsnews.app.core.model.ReadingStats
import com.nutsnews.app.core.model.StoryId
import com.nutsnews.app.core.model.StoryReflection
import com.nutsnews.app.core.model.StoryReflectionReaction
import com.nutsnews.app.data.article.ArticleStateCodec
import com.nutsnews.app.data.story.ReadingStatsRepository
import com.nutsnews.app.data.story.SavedStoryRepository
import com.nutsnews.app.data.story.StoryNoteRepository
import com.nutsnews.app.data.story.StoryReflectionRepository
import com.nutsnews.app.widget.NoOpWidgetRefreshRequester
import com.nutsnews.app.widget.WidgetRefreshRequester
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Immutable
data class ArticleDetailUiState(
    val activeArticle: Article? = null,
    val likedStoryIds: Set<StoryId> = emptySet(),
    val readingStats: ReadingStats? = null,
    val noteArticleId: StoryId? = null,
    val noteDraft: String = "",
    val hasSavedNote: Boolean = false,
    val isNoteLoading: Boolean = false,
    val noteStatusMessage: String? = null,
    val reflectionArticleId: StoryId? = null,
    val reflection: StoryReflection? = null,
    val isReflectionLoading: Boolean = false,
    val reflectionStatusMessage: String? = null,
)

class ArticleDetailViewModel(
    private val savedStoryRepository: SavedStoryRepository,
    private val readingStatsRepository: ReadingStatsRepository,
    private val storyNoteRepository: StoryNoteRepository,
    private val storyReflectionRepository: StoryReflectionRepository,
    private val widgetRefreshRequester: WidgetRefreshRequester =
        NoOpWidgetRefreshRequester,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    private val likeMutex = Mutex()
    private val noteMutex = Mutex()
    private val reflectionMutex = Mutex()
    private val noteEditorState = MutableStateFlow(ArticleNoteEditorState())
    private val reflectionEditorState = MutableStateFlow(ArticleReflectionEditorState())
    private val activeArticleState =
        MutableStateFlow(
            ArticleStateCodec
                .decodeOrNull(savedStateHandle[ActiveArticleStateKey])
                ?.articles
                ?.firstOrNull(),
        )
    private var activeArticle: Article? = null
    private var noteObservationJob: Job? = null
    private var noteStatusJob: Job? = null
    private var reflectionObservationJob: Job? = null
    private var reflectionStatusJob: Job? = null
    private val recordedStoryIds =
        savedStateHandle
            .get<ArrayList<String>>(RecordedStoryIdsStateKey)
            ?.toCollection(linkedSetOf())
            ?: linkedSetOf()

    val uiState: StateFlow<ArticleDetailUiState> =
        combine(
            savedStoryRepository.stories,
            readingStatsRepository.observeStats(),
            noteEditorState,
            reflectionEditorState,
            activeArticleState,
        ) { savedStories, stats, noteEditor, reflectionEditor, activeArticle ->
            ArticleDetailUiState(
                activeArticle = activeArticle,
                likedStoryIds = savedStories.mapTo(linkedSetOf()) { story -> story.id },
                readingStats = stats,
                noteArticleId = noteEditor.articleId,
                noteDraft = noteEditor.draft,
                hasSavedNote = noteEditor.hasSavedNote,
                isNoteLoading = noteEditor.isLoading,
                noteStatusMessage = noteEditor.statusMessage,
                reflectionArticleId = reflectionEditor.articleId,
                reflection = reflectionEditor.reflection,
                isReflectionLoading = reflectionEditor.isLoading,
                reflectionStatusMessage = reflectionEditor.statusMessage,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue =
                ArticleDetailUiState(
                    activeArticle = activeArticleState.value,
                ),
        )

    fun onArticleShown(article: Article) {
        activeArticleState.value = article
        savedStateHandle[ActiveArticleStateKey] =
            ArticleStateCodec.encode(
                ArticlesResponse(
                    articles = listOf(article),
                    nextPage = null,
                ),
            )
        loadNoteEditor(article)
        loadReflectionEditor(article)
        if (!recordedStoryIds.add(article.stableId.value)) return
        savedStateHandle[RecordedStoryIdsStateKey] = ArrayList(recordedStoryIds)
        viewModelScope.launch {
            readingStatsRepository.recordStoryOpen(article)
            widgetRefreshRequester.requestRefresh()
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
        persistNoteDraft()
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
                persistNoteDraft()
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
                clearSavedNoteDraft()
                showNoteStatus(
                    articleId = article.stableId,
                    message = NoteClearedMessage,
                )
            }
        }
    }

    fun saveReflection(
        article: Article,
        reaction: StoryReflectionReaction,
    ) {
        viewModelScope.launch {
            reflectionMutex.withLock {
                if (
                    reflectionEditorState.value.articleId != article.stableId ||
                    activeArticle?.stableId != article.stableId
                ) {
                    return@withLock
                }
                storyReflectionRepository.setReaction(article, reaction)
                showReflectionStatus(
                    articleId = article.stableId,
                    message = "Saved: ${reaction.title}",
                )
            }
        }
    }

    private fun loadNoteEditor(article: Article) {
        activeArticle = article
        noteObservationJob?.cancel()
        noteStatusJob?.cancel()
        val restoredDraft =
            savedStateHandle
                .get<String>(NoteDraftStateKey)
                ?.takeIf {
                    savedStateHandle.get<String>(NoteArticleIdStateKey) ==
                        article.stableId.value
                }
        if (
            savedStateHandle.get<String>(NoteArticleIdStateKey) != null &&
            restoredDraft == null
        ) {
            clearSavedNoteDraft()
        }
        noteEditorState.value =
            ArticleNoteEditorState(
                articleId = article.stableId,
                draft = restoredDraft.orEmpty(),
                isDirty = restoredDraft != null,
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

    private fun loadReflectionEditor(article: Article) {
        reflectionObservationJob?.cancel()
        reflectionStatusJob?.cancel()
        reflectionEditorState.value =
            ArticleReflectionEditorState(
                articleId = article.stableId,
                isLoading = true,
            )
        reflectionObservationJob =
            viewModelScope.launch {
                storyReflectionRepository.observeReflection(article).collect { reflection ->
                    reflectionEditorState.update { current ->
                        if (current.articleId == article.stableId) {
                            current.copy(
                                reflection = reflection,
                                isLoading = false,
                            )
                        } else {
                            current
                        }
                    }
                }
            }
    }

    private fun showReflectionStatus(
        articleId: StoryId,
        message: String,
    ) {
        reflectionStatusJob?.cancel()
        reflectionEditorState.update { current ->
            if (current.articleId == articleId) {
                current.copy(statusMessage = message)
            } else {
                current
            }
        }
        reflectionStatusJob =
            viewModelScope.launch {
                delay(ReflectionStatusDurationMillis)
                reflectionEditorState.update { current ->
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
        private val storyReflectionRepository: StoryReflectionRepository,
        private val widgetRefreshRequester: WidgetRefreshRequester =
            NoOpWidgetRefreshRequester,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ArticleDetailViewModel::class.java))
            return ArticleDetailViewModel(
                savedStoryRepository = savedStoryRepository,
                readingStatsRepository = readingStatsRepository,
                storyNoteRepository = storyNoteRepository,
                storyReflectionRepository = storyReflectionRepository,
                widgetRefreshRequester = widgetRefreshRequester,
            ) as T
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras,
        ): T {
            require(modelClass.isAssignableFrom(ArticleDetailViewModel::class.java))
            return ArticleDetailViewModel(
                savedStoryRepository = savedStoryRepository,
                readingStatsRepository = readingStatsRepository,
                storyNoteRepository = storyNoteRepository,
                storyReflectionRepository = storyReflectionRepository,
                widgetRefreshRequester = widgetRefreshRequester,
                savedStateHandle = extras.createSavedStateHandle(),
            ) as T
        }
    }

    private fun persistNoteDraft() {
        val editor = noteEditorState.value
        val articleId = editor.articleId
        if (!editor.isDirty || articleId == null) {
            clearSavedNoteDraft()
            return
        }
        savedStateHandle[NoteArticleIdStateKey] = articleId.value
        savedStateHandle[NoteDraftStateKey] = editor.draft
    }

    private fun clearSavedNoteDraft() {
        savedStateHandle.remove<String>(NoteArticleIdStateKey)
        savedStateHandle.remove<String>(NoteDraftStateKey)
    }
}

internal const val NoteArticleIdStateKey = "articleDetail.noteArticleId"
internal const val NoteDraftStateKey = "articleDetail.noteDraft"
internal const val RecordedStoryIdsStateKey = "articleDetail.recordedStoryIds"
internal const val ActiveArticleStateKey = "articleDetail.activeArticle"

@Immutable
private data class ArticleNoteEditorState(
    val articleId: StoryId? = null,
    val draft: String = "",
    val hasSavedNote: Boolean = false,
    val isDirty: Boolean = false,
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
)

@Immutable
private data class ArticleReflectionEditorState(
    val articleId: StoryId? = null,
    val reflection: StoryReflection? = null,
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
)

private const val NoteSavedMessage = "Note saved on this device"
private const val NoteClearedMessage = "Note cleared"
private const val NoteStatusDurationMillis = 1_800L
private const val ReflectionStatusDurationMillis = 1_800L
