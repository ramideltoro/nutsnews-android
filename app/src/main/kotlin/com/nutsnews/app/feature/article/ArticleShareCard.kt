package com.nutsnews.app.feature.article

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.ColorInt
import androidx.compose.runtime.Immutable
import androidx.core.content.FileProvider
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.designsystem.NutsNewsMotion
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.ceil
import kotlin.math.max
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class ArticleShareCardUiState(
    val isCreating: Boolean = false,
    val failureMessage: String? = null,
)

data class ArticleSharePackage(
    val imageUri: Uri,
    val shareText: String,
)

interface ArticleSharePackageCreator {
    suspend fun create(article: Article): ArticleSharePackage
}

interface ArticleShareLauncher {
    fun launch(sharePackage: ArticleSharePackage)
}

class ArticleShareCardController(
    private val scope: CoroutineScope,
    private val packageCreator: ArticleSharePackageCreator,
    private val shareLauncher: ArticleShareLauncher,
    private val workDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutableUiState = MutableStateFlow(ArticleShareCardUiState())
    val uiState: StateFlow<ArticleShareCardUiState> = mutableUiState.asStateFlow()

    private var creationJob: Job? = null

    fun share(article: Article) {
        if (mutableUiState.value.isCreating) return

        mutableUiState.value = ArticleShareCardUiState(isCreating = true)
        creationJob =
            scope.launch {
                try {
                    val sharePackage =
                        withContext(workDispatcher) {
                            packageCreator.create(article)
                    }
                    shareLauncher.launch(sharePackage)
                    delay(NutsNewsMotion.ShareCreatingResetMillis)
                    mutableUiState.value = ArticleShareCardUiState()
                } catch (cancellation: CancellationException) {
                    mutableUiState.value = ArticleShareCardUiState()
                    throw cancellation
                } catch (_: Exception) {
                    mutableUiState.value =
                        ArticleShareCardUiState(
                            failureMessage =
                                "The positive share card couldn’t be created. Please try again.",
                        )
                }
            }
    }

    fun clearFailure() {
        if (!mutableUiState.value.isCreating) {
            mutableUiState.value = ArticleShareCardUiState()
        }
    }

    fun cancel() {
        creationJob?.cancel()
        creationJob = null
        mutableUiState.value = ArticleShareCardUiState()
    }
}

class AndroidArticleSharePackageCreator(
    context: Context,
) : ArticleSharePackageCreator {
    private val applicationContext = context.applicationContext
    private val fileStore = ArticleShareCardFileStore(applicationContext)

    override suspend fun create(article: Article): ArticleSharePackage {
        val brief = deriveArticleBrief(article)
        val thumbnail = loadThumbnail(article)
        val bitmap =
            PositiveShareCardRenderer.render(
                article = article,
                whyGood = brief.whyGoodNews,
                takeaway = brief.takeaway,
                moodLabel = brief.primaryMoodLabel,
                thumbnail = thumbnail,
            )
        return try {
            ArticleSharePackage(
                imageUri = fileStore.write(article, bitmap),
                shareText = buildPositiveShareText(article, brief.takeaway),
            )
        } finally {
            bitmap.recycle()
        }
    }

    private suspend fun loadThumbnail(article: Article): Bitmap? {
        val thumbnailUrl = article.thumbnailUrl ?: return null
        return runCatching {
            val request =
                ImageRequest
                    .Builder(applicationContext)
                    .data(thumbnailUrl.toString())
                    .size(
                        PositiveShareCardRenderer.Width,
                        PositiveShareCardRenderer.ThumbnailHeight,
                    ).allowHardware(false)
                    .build()
            val result = applicationContext.imageLoader.execute(request)
            (result as? SuccessResult)?.image?.toBitmap()
        }.getOrNull()
    }
}

class AndroidArticleShareLauncher(
    private val activity: Activity,
) : ArticleShareLauncher {
    override fun launch(sharePackage: ArticleSharePackage) {
        activity.startActivity(
            Intent.createChooser(
                createSendIntent(
                    imageUri = sharePackage.imageUri,
                    shareText = sharePackage.shareText,
                ),
                "Share good news",
            ),
        )
    }

    internal fun createSendIntent(
        imageUri: Uri,
        shareText: String,
    ): Intent =
        Intent(Intent.ACTION_SEND)
            .setType(ShareCardMimeType)
            .putExtra(Intent.EXTRA_STREAM, imageUri)
            .putExtra(Intent.EXTRA_TEXT, shareText)
            .apply {
                clipData =
                    ClipData.newUri(
                        activity.contentResolver,
                        "NutsNews positive share card",
                        imageUri,
                    )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
}

internal class ArticleShareCardFileStore(
    private val context: Context,
) {
    fun write(
        article: Article,
        bitmap: Bitmap,
    ): Uri {
        val directory = File(context.cacheDir, ShareCardDirectory)
        check(directory.exists() || directory.mkdirs()) {
            "Share-card cache directory could not be created"
        }
        val filename =
            "nutsnews-${article.stableId.value.hashCode().toUInt().toString(16)}.png"
        val output = File(directory, filename)
        FileOutputStream(output).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                "Share-card PNG could not be encoded"
            }
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            output,
        )
    }
}

internal fun buildPositiveShareText(
    article: Article,
    takeaway: String,
): String =
    buildList {
        add("A good-news moment from NutsNews:")
        add(article.title)
        add("Takeaway: $takeaway")
        add("Source: ${article.source}")
        article.originalUrl?.toString()?.let(::add)
    }.joinToString("\n")

internal object PositiveShareCardRenderer {
    const val Width = 1080
    const val Height = 1350
    const val ThumbnailHeight = 430

    @ColorInt
    internal val DarkBackground = Color.rgb(14, 11, 6)

    @ColorInt
    internal val CardSurface = Color.rgb(31, 23, 10)

    @ColorInt
    internal val WarmAmber = Color.rgb(255, 184, 41)

    @ColorInt
    internal val SoftAmber = Color.rgb(255, 222, 125)

    @ColorInt
    internal val PrimaryText = Color.rgb(255, 245, 219)

    @ColorInt
    internal val SecondaryText = Color.rgb(224, 199, 158)

    private val boldRounded = Typeface.create("sans-serif-rounded", Typeface.BOLD)

    fun render(
        article: Article,
        whyGood: String,
        takeaway: String,
        moodLabel: String,
        thumbnail: Bitmap? = null,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(Width, Height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawBackground(canvas, thumbnail)
        drawHeader(canvas)
        val titleBottom =
            drawTitleBlock(
                canvas = canvas,
                article = article,
                moodLabel = moodLabel,
            )
        val whyGoodBottom =
            drawWhyGoodBlock(
                canvas = canvas,
                whyGood = whyGood,
                top = titleBottom + 34f,
            )
        drawTakeawayBlock(
            canvas = canvas,
            takeaway = takeaway,
            top = whyGoodBottom + 38f,
        )
        drawFooter(canvas, article)
        return bitmap
    }

    private fun drawBackground(
        canvas: Canvas,
        thumbnail: Bitmap?,
    ) {
        val backgroundPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader =
                    LinearGradient(
                        0f,
                        0f,
                        Width.toFloat(),
                        Height.toFloat(),
                        intArrayOf(
                            DarkBackground,
                            Color.rgb(28, 19, 6),
                            DarkBackground,
                        ),
                        floatArrayOf(0f, 0.54f, 1f),
                        Shader.TileMode.CLAMP,
                    )
            }
        canvas.drawRect(0f, 0f, Width.toFloat(), Height.toFloat(), backgroundPaint)

        if (thumbnail != null && !thumbnail.isRecycled) {
            val source = centerCropSource(thumbnail, Width, ThumbnailHeight)
            val thumbnailPaint =
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    alpha = 92
                }
            canvas.drawBitmap(
                thumbnail,
                source,
                Rect(0, 0, Width, ThumbnailHeight),
                thumbnailPaint,
            )
            canvas.drawRect(
                0f,
                0f,
                Width.toFloat(),
                ThumbnailHeight.toFloat(),
                Paint().apply {
                    shader =
                        LinearGradient(
                            0f,
                            0f,
                            0f,
                            ThumbnailHeight.toFloat(),
                            intArrayOf(
                                Color.argb(125, 14, 11, 6),
                                Color.argb(218, 14, 11, 6),
                                DarkBackground,
                            ),
                            null,
                            Shader.TileMode.CLAMP,
                        )
                },
            )
        }

        drawGlow(
            canvas = canvas,
            centerX = 980f,
            centerY = -120f,
            radius = 420f,
            centerColor = Color.argb(72, 255, 184, 41),
        )
        drawGlow(
            canvas = canvas,
            centerX = -80f,
            centerY = 1_260f,
            radius = 370f,
            centerColor = Color.argb(43, 255, 222, 125),
        )
    }

    private fun drawGlow(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        @ColorInt centerColor: Int,
    ) {
        canvas.drawCircle(
            centerX,
            centerY,
            radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader =
                    RadialGradient(
                        centerX,
                        centerY,
                        radius,
                        centerColor,
                        Color.TRANSPARENT,
                        Shader.TileMode.CLAMP,
                    )
            },
        )
    }

    private fun drawHeader(canvas: Canvas) {
        val badgeCenterX = 115f
        val badgeCenterY = 115f
        canvas.drawCircle(
            badgeCenterX,
            badgeCenterY,
            39f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = WarmAmber
                setShadowLayer(24f, 0f, 0f, Color.argb(106, 255, 184, 41))
            },
        )
        drawCenteredText(
            canvas = canvas,
            text = "N",
            centerX = badgeCenterX,
            centerY = badgeCenterY,
            size = 34f,
            color = DarkBackground,
            typeface = boldRounded,
        )
        drawSingleLineText(
            canvas = canvas,
            text = "NutsNews",
            x = 178f,
            baseline = 112f,
            size = 42f,
            color = PrimaryText,
            typeface = boldRounded,
        )
        drawSingleLineText(
            canvas = canvas,
            text = "Positive news, simplified",
            x = 178f,
            baseline = 143f,
            size = 23f,
            color = SecondaryText,
            typeface = boldRounded,
        )
    }

    private fun drawTitleBlock(
        canvas: Canvas,
        article: Article,
        moodLabel: String,
    ): Float {
        drawSingleLineText(
            canvas = canvas,
            text = "♥  $moodLabel     ◷  ${estimatedReadTime(article)}",
            x = ContentLeft,
            baseline = 214f,
            size = 24f,
            color = SoftAmber,
            typeface = boldRounded,
        )
        val titleHeight =
            drawFittingText(
            canvas = canvas,
            text = article.title,
            x = ContentLeft,
            top = 242f,
            width = ContentWidth,
            maximumHeight = 342,
            initialSize = 62f,
            minimumSize = 43f,
            color = PrimaryText,
            typeface = boldRounded,
            maximumLines = 7,
            lineSpacingExtra = 4f,
        )
        return 242f + titleHeight
    }

    private fun drawWhyGoodBlock(
        canvas: Canvas,
        whyGood: String,
        top: Float,
    ): Float {
        val contentLayout =
            fittingTextLayout(
                text = whyGood,
                width = (ContentRight - ContentLeft - 68f).toInt(),
                maximumHeight = 202,
                initialSize = 34f,
                minimumSize = 27f,
                color = SecondaryText,
                typeface = boldRounded,
                maximumLines = 5,
                lineSpacingExtra = 5f,
            )
        val rect =
            RectF(
                ContentLeft,
                top,
                ContentRight,
                top + 88f + contentLayout.height + 34f,
            )
        val shapePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(224, 31, 23, 10)
                style = Paint.Style.FILL
            }
        canvas.drawRoundRect(rect, 32f, 32f, shapePaint)
        canvas.drawRoundRect(
            rect,
            32f,
            32f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(71, 255, 184, 41)
                style = Paint.Style.STROKE
                strokeWidth = 2f
            },
        )
        drawSingleLineText(
            canvas = canvas,
            text = "WHY IT’S GOOD NEWS",
            x = rect.left + 34f,
            baseline = rect.top + 62f,
            size = 25f,
            color = WarmAmber,
            typeface = boldRounded,
        )
        drawTextLayout(
            canvas = canvas,
            layout = contentLayout,
            x = rect.left + 34f,
            top = rect.top + 88f,
        )
        return rect.bottom
    }

    private fun drawTakeawayBlock(
        canvas: Canvas,
        takeaway: String,
        top: Float,
    ) {
        drawCenteredText(
            canvas = canvas,
            text = "✦",
            centerX = 108f,
            centerY = top + 36f,
            size = 42f,
            color = WarmAmber,
            typeface = boldRounded,
        )
        drawSingleLineText(
            canvas = canvas,
            text = "FEEL-GOOD TAKEAWAY",
            x = 164f,
            baseline = top + 28f,
            size = 24f,
            color = WarmAmber,
            typeface = boldRounded,
        )
        drawFittingText(
            canvas = canvas,
            text = takeaway,
            x = 164f,
            top = top + 51f,
            width = (ContentRight - 164f).toInt(),
            maximumHeight = 150,
            initialSize = 43f,
            minimumSize = 34f,
            color = PrimaryText,
            typeface = boldRounded,
            maximumLines = 3,
            lineSpacingExtra = 4f,
        )
    }

    private fun drawFooter(
        canvas: Canvas,
        article: Article,
    ) {
        canvas.drawRect(
            ContentLeft,
            1_218f,
            ContentRight,
            1_220f,
            Paint().apply {
                color = Color.argb(71, 255, 184, 41)
            },
        )
        val source =
            article.source
                .trim()
                .ifEmpty { Article.DefaultSourceLabel }
        val sourcePaint =
            textPaint(
                size = 24f,
                color = PrimaryText,
                typeface = boldRounded,
            )
        val maximumSourceWidth = 260f
        val displayedSource =
            TextUtils.ellipsize(
                source,
                sourcePaint,
                maximumSourceWidth,
                TextUtils.TruncateAt.END,
            ).toString()
        drawSingleLineText(
            canvas = canvas,
            text = displayedSource,
            x = ContentLeft,
            baseline = 1_275f,
            size = 24f,
            color = PrimaryText,
            typeface = boldRounded,
        )
        val sourceWidth = sourcePaint.measureText(displayedSource)
        canvas.drawCircle(
            ContentLeft + sourceWidth + 18f,
            1_267f,
            4f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = WarmAmber
            },
        )
        drawSingleLineText(
            canvas = canvas,
            text = article.displayDate,
            x = ContentLeft + sourceWidth + 38f,
            baseline = 1_275f,
            size = 24f,
            color = SecondaryText,
            typeface = boldRounded,
        )
        val sitePaint =
            textPaint(
                size = 24f,
                color = WarmAmber,
                typeface = boldRounded,
            )
        val site = "nutsnews.com"
        canvas.drawText(
            site,
            ContentRight - sitePaint.measureText(site),
            1_275f,
            sitePaint,
        )
    }

    private fun drawFittingText(
        canvas: Canvas,
        text: String,
        x: Float,
        top: Float,
        width: Int,
        maximumHeight: Int,
        initialSize: Float,
        minimumSize: Float,
        @ColorInt color: Int,
        typeface: Typeface,
        maximumLines: Int,
        lineSpacingExtra: Float,
    ): Int {
        val layout =
            fittingTextLayout(
                text = text,
                width = width,
                maximumHeight = maximumHeight,
                initialSize = initialSize,
                minimumSize = minimumSize,
                color = color,
                typeface = typeface,
                maximumLines = maximumLines,
                lineSpacingExtra = lineSpacingExtra,
            )
        drawTextLayout(
            canvas = canvas,
            layout = layout,
            x = x,
            top = top,
        )
        return layout.height
    }

    private fun fittingTextLayout(
        text: String,
        width: Int,
        maximumHeight: Int,
        initialSize: Float,
        minimumSize: Float,
        @ColorInt color: Int,
        typeface: Typeface,
        maximumLines: Int,
        lineSpacingExtra: Float,
    ): StaticLayout {
        var size = initialSize
        var layout =
            textLayout(
                text = text,
                width = width,
                size = size,
                color = color,
                typeface = typeface,
                maximumLines = maximumLines,
                lineSpacingExtra = lineSpacingExtra,
            )
        while (layout.height > maximumHeight && size > minimumSize) {
            size = max(minimumSize, size - 2f)
            layout =
                textLayout(
                    text = text,
                    width = width,
                    size = size,
                    color = color,
                    typeface = typeface,
                    maximumLines = maximumLines,
                    lineSpacingExtra = lineSpacingExtra,
                )
        }
        return layout
    }

    private fun drawTextLayout(
        canvas: Canvas,
        layout: StaticLayout,
        x: Float,
        top: Float,
    ) {
        canvas.save()
        canvas.translate(x, top)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun textLayout(
        text: String,
        width: Int,
        size: Float,
        @ColorInt color: Int,
        typeface: Typeface,
        maximumLines: Int,
        lineSpacingExtra: Float,
    ): StaticLayout =
        StaticLayout
            .Builder
            .obtain(
                text.trim(),
                0,
                text.trim().length,
                textPaint(size, color, typeface),
                width,
            ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .setLineSpacing(lineSpacingExtra, 1f)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setMaxLines(maximumLines)
            .build()

    private fun drawSingleLineText(
        canvas: Canvas,
        text: String,
        x: Float,
        baseline: Float,
        size: Float,
        @ColorInt color: Int,
        typeface: Typeface,
    ) {
        canvas.drawText(
            text,
            x,
            baseline,
            textPaint(size, color, typeface),
        )
    }

    private fun drawCenteredText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        centerY: Float,
        size: Float,
        @ColorInt color: Int,
        typeface: Typeface,
    ) {
        val paint = textPaint(size, color, typeface)
        val metrics = paint.fontMetrics
        canvas.drawText(
            text,
            centerX - paint.measureText(text) / 2f,
            centerY - (metrics.ascent + metrics.descent) / 2f,
            paint,
        )
    }

    private fun textPaint(
        size: Float,
        @ColorInt color: Int,
        typeface: Typeface,
    ): TextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            textSize = size
            this.color = color
            this.typeface = typeface
        }

    private fun estimatedReadTime(article: Article): String {
        val count =
            "${article.title} ${article.summary}"
                .split(ShareWhitespace)
                .count(String::isNotBlank)
        return "${max(1, ceil(count / 180.0).toInt())} min"
    }

    private fun centerCropSource(
        bitmap: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
    ): Rect {
        val targetRatio = targetWidth.toFloat() / targetHeight
        val bitmapRatio = bitmap.width.toFloat() / bitmap.height
        return if (bitmapRatio > targetRatio) {
            val sourceWidth = (bitmap.height * targetRatio).toInt()
            val left = (bitmap.width - sourceWidth) / 2
            Rect(left, 0, left + sourceWidth, bitmap.height)
        } else {
            val sourceHeight = (bitmap.width / targetRatio).toInt()
            val top = (bitmap.height - sourceHeight) / 2
            Rect(0, top, bitmap.width, top + sourceHeight)
        }
    }

    private const val ContentLeft = 76f
    private const val ContentRight = 1_004f
    private const val ContentWidth = 928
    private val ShareWhitespace = Regex("\\s+")
}

private const val ShareCardDirectory = "share_cards"
private const val ShareCardMimeType = "image/png"
