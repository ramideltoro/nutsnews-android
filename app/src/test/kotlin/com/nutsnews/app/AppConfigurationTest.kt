package com.nutsnews.app

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class AppConfigurationTest {
    @Test
    fun debugBuildKeepsLockedApplicationIdentity() {
        assertEquals("com.nutsnews.app", BuildConfig.APPLICATION_ID)
        assertEquals("debug", BuildConfig.BUILD_TYPE)
        assertEquals("1.1.1", BuildConfig.VERSION_NAME)
        assertEquals(1, BuildConfig.VERSION_CODE)
        assertTrue(BuildConfig.DEBUG)
    }
}
