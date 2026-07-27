package com.nutsnews.app.parity

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.File
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotComparisonArtifactTest {
    @Test
    fun mismatchWritesExpectedActualAndHighContrastDiff() {
        val repository = repositoryRoot()
        val baseline =
            File(
                repository,
                "app/src/test/goldens/phone_s01_splash.png",
            )
        val report =
            File(
                repository,
                "app/build/reports/screenshot-parity/phone_s01_splash",
            )
        val altered =
            checkNotNull(BitmapFactory.decodeFile(baseline.absolutePath))
                .copy(Bitmap.Config.ARGB_8888, true)
        repeat(20) { x ->
            repeat(20) { y ->
                altered.setPixel(x, y, Color.MAGENTA)
            }
        }

        try {
            val failure =
                assertFailsWith<AssertionError> {
                    ScreenshotGolden.assertMatches("phone_s01_splash", altered)
                }
            assertTrue(failure.message.orEmpty().contains("Comparison artifacts"))
            assertTrue(File(report, "expected.png").isFile)
            assertTrue(File(report, "actual.png").isFile)
            assertTrue(File(report, "diff.png").isFile)
        } finally {
            report.deleteRecursively()
        }
    }

    private fun repositoryRoot(): File {
        var candidate =
            File(checkNotNull(System.getProperty("user.dir"))).absoluteFile
        while (true) {
            if (File(candidate, "settings.gradle.kts").isFile) return candidate
            candidate = candidate.parentFile
                ?: error("Could not find the repository root")
        }
    }
}
