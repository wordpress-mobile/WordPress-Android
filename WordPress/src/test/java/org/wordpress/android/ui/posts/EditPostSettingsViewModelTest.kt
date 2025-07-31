package org.wordpress.android.ui.posts

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.posts.PostSettingsListDialogFragment.DialogType

@ExperimentalCoroutinesApi
class EditPostSettingsViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: EditPostSettingsViewModel

    @Before
    fun setUp() {
        viewModel = EditPostSettingsViewModel()
    }

    // Dialog Result Tests
    @Test
    fun `onDialogResult emits dialog result event`() {
        // Arrange
        val dialogType = DialogType.POST_STATUS
        val checkedIndex = 1
        val selectedItem = "Draft"

        // Act
        viewModel.onDialogResult(dialogType, checkedIndex, selectedItem)

        // Assert
        val result = viewModel.dialogResult.value
        assertThat(result).isNotNull
        assertThat(result!!.peekContent().dialogType).isEqualTo(dialogType)
        assertThat(result.peekContent().checkedIndex).isEqualTo(checkedIndex)
        assertThat(result.peekContent().selectedItem).isEqualTo(selectedItem)
    }

    @Test
    fun `onDialogResult with null selectedItem works correctly`() {
        // Arrange
        val dialogType = DialogType.POST_FORMAT
        val checkedIndex = 0

        // Act
        viewModel.onDialogResult(dialogType, checkedIndex, null)

        // Assert
        val result = viewModel.dialogResult.value
        assertThat(result).isNotNull
        assertThat(result!!.peekContent().dialogType).isEqualTo(dialogType)
        assertThat(result.peekContent().checkedIndex).isEqualTo(checkedIndex)
        assertThat(result.peekContent().selectedItem).isNull()
    }

    @Test
    fun `multiple onDialogResult calls emit separate events`() {
        // Arrange
        val firstDialogType = DialogType.POST_STATUS
        val secondDialogType = DialogType.POST_FORMAT

        // Act
        viewModel.onDialogResult(firstDialogType, 0, "Published")
        val firstResult = viewModel.dialogResult.value

        viewModel.onDialogResult(secondDialogType, 1, "Gallery")
        val secondResult = viewModel.dialogResult.value

        // Assert
        assertThat(firstResult).isNotNull
        assertThat(secondResult).isNotNull
        assertThat(firstResult!!.peekContent().dialogType).isEqualTo(firstDialogType)
        assertThat(secondResult!!.peekContent().dialogType).isEqualTo(secondDialogType)
        // Events should be different instances
        assertThat(firstResult).isNotSameAs(secondResult)
    }

    @Test
    fun `dialog result event is consumed only once`() {
        // Arrange
        viewModel.onDialogResult(DialogType.POST_STATUS, 0, "Draft")
        val event = viewModel.dialogResult.value!!

        // Act & Assert
        val firstConsumption = event.getContentIfNotHandled()
        val secondConsumption = event.getContentIfNotHandled()

        assertThat(firstConsumption).isNotNull
        assertThat(secondConsumption).isNull() // Should be null on second consumption
    }

    // Clear Featured Image Tests
    @Test
    fun `onClearFeaturedImage emits clear featured image event`() {
        // Act
        viewModel.onClearFeaturedImage()

        // Assert
        val result = viewModel.clearFeaturedImage.value
        assertThat(result).isNotNull
        assertThat(result!!.peekContent()).isEqualTo(Unit)
    }

    @Test
    fun `multiple onClearFeaturedImage calls emit separate events`() {
        // Act
        viewModel.onClearFeaturedImage()
        val firstResult = viewModel.clearFeaturedImage.value

        viewModel.onClearFeaturedImage()
        val secondResult = viewModel.clearFeaturedImage.value

        // Assert
        assertThat(firstResult).isNotNull
        assertThat(secondResult).isNotNull
        // Events should be different instances
        assertThat(firstResult).isNotSameAs(secondResult)
    }

    @Test
    fun `clear featured image event is consumed only once`() {
        // Arrange
        viewModel.onClearFeaturedImage()
        val event = viewModel.clearFeaturedImage.value!!

        // Act & Assert
        val firstConsumption = event.getContentIfNotHandled()
        val secondConsumption = event.getContentIfNotHandled()

        assertThat(firstConsumption).isNotNull
        assertThat(secondConsumption).isNull() // Should be null on second consumption
    }

    // Integration Tests
    @Test
    fun `dialog result and clear featured image events are independent`() {
        // Act
        viewModel.onDialogResult(DialogType.POST_STATUS, 0, "Published")
        viewModel.onClearFeaturedImage()

        // Assert
        assertThat(viewModel.dialogResult.value).isNotNull
        assertThat(viewModel.clearFeaturedImage.value).isNotNull

        // Consuming one should not affect the other
        val dialogEvent = viewModel.dialogResult.value!!
        val clearImageEvent = viewModel.clearFeaturedImage.value!!

        dialogEvent.getContentIfNotHandled()
        assertThat(clearImageEvent.getContentIfNotHandled()).isNotNull
    }
}
