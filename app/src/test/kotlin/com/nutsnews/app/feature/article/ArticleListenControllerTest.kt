package com.nutsnews.app.feature.article

import com.nutsnews.app.core.model.Article
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class ArticleListenControllerTest {
    @Test
    fun scriptMatchesTheStructuredIosListeningBrief() {
        val article = listenArticle()
        val brief = deriveArticleBrief(article)

        val script = buildArticleListenScript(article, brief)

        assertEquals(
            listOf(
                "Intro" to "Here’s your NutsNews brief.",
                "Story" to article.title,
                "What happened" to "What happened: ${brief.whatHappened}",
                "Why it’s good news" to "Why it’s good news: ${brief.whyGoodNews}",
                "Takeaway" to "The feel-good takeaway: ${brief.takeaway}",
                "Source" to "Source: ${article.source}.",
            ),
            script.segments.map { it.label to it.text },
        )
        assertEquals(6, script.text.lines().size)
    }

    @Test
    fun playPauseResumeProgressAndCompletionFollowTheCurrentSegment() {
        val engine = FakeArticleSpeechEngine()
        val controller = ArticleListenController(engine)
        val script = buildArticleListenScript(listenArticle())

        controller.toggle(script)

        assertEquals(ArticleListenPlaybackState.Reading, controller.uiState.value.playbackState)
        assertEquals(listOf(0), engine.spokenStartIndices)
        assertEquals(script.segments, controller.uiState.value.segments)

        engine.listener.onSegmentStarted(2)
        val waveSeedBeforeRange = controller.uiState.value.speechWaveSeed
        val spokenText = script.segments[2].text
        engine.listener.onRangeStarted(
            index = 2,
            text = spokenText,
            start = 0,
            end = "What".length,
        )

        assertEquals(2, controller.uiState.value.currentSegmentIndex)
        assertTrue(controller.uiState.value.speechWaveSeed > waveSeedBeforeRange)
        assertTrue(controller.uiState.value.speechWaveLevel > 0.18f)

        controller.toggle(script)

        assertEquals(ArticleListenPlaybackState.Paused, controller.uiState.value.playbackState)
        assertEquals(1, engine.stopCount)
        assertEquals(2, controller.uiState.value.currentSegmentIndex)

        controller.toggle(script)

        assertEquals(ArticleListenPlaybackState.Reading, controller.uiState.value.playbackState)
        assertEquals(listOf(0, 2), engine.spokenStartIndices)

        engine.listener.onSegmentFinished(script.segments.lastIndex)

        assertEquals(ArticleListenPlaybackState.Idle, controller.uiState.value.playbackState)
        assertEquals("Finished reading", controller.uiState.value.statusMessage)
        assertNull(controller.uiState.value.currentSegmentIndex)
    }

    @Test
    fun stopAndShutdownCancelSpeechAndClearActiveState() {
        val engine = FakeArticleSpeechEngine()
        val controller = ArticleListenController(engine)
        val script = buildArticleListenScript(listenArticle())
        controller.toggle(script)

        controller.stop()

        assertEquals(ArticleListenPlaybackState.Idle, controller.uiState.value.playbackState)
        assertEquals("Stopped", controller.uiState.value.statusMessage)
        assertFalse(controller.uiState.value.isActive)
        assertNull(controller.uiState.value.currentSegmentIndex)
        assertEquals(1, engine.stopCount)

        controller.shutdown()

        assertEquals(2, engine.stopCount)
        assertEquals(1, engine.shutdownCount)
        assertFalse(controller.uiState.value.isEngineReady)
    }

    @Test
    fun unavailableEngineShowsFailureAndCanRetryWhenItBecomesAvailable() {
        val engine =
            FakeArticleSpeechEngine(
                initializationResult =
                    Result.failure(IllegalStateException("No installed voice")),
            )
        val controller = ArticleListenController(engine)
        val script = buildArticleListenScript(listenArticle())

        assertEquals(ArticleListenPlaybackState.Failed, controller.uiState.value.playbackState)
        assertEquals(
            "On-device TextToSpeech is unavailable",
            controller.uiState.value.statusMessage,
        )

        engine.initializationResult = Result.success("Test voice")
        controller.toggle(script)

        assertEquals(2, engine.initializeCount)
        assertEquals(ArticleListenPlaybackState.Reading, controller.uiState.value.playbackState)
        assertEquals(listOf(0), engine.spokenStartIndices)
        assertTrue(controller.uiState.value.isEngineReady)
    }

    @Test
    fun queueAndRuntimeFailuresBecomeActionableFailedStates() {
        val engine = FakeArticleSpeechEngine(speakSucceeds = false)
        val controller = ArticleListenController(engine)
        val script = buildArticleListenScript(listenArticle())

        controller.toggle(script)

        assertEquals(ArticleListenPlaybackState.Failed, controller.uiState.value.playbackState)
        assertEquals(
            "Text-to-speech could not read this brief",
            controller.uiState.value.statusMessage,
        )

        engine.speakSucceeds = true
        controller.toggle(script)
        engine.listener.onError()

        assertEquals(ArticleListenPlaybackState.Failed, controller.uiState.value.playbackState)
        assertFalse(controller.uiState.value.isActive)
        assertNull(controller.uiState.value.currentSegmentIndex)
    }

    @Test
    fun emptyScriptDoesNotAskTheSpeechEngineToRead() {
        val engine = FakeArticleSpeechEngine()
        val controller = ArticleListenController(engine)

        controller.toggle(ArticleListenScript(emptyList()))

        assertEquals(ArticleListenPlaybackState.Idle, controller.uiState.value.playbackState)
        assertEquals("Nothing to read yet", controller.uiState.value.statusMessage)
        assertTrue(engine.spokenStartIndices.isEmpty())
    }
}

private class FakeArticleSpeechEngine(
    var initializationResult: Result<String> = Result.success("Test voice"),
    var speakSucceeds: Boolean = true,
) : ArticleSpeechEngine {
    lateinit var listener: ArticleSpeechEngine.Listener
    var initializeCount = 0
    var stopCount = 0
    var shutdownCount = 0
    val spokenStartIndices = mutableListOf<Int>()

    override fun initialize(
        listener: ArticleSpeechEngine.Listener,
        onResult: (Result<String>) -> Unit,
    ) {
        this.listener = listener
        initializeCount += 1
        onResult(initializationResult)
    }

    override fun speak(
        segments: List<ArticleListenSegment>,
        startIndex: Int,
    ): Boolean {
        spokenStartIndices += startIndex
        return speakSucceeds
    }

    override fun stop() {
        stopCount += 1
    }

    override fun shutdown() {
        shutdownCount += 1
    }
}

private fun listenArticle(): Article =
    Article(
        id = "listen-story",
        title = "Researchers discover a practical clean-energy breakthrough",
        summary =
            "A university research team found a promising way to store renewable energy.",
        originalUrl = URI("https://example.com/listen-story"),
        source = "Positive Science Daily",
        publishedAt = "2026-07-26T12:00:00Z",
        createdAt = null,
        thumbnailUrl = null,
        categories = listOf("Science", "Discovery"),
    )
