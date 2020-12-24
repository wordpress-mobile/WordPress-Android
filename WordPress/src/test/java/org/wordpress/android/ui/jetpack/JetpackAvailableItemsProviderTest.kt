package org.wordpress.android.ui.jetpack

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider

@RunWith(MockitoJUnitRunner::class)
class JetpackAvailableItemsProviderTest {
    private lateinit var provider: JetpackAvailableItemsProvider

    @Before
    fun setUp() {
        provider = JetpackAvailableItemsProvider()
    }

    @Test
    fun `when requested, the available items list is returned`() {
        // setup
        val expectedItemCount = 6

        // act
        val items = provider.getAvailableItems()

        // Assert
        assertThat(items.size).isEqualTo(expectedItemCount)
    }
}
