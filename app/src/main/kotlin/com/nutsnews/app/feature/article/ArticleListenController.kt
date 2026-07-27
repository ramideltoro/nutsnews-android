package com.nutsnews.app.feature.article

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Immutable
import com.nutsnews.app.core.model.Article
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Immutable
data class ArticleListenSegment(
    val id: String,
    val label: String,
    val text: String,
)

@Immutable
data class ArticleListenScript(
    val segments: List<ArticleListenSegment>,
) {
    val text: String
        get() = segments.joinToString("\n", transform = ArticleListenSegment::text)
}

enum class ArticleListenPlaybackState {
    Idle,
    Reading,
    Paused,
    Failed,
}

@Immutable
data class ArticleListenUiState(
    val playbackState: ArticleListenPlaybackState = ArticleListenPlaybackState.Idle,
    val statusMessage: String = "Preparing on-device voice",
    val segments: List<ArticleListenSegment> = emptyList(),
    val currentSegmentIndex: Int? = null,
    val speechWaveLevel: Float = 0.18f,
    val speechWaveFrequency: Float = 0.9f,
    val speechWaveSeed: Long = 0,
    val isEngineReady: Boolean = false,
) {
    val isActive: Boolean
        get() =
            playbackState == ArticleListenPlaybackState.Reading ||
                playbackState == ArticleListenPlaybackState.Paused

    val primaryButtonTitle: String
        get() =
            when (playbackState) {
                ArticleListenPlaybackState.Reading -> "Pause"
                ArticleListenPlaybackState.Paused -> "Resume"
                ArticleListenPlaybackState.Failed -> "Try again"
                ArticleListenPlaybackState.Idle -> "Listen to brief"
            }

    val shortStatusMessage: String
        get() =
            when (playbackState) {
                ArticleListenPlaybackState.Reading -> "Reading"
                ArticleListenPlaybackState.Paused -> "Paused"
                ArticleListenPlaybackState.Failed -> "Unavailable"
                ArticleListenPlaybackState.Idle -> "Ready"
            }
}

interface ArticleSpeechEngine {
    fun initialize(
        listener: Listener,
        onResult: (Result<String>) -> Unit,
    )

    fun speak(
        segments: List<ArticleListenSegment>,
        startIndex: Int,
    ): Boolean

    fun stop()

    fun shutdown()

    interface Listener {
        fun onSegmentStarted(index: Int)

        fun onRangeStarted(
            index: Int,
            text: String,
            start: Int,
            end: Int,
        )

        fun onSegmentFinished(index: Int)

        fun onStopped()

        fun onError()
    }
}

class ArticleListenController(
    private val engine: ArticleSpeechEngine,
) : ArticleSpeechEngine.Listener {
    private val mutableUiState = MutableStateFlow(ArticleListenUiState())
    val uiState: StateFlow<ArticleListenUiState> = mutableUiState.asStateFlow()

    private var engineReady = false
    private var initializing = false
    private var pendingScript: ArticleListenScript? = null

    init {
        initializeEngine()
    }

    fun toggle(script: ArticleListenScript) {
        when (mutableUiState.value.playbackState) {
            ArticleListenPlaybackState.Reading -> pause()
            ArticleListenPlaybackState.Paused -> resume()
            ArticleListenPlaybackState.Idle,
            ArticleListenPlaybackState.Failed,
            -> start(script)
        }
    }

    fun stop() {
        pendingScript = null
        engine.stop()
        mutableUiState.update { current ->
            current.copy(
                playbackState = ArticleListenPlaybackState.Idle,
                statusMessage = "Stopped",
                currentSegmentIndex = null,
                speechWaveLevel = 0.18f,
                speechWaveFrequency = 0.9f,
            )
        }
    }

    fun shutdown() {
        pendingScript = null
        engine.stop()
        engine.shutdown()
        engineReady = false
        mutableUiState.value = ArticleListenUiState(statusMessage = "Stopped")
    }

    private fun start(script: ArticleListenScript) {
        if (script.segments.isEmpty()) {
            mutableUiState.update {
                it.copy(
                    playbackState = ArticleListenPlaybackState.Idle,
                    statusMessage = "Nothing to read yet",
                    segments = emptyList(),
                    currentSegmentIndex = null,
                )
            }
            return
        }
        mutableUiState.update {
            it.copy(
                segments = script.segments,
                currentSegmentIndex = 0,
            )
        }
        if (!engineReady) {
            pendingScript = script
            mutableUiState.update {
                it.copy(
                    playbackState = ArticleListenPlaybackState.Idle,
                    statusMessage = "Preparing on-device voice",
                )
            }
            if (!initializing) initializeEngine()
            return
        }
        queueFrom(script, startIndex = 0)
    }

    private fun pause() {
        engine.stop()
        mutableUiState.update {
            it.copy(
                playbackState = ArticleListenPlaybackState.Paused,
                statusMessage = "Paused",
                speechWaveLevel = 0.18f,
                speechWaveFrequency = 0.9f,
            )
        }
    }

    private fun resume() {
        val current = mutableUiState.value
        val startIndex = current.currentSegmentIndex ?: 0
        val script = ArticleListenScript(current.segments)
        queueFrom(script, startIndex)
    }

    private fun queueFrom(
        script: ArticleListenScript,
        startIndex: Int,
    ) {
        val started = engine.speak(script.segments, startIndex)
        if (!started) {
            mutableUiState.update {
                it.copy(
                    playbackState = ArticleListenPlaybackState.Failed,
                    statusMessage = "Text-to-speech could not read this brief",
                    speechWaveLevel = 0.18f,
                    speechWaveFrequency = 0.9f,
                )
            }
            return
        }
        mutableUiState.update {
            it.copy(
                playbackState = ArticleListenPlaybackState.Reading,
                statusMessage = "Reading with on-device voice",
                segments = script.segments,
                currentSegmentIndex = startIndex,
                speechWaveLevel = 0.28f,
                speechWaveFrequency = 1.05f,
                speechWaveSeed = it.speechWaveSeed + 1,
            )
        }
    }

    private fun initializeEngine() {
        initializing = true
        engine.initialize(this) { result ->
            initializing = false
            result.fold(
                onSuccess = { voiceName ->
                    engineReady = true
                    mutableUiState.update {
                        it.copy(
                            statusMessage = "Ready to listen",
                            isEngineReady = true,
                        )
                    }
                    pendingScript?.let { script ->
                        pendingScript = null
                        queueFrom(script, startIndex = 0)
                    }
                },
                onFailure = {
                    engineReady = false
                    pendingScript = null
                    mutableUiState.update {
                        it.copy(
                            playbackState = ArticleListenPlaybackState.Failed,
                            statusMessage = "On-device TextToSpeech is unavailable",
                            currentSegmentIndex = null,
                            isEngineReady = false,
                        )
                    }
                },
            )
        }
    }

    override fun onSegmentStarted(index: Int) {
        mutableUiState.update { current ->
            if (current.playbackState == ArticleListenPlaybackState.Reading) {
                current.copy(currentSegmentIndex = index)
            } else {
                current
            }
        }
    }

    override fun onRangeStarted(
        index: Int,
        text: String,
        start: Int,
        end: Int,
    ) {
        if (mutableUiState.value.playbackState != ArticleListenPlaybackState.Reading) return
        val spokenWord =
            text
                .substring(
                    start.coerceIn(0, text.length),
                    end.coerceIn(start.coerceAtMost(text.length), text.length),
                ).trim()
        if (spokenWord.isEmpty()) return
        val normalizedWord = spokenWord.lowercase(Locale.ROOT)
        val vowelCount = normalizedWord.count { it in "aeiou" }
        val consonantCount = normalizedWord.count { it.isLetter() && it !in "aeiou" }
        val punctuationBoost = if (spokenWord.any(Char::isLetterOrDigit)) 0f else 0.12f
        val lengthBoost = (spokenWord.length * 0.032f).coerceAtMost(0.34f)
        val vowelBoost = (vowelCount * 0.042f).coerceAtMost(0.22f)
        val consonantTexture = (consonantCount * 0.026f).coerceAtMost(0.24f)
        val cadence = vowelCount.coerceAtLeast(1) / spokenWord.length.coerceAtLeast(2).toFloat()
        mutableUiState.update {
            it.copy(
                currentSegmentIndex = index,
                speechWaveLevel =
                    (0.26f + lengthBoost + vowelBoost + punctuationBoost)
                        .coerceAtMost(1f),
                speechWaveFrequency =
                    (0.9f + cadence * 3.1f + consonantTexture + punctuationBoost)
                        .coerceIn(0.75f, 2.2f),
                speechWaveSeed = it.speechWaveSeed + 1,
            )
        }
    }

    override fun onSegmentFinished(index: Int) {
        val current = mutableUiState.value
        if (
            current.playbackState == ArticleListenPlaybackState.Reading &&
            index >= current.segments.lastIndex
        ) {
            mutableUiState.update {
                it.copy(
                    playbackState = ArticleListenPlaybackState.Idle,
                    statusMessage = "Finished reading",
                    currentSegmentIndex = null,
                    speechWaveLevel = 0.18f,
                    speechWaveFrequency = 0.9f,
                )
            }
        }
    }

    override fun onStopped() = Unit

    override fun onError() {
        mutableUiState.update {
            it.copy(
                playbackState = ArticleListenPlaybackState.Failed,
                statusMessage = "Text-to-speech could not read this brief",
                currentSegmentIndex = null,
                speechWaveLevel = 0.18f,
                speechWaveFrequency = 0.9f,
            )
        }
    }
}

class AndroidArticleSpeechEngine(
    context: Context,
) : ArticleSpeechEngine {
    private val applicationContext = context.applicationContext
    private var textToSpeech: TextToSpeech? = null
    private var listener: ArticleSpeechEngine.Listener? = null
    private var onInitializationResult: ((Result<String>) -> Unit)? = null

    override fun initialize(
        listener: ArticleSpeechEngine.Listener,
        onResult: (Result<String>) -> Unit,
    ) {
        this.listener = listener
        onInitializationResult = onResult
        textToSpeech?.shutdown()
        textToSpeech =
            try {
                TextToSpeech(applicationContext) { status ->
                    val current = textToSpeech
                    if (status != TextToSpeech.SUCCESS || current == null) {
                        finishInitialization(Result.failure(IllegalStateException("TTS init failed")))
                        return@TextToSpeech
                    }
                    val languageResult = current.setLanguage(Locale.US)
                    if (
                        languageResult == TextToSpeech.LANG_MISSING_DATA ||
                        languageResult == TextToSpeech.LANG_NOT_SUPPORTED
                    ) {
                        finishInitialization(
                            Result.failure(IllegalStateException("English TTS unavailable")),
                        )
                        return@TextToSpeech
                    }
                    val voice =
                        current.voices
                            ?.asSequence()
                            ?.filter { voice -> voice.locale.language == Locale.ENGLISH.language }
                            ?.sortedWith(
                                compareByDescending<android.speech.tts.Voice> { it.quality }
                                    .thenByDescending { it.locale == Locale.US }
                                    .thenBy { it.name },
                            )?.firstOrNull()
                    if (voice != null) current.voice = voice
                    current.setSpeechRate(0.9f)
                    current.setPitch(0.96f)
                    current.setAudioAttributes(
                        AudioAttributes
                            .Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build(),
                    )
                    current.setOnUtteranceProgressListener(progressListener)
                    finishInitialization(Result.success(voice?.name ?: "Android voice"))
                }
            } catch (failure: Exception) {
                finishInitialization(Result.failure(failure))
                null
            }
    }

    override fun speak(
        segments: List<ArticleListenSegment>,
        startIndex: Int,
    ): Boolean {
        val current = textToSpeech ?: return false
        if (startIndex !in segments.indices) return false
        lastSegments = segments
        var result = TextToSpeech.SUCCESS
        for (index in startIndex..segments.lastIndex) {
            val segment = segments[index]
            if (index > startIndex) {
                current.playSilentUtterance(
                    pauseBeforeSegment(segment.text),
                    TextToSpeech.QUEUE_ADD,
                    "pause-$index",
                )
            }
            val queueMode = if (index == startIndex) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val params =
                Bundle().apply {
                    putString(
                        TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                        utteranceId(index),
                    )
                }
            val speakResult =
                current.speak(
                    spokenFriendlyText(segment.text),
                    queueMode,
                    params,
                    utteranceId(index),
                )
            if (speakResult == TextToSpeech.ERROR) result = TextToSpeech.ERROR
        }
        return result == TextToSpeech.SUCCESS
    }

    override fun stop() {
        textToSpeech?.stop()
    }

    override fun shutdown() {
        textToSpeech?.shutdown()
        textToSpeech = null
        listener = null
    }

    private fun finishInitialization(result: Result<String>) {
        onInitializationResult?.invoke(result)
        onInitializationResult = null
    }

    private val progressListener =
        object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                utteranceIndex(utteranceId)?.let { listener?.onSegmentStarted(it) }
            }

            override fun onDone(utteranceId: String) {
                utteranceIndex(utteranceId)?.let { listener?.onSegmentFinished(it) }
            }

            @Deprecated("Deprecated by Android")
            override fun onError(utteranceId: String) {
                if (utteranceIndex(utteranceId) != null) listener?.onError()
            }

            override fun onError(
                utteranceId: String,
                errorCode: Int,
            ) {
                if (utteranceIndex(utteranceId) != null) listener?.onError()
            }

            override fun onStop(
                utteranceId: String,
                interrupted: Boolean,
            ) {
                if (utteranceIndex(utteranceId) != null) listener?.onStopped()
            }

            override fun onRangeStart(
                utteranceId: String,
                start: Int,
                end: Int,
                frame: Int,
            ) {
                val index = utteranceIndex(utteranceId) ?: return
                val segment = lastSegments.getOrNull(index) ?: return
                listener?.onRangeStarted(
                    index = index,
                    text = spokenFriendlyText(segment.text),
                    start = start,
                    end = end,
                )
            }
        }

    private var lastSegments: List<ArticleListenSegment> = emptyList()

    private fun utteranceId(index: Int): String = "nutsnews-segment-$index"

    private fun utteranceIndex(id: String): Int? =
        id.removePrefix("nutsnews-segment-")
            .takeIf { id.startsWith("nutsnews-segment-") }
            ?.toIntOrNull()

    private fun pauseBeforeSegment(text: String): Long =
        when {
            text.contains("what happened", ignoreCase = true) ||
                text.contains("why it", ignoreCase = true) ||
                text.contains("takeaway", ignoreCase = true) ||
                text.contains("source", ignoreCase = true) -> 340L

            else -> 200L
        }

    private fun spokenFriendlyText(text: String): String =
        text
            .replace("NutsNews", "Nuts News")
            .replace("AI", "A I")
            .replace("iOS", "I O S")
            .replace("&", "and")
            .replace(" — ", ", ")
            .replace(" – ", ", ")
}

internal fun buildArticleListenScript(
    article: Article,
    brief: ArticleBriefContent = deriveArticleBrief(article),
): ArticleListenScript =
    ArticleListenScript(
        segments =
            listOf(
                ArticleListenSegment(
                    id = "intro",
                    label = "Intro",
                    text = "Here’s your NutsNews brief.",
                ),
                ArticleListenSegment(
                    id = "story",
                    label = "Story",
                    text = article.title.trim(),
                ),
                ArticleListenSegment(
                    id = "what_happened",
                    label = "What happened",
                    text = "What happened: ${brief.whatHappened}",
                ),
                ArticleListenSegment(
                    id = "why_good",
                    label = "Why it’s good news",
                    text = "Why it’s good news: ${brief.whyGoodNews}",
                ),
                ArticleListenSegment(
                    id = "takeaway",
                    label = "Takeaway",
                    text = "The feel-good takeaway: ${brief.takeaway}",
                ),
                ArticleListenSegment(
                    id = "source",
                    label = "Source",
                    text = "Source: ${article.source.trim()}.",
                ),
            ).filter { it.text.substringAfter(':', it.text).trim().isNotEmpty() },
    )
