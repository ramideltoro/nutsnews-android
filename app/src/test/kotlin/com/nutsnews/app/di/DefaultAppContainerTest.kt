package com.nutsnews.app.di

import kotlin.test.assertSame
import org.junit.Test

class DefaultAppContainerTest {
    @Test
    fun applicationDependenciesAreStableForTheContainerLifetime() {
        val container = DefaultAppContainer()

        assertSame(container.navigator, container.navigator)
    }
}
