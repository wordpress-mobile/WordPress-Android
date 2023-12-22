package org.wordpress.android.ui.reader.viewmodels

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ContentStream.CUSTOM_LIST
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ContentStream.DISCOVER
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ContentStream.LIKED
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ContentStream.SAVED
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ContentStream.SUBSCRIPTIONS

class ContentStreamTest {
    @Test
    fun `Should return correct menuItemId for SUBSCRIPTIONS`() {
        assertThat(SUBSCRIPTIONS.menuItemId).isEqualTo("subscriptions")
    }

    @Test
    fun `Should return correct position for SUBSCRIPTIONS`() {
        assertThat(SUBSCRIPTIONS.position).isEqualTo(0)
    }

    @Test
    fun `Should return correct menuItemId for DISCOVER`() {
        assertThat(DISCOVER.menuItemId).isEqualTo("discover")
    }

    @Test
    fun `Should return correct position for DISCOVER`() {
        assertThat(DISCOVER.position).isEqualTo(1)
    }

    @Test
    fun `Should return correct menuItemId for LIKED`() {
        assertThat(LIKED.menuItemId).isEqualTo("liked")
    }

    @Test
    fun `Should return correct position for LIKED`() {
        assertThat(LIKED.position).isEqualTo(2)
    }

    @Test
    fun `Should return correct menuItemId for SAVED`() {
        assertThat(SAVED.menuItemId).isEqualTo("saved")
    }

    @Test
    fun `Should return correct position for SAVED`() {
        assertThat(SAVED.position).isEqualTo(3)
    }

    @Test
    fun `Should return correct menuItemId for CUSTOM_LIST`() {
        assertThat(CUSTOM_LIST.menuItemId).isEqualTo("custom_list")
    }

    @Test
    fun `Should return correct position for CUSTOM_LIST`() {
        assertThat(CUSTOM_LIST.position).isEqualTo(4)
    }
}
