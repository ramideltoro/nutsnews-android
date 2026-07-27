package com.nutsnews.app.parity

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import java.io.File
import kotlin.math.abs
import kotlin.test.fail

internal object ScreenshotGolden {
    private const val RecordProperty = "nutsnews.recordGoldens"
    private const val ChannelTolerance = 1
    private const val MaximumChangedPixelFraction = 0.0001

    fun assertMatches(
        name: String,
        actual: Bitmap,
    ) {
        require(name.matches(Regex("[a-z0-9_-]+"))) {
            "Golden names must be stable lowercase file names: $name"
        }

        val repository = findRepositoryRoot()
        val baseline = File(repository, "app/src/test/goldens/$name.png")
        val reportDirectory = File(repository, "app/build/reports/screenshot-parity/$name")

        if (System.getProperty(RecordProperty).toBoolean()) {
            checkNotNull(baseline.parentFile).mkdirs()
            writePng(actual, baseline)
            reportDirectory.deleteRecursively()
            return
        }

        if (!baseline.isFile) {
            reportDirectory.mkdirs()
            writePng(actual, File(reportDirectory, "actual.png"))
            fail(
                "Missing screenshot baseline ${baseline.relativeTo(repository)}. " +
                    "Actual output: ${reportDirectory.relativeTo(repository)}/actual.png. " +
                    "Record approved baselines with ./scripts/record-screenshot-goldens.sh.",
            )
        }

        val expected =
            BitmapFactory.decodeFile(baseline.absolutePath)
                ?: fail("Could not decode screenshot baseline ${baseline.relativeTo(repository)}")
        val comparison = compare(expected = expected, actual = actual)
        if (comparison.changedPixelFraction <= MaximumChangedPixelFraction) {
            reportDirectory.deleteRecursively()
            return
        }

        reportDirectory.mkdirs()
        baseline.copyTo(File(reportDirectory, "expected.png"), overwrite = true)
        writePng(actual, File(reportDirectory, "actual.png"))
        writePng(comparison.diff, File(reportDirectory, "diff.png"))
        fail(
            "Screenshot $name changed: ${comparison.changedPixels}/${comparison.totalPixels} " +
                "pixels (${formatPercent(comparison.changedPixelFraction)}) differ beyond " +
                "the ±$ChannelTolerance channel tolerance. Comparison artifacts: " +
                "${reportDirectory.relativeTo(repository)}. " +
                "If intentional, inspect the diff and run ./scripts/record-screenshot-goldens.sh.",
        )
    }

    private fun compare(
        expected: Bitmap,
        actual: Bitmap,
    ): Comparison {
        if (expected.width != actual.width || expected.height != actual.height) {
            val width = maxOf(expected.width, actual.width)
            val height = maxOf(expected.height, actual.height)
            val diff =
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(Color.MAGENTA)
                }
            return Comparison(
                changedPixels = width * height,
                totalPixels = width * height,
                diff = diff,
            )
        }

        val width = actual.width
        val height = actual.height
        val expectedPixels = IntArray(width * height)
        val actualPixels = IntArray(width * height)
        expected.getPixels(expectedPixels, 0, width, 0, 0, width, height)
        actual.getPixels(actualPixels, 0, width, 0, 0, width, height)
        val diffPixels = IntArray(width * height)
        var changedPixels = 0

        actualPixels.indices.forEach { index ->
            val expectedPixel = expectedPixels[index]
            val actualPixel = actualPixels[index]
            val changed =
                abs(Color.alpha(expectedPixel) - Color.alpha(actualPixel)) > ChannelTolerance ||
                    abs(Color.red(expectedPixel) - Color.red(actualPixel)) > ChannelTolerance ||
                    abs(Color.green(expectedPixel) - Color.green(actualPixel)) > ChannelTolerance ||
                    abs(Color.blue(expectedPixel) - Color.blue(actualPixel)) > ChannelTolerance
            if (changed) {
                changedPixels += 1
                val intensity =
                    maxOf(
                        abs(Color.red(expectedPixel) - Color.red(actualPixel)),
                        abs(Color.green(expectedPixel) - Color.green(actualPixel)),
                        abs(Color.blue(expectedPixel) - Color.blue(actualPixel)),
                    )
                diffPixels[index] =
                    Color.rgb(
                        255,
                        (96 - intensity).coerceIn(0, 96),
                        255,
                    )
            } else {
                val luminance =
                    (
                        Color.red(expectedPixel) * 0.2126 +
                            Color.green(expectedPixel) * 0.7152 +
                            Color.blue(expectedPixel) * 0.0722
                    ).toInt()
                val muted = (luminance * 0.22).toInt()
                diffPixels[index] = Color.rgb(muted, muted, muted)
            }
        }

        return Comparison(
            changedPixels = changedPixels,
            totalPixels = actualPixels.size,
            diff =
                Bitmap.createBitmap(
                    diffPixels,
                    width,
                    height,
                    Bitmap.Config.ARGB_8888,
                ),
        )
    }

    private fun writePng(
        bitmap: Bitmap,
        output: File,
    ) {
        checkNotNull(output.parentFile).mkdirs()
        output.outputStream().use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                "Could not encode screenshot ${output.name}"
            }
        }
    }

    private fun findRepositoryRoot(): File {
        val userDirectory = checkNotNull(System.getProperty("user.dir"))
        var candidate = File(userDirectory).absoluteFile
        while (true) {
            if (File(candidate, "settings.gradle.kts").isFile) return candidate
            candidate = candidate.parentFile
                ?: error("Could not find repository root from $userDirectory")
        }
    }

    private fun formatPercent(fraction: Double): String =
        "%.4f%%".format(fraction * 100)

    private data class Comparison(
        val changedPixels: Int,
        val totalPixels: Int,
        val diff: Bitmap,
    ) {
        val changedPixelFraction: Double
            get() = changedPixels.toDouble() / totalPixels.coerceAtLeast(1)
    }
}

internal fun captureLargestWindow(): Bitmap {
    val windowManagerClass = Class.forName("android.view.WindowManagerGlobal")
    val instance =
        windowManagerClass
            .getDeclaredMethod("getInstance")
            .invoke(null)
    val viewsField =
        windowManagerClass
            .getDeclaredField("mViews")
            .apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    val views = viewsField.get(instance) as List<View>
    val view =
        views
            .filter { candidate ->
                candidate.width > 0 && candidate.height > 0 && candidate.isShown
            }.maxByOrNull { candidate -> candidate.width * candidate.height }
            ?: error("No visible Android window was available for screenshot capture")

    return Bitmap
        .createBitmap(
            view.width,
            view.height,
            Bitmap.Config.ARGB_8888,
        ).also { bitmap ->
            view.draw(Canvas(bitmap))
        }
}
