package com.nutsnews.app.feature.article

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.nutsnews.app.core.model.Article
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PositiveShareCardRendererTest {
    @Test
    fun goldenCardIsDeterministicAndUsesTheIosResolutionAndBrandPalette() {
        val article = shareArticle()

        val first =
            PositiveShareCardRenderer.render(
                article = article,
                whyGood =
                    "It highlights progress and curiosity, showing how discovery can make " +
                        "the world feel more hopeful.",
                takeaway = "Progress is still happening, one discovery at a time.",
                moodLabel = "Curious",
            )
        val second =
            PositiveShareCardRenderer.render(
                article = article,
                whyGood =
                    "It highlights progress and curiosity, showing how discovery can make " +
                        "the world feel more hopeful.",
                takeaway = "Progress is still happening, one discovery at a time.",
                moodLabel = "Curious",
            )

        assertEquals(1_080, first.width)
        assertEquals(1_350, first.height)
        assertEquals(Bitmap.Config.ARGB_8888, first.config)
        assertTrue(first.sameAs(second), "The golden render must be pixel deterministic")
        assertEquals(PositiveShareCardRenderer.WarmAmber, first.getPixel(115, 90))
        assertTrue(sampledShareCardColors(first) >= 28)
    }

    @Test
    fun thumbnailIsCenterCroppedBehindADarkReadabilityTreatment() {
        val thumbnail =
            Bitmap
                .createBitmap(900, 300, Bitmap.Config.ARGB_8888)
                .apply {
                    eraseColor(Color.rgb(30, 210, 100))
                }
        val withoutThumbnail =
            PositiveShareCardRenderer.render(
                article = shareArticle(),
                whyGood = "A positive step forward.",
                takeaway = "Progress is worth celebrating.",
                moodLabel = "Curious",
            )
        val withThumbnail =
            PositiveShareCardRenderer.render(
                article = shareArticle(),
                whyGood = "A positive step forward.",
                takeaway = "Progress is worth celebrating.",
                moodLabel = "Curious",
                thumbnail = thumbnail,
            )

        val treatedPixel = withThumbnail.getPixel(10, 390)
        assertNotEquals(withoutThumbnail.getPixel(10, 390), treatedPixel)
        assertTrue(Color.green(treatedPixel) < 210, "The thumbnail should be visibly darkened")
        assertEquals(
            withoutThumbnail.getPixel(540, 500),
            withThumbnail.getPixel(540, 500),
            "Thumbnail treatment must remain within its fixed header region",
        )
    }

    @Test
    fun shareTextMatchesIosAndIncludesTheOriginalStoryUrlWhenAvailable() {
        val article = shareArticle()

        assertEquals(
            """
            A good-news moment from NutsNews:
            ${article.title}
            Takeaway: Progress is still happening, one discovery at a time.
            Source: ${article.source}
            ${article.originalUrl}
            """.trimIndent(),
            buildPositiveShareText(
                article = article,
                takeaway = "Progress is still happening, one discovery at a time.",
            ),
        )
        assertFalse(
            buildPositiveShareText(
                article = article.copy(originalUrl = null),
                takeaway = "A brighter moment.",
            ).contains("null"),
        )
    }

    @Test
    fun fileProviderUriOpensTheExactPngAndStaysInsideTheShareCache() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val bitmap =
            PositiveShareCardRenderer.render(
                article = shareArticle(),
                whyGood = "A positive step forward.",
                takeaway = "Progress is worth celebrating.",
                moodLabel = "Curious",
            )

        val uri = ArticleShareCardFileStore(context).write(shareArticle(), bitmap)

        assertEquals("content", uri.scheme)
        assertEquals("${context.packageName}.fileprovider", uri.authority)
        assertTrue(uri.path.orEmpty().startsWith("/share_cards/"))
        context.contentResolver.openInputStream(uri).use { input ->
            checkNotNull(input)
            val decoded = android.graphics.BitmapFactory.decodeStream(input)
            assertEquals(PositiveShareCardRenderer.Width, decoded.width)
            assertEquals(PositiveShareCardRenderer.Height, decoded.height)
        }
    }

    @Test
    fun androidSendIntentGrantsReadAccessToTheImageAndCarriesText() {
        val activity =
            Robolectric
                .buildActivity(Activity::class.java)
                .setup()
                .get()
        val imageUri =
            Uri.parse(
                "content://com.nutsnews.app.fileprovider/share_cards/nutsnews-test.png",
            )
        val shareText = "A good-news moment from NutsNews"

        val intent =
            AndroidArticleShareLauncher(activity)
                .createSendIntent(imageUri, shareText)

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("image/png", intent.type)
        assertEquals(imageUri, intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
        assertEquals(shareText, intent.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals(imageUri, intent.clipData?.getItemAt(0)?.uri)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    private fun sampledShareCardColors(bitmap: Bitmap): Int =
        buildSet {
            for (y in 0 until bitmap.height step 24) {
                for (x in 0 until bitmap.width step 24) {
                    add(bitmap.getPixel(x, y))
                }
            }
        }.size
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class ArticleShareCardControllerTest {
    @Test
    fun creationPublishesLoadingLaunchesOnceAndReturnsToIdle() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val creator = RecordingSharePackageCreator()
            val launcher = RecordingShareLauncher()
            val controller =
                ArticleShareCardController(
                    scope = this,
                    packageCreator = creator,
                    shareLauncher = launcher,
                    workDispatcher = dispatcher,
                )
            val article = shareArticle()

            controller.share(article)
            controller.share(article)

            assertTrue(controller.uiState.value.isCreating)
            advanceUntilIdle()
            assertEquals(listOf(article), creator.createdArticles)
            assertEquals(listOf(creator.sharePackage), launcher.launchedPackages)
            assertEquals(ArticleShareCardUiState(), controller.uiState.value)
        }

    @Test
    fun creatingGlowRemainsVisibleForTheIosResetWindowAfterSharesheetLaunch() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val launcher = RecordingShareLauncher()
            val controller =
                ArticleShareCardController(
                    scope = this,
                    packageCreator = RecordingSharePackageCreator(),
                    shareLauncher = launcher,
                    workDispatcher = dispatcher,
                )

            controller.share(shareArticle())
            runCurrent()

            assertEquals(1, launcher.launchedPackages.size)
            assertTrue(controller.uiState.value.isCreating)
            advanceTimeBy(799)
            assertTrue(controller.uiState.value.isCreating)
            advanceTimeBy(1)
            runCurrent()
            assertEquals(ArticleShareCardUiState(), controller.uiState.value)
        }

    @Test
    fun creationOrSharesheetFailureShowsRetryableFailureAndCancelCleansUp() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val creator = RecordingSharePackageCreator(fail = true)
            val controller =
                ArticleShareCardController(
                    scope = this,
                    packageCreator = creator,
                    shareLauncher = RecordingShareLauncher(),
                    workDispatcher = dispatcher,
                )

            controller.share(shareArticle())
            advanceUntilIdle()

            assertFalse(controller.uiState.value.isCreating)
            assertEquals(
                "The positive share card couldn’t be created. Please try again.",
                controller.uiState.value.failureMessage,
            )
            controller.clearFailure()
            assertEquals(ArticleShareCardUiState(), controller.uiState.value)

            controller.share(shareArticle())
            assertTrue(controller.uiState.value.isCreating)
            controller.cancel()
            advanceUntilIdle()
            assertEquals(ArticleShareCardUiState(), controller.uiState.value)
        }
}

private class RecordingSharePackageCreator(
    private val fail: Boolean = false,
) : ArticleSharePackageCreator {
    val sharePackage =
        ArticleSharePackage(
            imageUri =
                Uri.parse(
                    "content://com.nutsnews.app.fileprovider/share_cards/nutsnews-test.png",
                ),
            shareText = "A good-news moment from NutsNews",
        )
    val createdArticles = mutableListOf<Article>()

    override suspend fun create(article: Article): ArticleSharePackage {
        createdArticles += article
        if (fail) error("Renderer failed")
        return sharePackage
    }
}

private class RecordingShareLauncher : ArticleShareLauncher {
    val launchedPackages = mutableListOf<ArticleSharePackage>()

    override fun launch(sharePackage: ArticleSharePackage) {
        launchedPackages += sharePackage
    }
}

private fun shareArticle(): Article =
    Article(
        id = "share-story",
        title = "Researchers discover a practical clean-energy breakthrough",
        summary =
            "A university research team found a promising way to store renewable energy.",
        originalUrl = URI("https://example.com/share-story"),
        source = "Positive Science Daily",
        publishedAt = "Published today",
        createdAt = null,
        thumbnailUrl = URI("https://example.com/share-story.jpg"),
        categories = listOf("Science", "Discovery"),
    )
