package org.wordpress.android.ui.posts.navigation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.viewmodel.Event

@ExperimentalCoroutinesApi
class EditPostNavigationViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: EditPostNavigationViewModel

    @Before
    fun setUp() {
        viewModel = EditPostNavigationViewModel()
    }

    @Test
    fun `should initialize with default destination`() {
        // Verify initial state
        assertThat(viewModel.currentDestination.value).isEqualTo(EditPostDestination.Editor)
    }

    @Test
    fun `should update current destination when navigateTo is called`() {
        // When navigating to settings
        viewModel.navigateTo(EditPostDestination.Settings)

        // Then current destination should be updated
        assertThat(viewModel.currentDestination.value).isEqualTo(EditPostDestination.Settings)
    }

    @Test
    fun `should emit navigation event when navigateTo is called`() {
        // Given
        var capturedEvent: Event<EditPostDestination>? = null
        viewModel.navigationEvents.observeForever { capturedEvent = it }

        // When
        viewModel.navigateTo(EditPostDestination.History)

        // Then
        assertThat(capturedEvent).isNotNull
        assertThat(capturedEvent!!.peekContent()).isEqualTo(EditPostDestination.History)
    }

    @Test
    fun `should emit new event for each navigation call`() {
        // Given
        val capturedEvents = mutableListOf<Event<EditPostDestination>>()
        viewModel.navigationEvents.observeForever { capturedEvents.add(it) }

        // When
        viewModel.navigateTo(EditPostDestination.Settings)
        viewModel.navigateTo(EditPostDestination.PublishSettings)

        // Then
        assertThat(capturedEvents).hasSize(2)
        assertThat(capturedEvents[0].peekContent()).isEqualTo(EditPostDestination.Settings)
        assertThat(capturedEvents[1].peekContent()).isEqualTo(EditPostDestination.PublishSettings)
    }

    @Test
    fun `canNavigateBack should return false when on Editor`() {
        // Given - initialized on Editor by default

        // When/Then
        assertThat(viewModel.canNavigateBack()).isFalse
    }

    @Test
    fun `canNavigateBack should return true when on Settings`() {
        // Given
        viewModel.navigateTo(EditPostDestination.Settings)

        // When/Then
        assertThat(viewModel.canNavigateBack()).isTrue
    }

    @Test
    fun `canNavigateBack should return true when on PublishSettings`() {
        // Given
        viewModel.navigateTo(EditPostDestination.PublishSettings)

        // When/Then
        assertThat(viewModel.canNavigateBack()).isTrue
    }

    @Test
    fun `canNavigateBack should return true when on History`() {
        // Given
        viewModel.navigateTo(EditPostDestination.History)

        // When/Then
        assertThat(viewModel.canNavigateBack()).isTrue
    }

    @Test
    fun `handleBackNavigation should return false when on Editor`() {
        // Given - initialized on Editor by default

        // When
        val handled = viewModel.handleBackNavigation()

        // Then
        assertThat(handled).isFalse
        assertThat(viewModel.currentDestination.value).isEqualTo(EditPostDestination.Editor)
    }

    @Test
    fun `handleBackNavigation should navigate from Settings to Editor`() {
        // Given
        viewModel.navigateTo(EditPostDestination.Settings)

        // When
        val handled = viewModel.handleBackNavigation()

        // Then
        assertThat(handled).isTrue
        assertThat(viewModel.currentDestination.value).isEqualTo(EditPostDestination.Editor)
    }

    @Test
    fun `handleBackNavigation should navigate from History to Editor`() {
        // Given
        viewModel.navigateTo(EditPostDestination.History)

        // When
        val handled = viewModel.handleBackNavigation()

        // Then
        assertThat(handled).isTrue
        assertThat(viewModel.currentDestination.value).isEqualTo(EditPostDestination.Editor)
    }

    @Test
    fun `handleBackNavigation should navigate from PublishSettings to Settings`() {
        // Given
        viewModel.navigateTo(EditPostDestination.PublishSettings)

        // When
        val handled = viewModel.handleBackNavigation()

        // Then
        assertThat(handled).isTrue
        assertThat(viewModel.currentDestination.value).isEqualTo(EditPostDestination.Settings)
    }

    @Test
    fun `should emit navigation events during back navigation`() {
        // Given
        val capturedEvents = mutableListOf<Event<EditPostDestination>>()
        viewModel.navigationEvents.observeForever { capturedEvents.add(it) }
        viewModel.navigateTo(EditPostDestination.PublishSettings)
        capturedEvents.clear() // Clear the initial navigation event

        // When
        viewModel.handleBackNavigation()

        // Then
        assertThat(capturedEvents).hasSize(1)
        assertThat(capturedEvents[0].peekContent()).isEqualTo(EditPostDestination.Settings)
    }

    @Test
    fun `should handle navigation flow through multiple destinations`() {
        // Test the complete navigation flow: Editor -> Settings -> PublishSettings -> Settings -> Editor

        // Start at Editor (default)
        assertThat(viewModel.currentDestination.value).isEqualTo(EditPostDestination.Editor)
        assertThat(viewModel.canNavigateBack()).isFalse

        // Navigate to Settings
        viewModel.navigateTo(EditPostDestination.Settings)
        assertThat(viewModel.currentDestination.value).isEqualTo(EditPostDestination.Settings)
        assertThat(viewModel.canNavigateBack()).isTrue

        // Navigate to PublishSettings
        viewModel.navigateTo(EditPostDestination.PublishSettings)
        assertThat(viewModel.currentDestination.value).isEqualTo(EditPostDestination.PublishSettings)
        assertThat(viewModel.canNavigateBack()).isTrue

        // Navigate back to Settings
        var handled = viewModel.handleBackNavigation()
        assertThat(handled).isTrue
        assertThat(viewModel.currentDestination.value).isEqualTo(EditPostDestination.Settings)

        // Navigate back to Editor
        handled = viewModel.handleBackNavigation()
        assertThat(handled).isTrue
        assertThat(viewModel.currentDestination.value).isEqualTo(EditPostDestination.Editor)
        assertThat(viewModel.canNavigateBack()).isFalse

        // Try to navigate back from Editor (should not be handled)
        handled = viewModel.handleBackNavigation()
        assertThat(handled).isFalse
        assertThat(viewModel.currentDestination.value).isEqualTo(EditPostDestination.Editor)
    }
}
