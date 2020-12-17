package org.wordpress.android.ui.jetpack

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class BackupAvailableItemsProviderTest {
    private lateinit var provider: BackupAvailableItemsProvider

    @Before
    fun setUp() {
        provider = BackupAvailableItemsProvider()
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
