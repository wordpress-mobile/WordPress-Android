package org.wordpress.android.ui.posts

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Rule
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.PostListViewLayoutType.STANDARD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.uploads.LocalDraftUploadStarter

class PostListMainViewModelTest {
    @get:Rule val rule = InstantTaskExecutorRule()

    @Test
    fun `when started, it uploads all local drafts`() {
        // Given
        val site = SiteModel()
        val localDraftUploadStarter = mock<LocalDraftUploadStarter>()

        val viewModel = createPostListMainViewModel(localDraftUploadStarter)

        // When
        viewModel.start(site = site)

        // Then
        verify(localDraftUploadStarter, times(1)).queueUploadFromSite(eq(site))
    }

    private companion object {
        fun createPostListMainViewModel(localDraftUploadStarter: LocalDraftUploadStarter): PostListMainViewModel {
            val prefs = mock<AppPrefsWrapper> {
                on { postListViewLayoutType } doReturn STANDARD
            }

            return PostListMainViewModel(
                    dispatcher = mock(),
                    postStore = mock(),
                    accountStore = mock(),
                    uploadStore = mock(),
                    mediaStore = mock(),
                    networkUtilsWrapper = mock(),
                    prefs = prefs,
                    mainDispatcher = mock(),
                    bgDispatcher = mock(),
                    postListEventListenerFactory = mock(),
                    localDraftUploadStarter = localDraftUploadStarter
            )
        }
    }
}
