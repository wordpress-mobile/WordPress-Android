package org.wordpress.android.ui.posts

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest

@ExperimentalCoroutinesApi
class GutenbergKitViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: GutenbergKitViewModel

    @Before
    fun setUp() {
        viewModel = GutenbergKitViewModel()
    }

    @Test
    fun `updateEditorSettings stores and exposes settings correctly`() = test {
        // Arrange
        val testSettings = GutenbergKitSettings(
            postId = 123,
            postType = "post",
            postTitle = "Test Post",
            postContent = "Test content",
            siteURL = "https://example.com",
            siteApiRoot = "https://example.com/wp-json",
            authHeader = "Bearer token123",
            locale = "en"
        )

        // Act
        viewModel.updateEditorSettings(testSettings)

        // Assert
        assertThat(viewModel.editorSettings.value).isEqualTo(testSettings)
        assertThat(viewModel.editorSettings.value?.postId).isEqualTo(123)
        assertThat(viewModel.editorSettings.value?.postTitle).isEqualTo("Test Post")
    }
}
