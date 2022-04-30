package org.wordpress.android.util

import android.content.Context
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.wordpress.android.ui.mlp.SupportedBlocksProvider
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

@HiltAndroidTest
class SupportedBlocksProviderTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var context: Context

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun fetchSupportedBlocks() {
        val contextProvider = ContextProvider(context)
        val supportedBlocksProvider = SupportedBlocksProvider(contextProvider)
        val supportedBlocks = supportedBlocksProvider.fromAssets().supported
        assertTrue(supportedBlocks.isNotEmpty())
    }
}
