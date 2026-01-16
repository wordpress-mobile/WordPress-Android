package org.wordpress.android.ui.posts.editor

import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.AppBarLayout
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.navigation.EditPostDestination
import org.wordpress.android.widgets.WPViewPager

@RunWith(MockitoJUnitRunner::class)
class EditorNavigationManagerTest {
    @Mock
    private lateinit var listener: EditorNavigationManager.NavigationListener

    @Mock
    private lateinit var viewPager: WPViewPager

    @Mock
    private lateinit var appBarLayout: AppBarLayout

    @Mock
    private lateinit var toolbar: Toolbar

    @Mock
    private lateinit var siteModel: SiteModel

    @Mock
    private lateinit var editPostRepository: EditPostRepository

    @Mock
    private lateinit var editorPhotoPicker: EditorPhotoPicker

    private lateinit var manager: EditorNavigationManager

    @Before
    fun setUp() {
        manager = EditorNavigationManager()
        manager.start(listener)

        whenever(listener.provideViewPager()).thenReturn(viewPager)
        whenever(listener.getAppBarLayout()).thenReturn(appBarLayout)
        whenever(listener.getToolbar()).thenReturn(toolbar)
        whenever(listener.getSiteModel()).thenReturn(siteModel)
        whenever(listener.getEditPostRepository()).thenReturn(editPostRepository)
        whenever(listener.getEditorPhotoPicker()).thenReturn(editorPhotoPicker)
    }

    // region getPageForDestination tests
    @Test
    fun `getPageForDestination returns 0 for Editor`() {
        assertThat(manager.getPageForDestination(EditPostDestination.Editor))
            .isEqualTo(EditorNavigationManager.VIEW_PAGER_PAGE_CONTENT)
    }

    @Test
    fun `getPageForDestination returns 1 for Settings`() {
        assertThat(manager.getPageForDestination(EditPostDestination.Settings))
            .isEqualTo(EditorNavigationManager.VIEW_PAGER_PAGE_SETTINGS)
    }

    @Test
    fun `getPageForDestination returns 2 for PublishSettings`() {
        assertThat(manager.getPageForDestination(EditPostDestination.PublishSettings))
            .isEqualTo(EditorNavigationManager.VIEW_PAGER_PAGE_PUBLISH_SETTINGS)
    }

    @Test
    fun `getPageForDestination returns 3 for History`() {
        assertThat(manager.getPageForDestination(EditPostDestination.History))
            .isEqualTo(EditorNavigationManager.VIEW_PAGER_PAGE_HISTORY)
    }
    // endregion

    // region updateViewPagerPosition tests
    @Test
    fun `updateViewPagerPosition navigates to Editor page`() {
        whenever(viewPager.currentItem).thenReturn(1) // Currently on Settings

        manager.updateViewPagerPosition(EditPostDestination.Editor)

        verify(viewPager).currentItem = EditorNavigationManager.VIEW_PAGER_PAGE_CONTENT
    }

    @Test
    fun `updateViewPagerPosition navigates to Settings page`() {
        whenever(viewPager.currentItem).thenReturn(0) // Currently on Editor

        manager.updateViewPagerPosition(EditPostDestination.Settings)

        verify(viewPager).currentItem = EditorNavigationManager.VIEW_PAGER_PAGE_SETTINGS
    }

    @Test
    fun `updateViewPagerPosition navigates to PublishSettings page`() {
        whenever(viewPager.currentItem).thenReturn(0)

        manager.updateViewPagerPosition(EditPostDestination.PublishSettings)

        verify(viewPager).currentItem = EditorNavigationManager.VIEW_PAGER_PAGE_PUBLISH_SETTINGS
    }

    @Test
    fun `updateViewPagerPosition navigates to History page`() {
        whenever(viewPager.currentItem).thenReturn(0)

        manager.updateViewPagerPosition(EditPostDestination.History)

        verify(viewPager).currentItem = EditorNavigationManager.VIEW_PAGER_PAGE_HISTORY
    }

    @Test
    fun `updateViewPagerPosition does not navigate when already on target page`() {
        whenever(viewPager.currentItem).thenReturn(EditorNavigationManager.VIEW_PAGER_PAGE_CONTENT)

        manager.updateViewPagerPosition(EditPostDestination.Editor)

        verify(viewPager, never()).currentItem = any()
    }

    @Test
    fun `updateViewPagerPosition handles null viewPager`() {
        whenever(listener.provideViewPager()).thenReturn(null)

        // Should not throw
        manager.updateViewPagerPosition(EditPostDestination.Editor)
    }
    // endregion

    // region updateUIForDestination tests
    @Test
    fun `updateUIForDestination sets title for Editor destination`() {
        whenever(siteModel.name).thenReturn("Test Site")

        manager.updateUIForDestination(EditPostDestination.Editor)

        verify(listener).setTitle(any<CharSequence>())
    }

    @Test
    fun `updateUIForDestination hides photo picker for Settings destination`() {
        whenever(editPostRepository.isPage).thenReturn(false)

        manager.updateUIForDestination(EditPostDestination.Settings)

        verify(editorPhotoPicker).hidePhotoPicker()
    }

    @Test
    fun `updateUIForDestination hides photo picker for PublishSettings destination`() {
        manager.updateUIForDestination(EditPostDestination.PublishSettings)

        verify(editorPhotoPicker).hidePhotoPicker()
    }

    @Test
    fun `updateUIForDestination hides photo picker for History destination`() {
        manager.updateUIForDestination(EditPostDestination.History)

        verify(editorPhotoPicker).hidePhotoPicker()
    }

    @Test
    fun `updateUIForDestination invalidates options menu`() {
        manager.updateUIForDestination(EditPostDestination.Editor)

        verify(listener).invalidateOptionsMenu()
    }

    @Test
    fun `updateUIForDestination handles null appBarLayout`() {
        whenever(listener.getAppBarLayout()).thenReturn(null)

        // Should not throw
        manager.updateUIForDestination(EditPostDestination.Editor)
    }

    @Test
    fun `updateUIForDestination handles null toolbar`() {
        whenever(listener.getToolbar()).thenReturn(null)

        // Should not throw
        manager.updateUIForDestination(EditPostDestination.Editor)
    }
    // endregion

    // region start tests
    @Test
    fun `manager works without listener before start`() {
        val newManager = EditorNavigationManager()

        // Should not throw
        newManager.updateViewPagerPosition(EditPostDestination.Editor)
        newManager.updateUIForDestination(EditPostDestination.Editor)
    }
    // endregion

    // region companion object tests
    @Test
    fun `companion object constants have correct values`() {
        assertThat(EditorNavigationManager.VIEW_PAGER_PAGE_CONTENT).isEqualTo(0)
        assertThat(EditorNavigationManager.VIEW_PAGER_PAGE_SETTINGS).isEqualTo(1)
        assertThat(EditorNavigationManager.VIEW_PAGER_PAGE_PUBLISH_SETTINGS).isEqualTo(2)
        assertThat(EditorNavigationManager.VIEW_PAGER_PAGE_HISTORY).isEqualTo(3)
        assertThat(EditorNavigationManager.VIEW_PAGER_OFFSCREEN_PAGE_LIMIT).isEqualTo(4)
    }
    // endregion
}
