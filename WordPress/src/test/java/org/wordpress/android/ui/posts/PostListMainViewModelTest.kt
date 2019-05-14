package org.wordpress.android.ui.posts

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
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
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import org.wordpress.android.viewmodel.helpers.ConnectionStatus.AVAILABLE
import org.wordpress.android.viewmodel.helpers.ConnectionStatus.UNAVAILABLE

class PostListMainViewModelTest {
    @get:Rule val rule = InstantTaskExecutorRule()

    @Test
    fun `when started, it uploads all local drafts`() {
        // Given
        val site = SiteModel()
        val localDraftUploadStarter = mock<LocalDraftUploadStarter>()
        val connectionStatus = MutableLiveData<ConnectionStatus>().apply { value = AVAILABLE }

        val viewModel = createPostListMainViewModel(
                localDraftUploadStarter = localDraftUploadStarter,
                connectionStatus = connectionStatus
        )

        // When
        viewModel.start(site = site)

        // Then
        verify(localDraftUploadStarter, times(1)).queueUpload(site = eq(site))
    }

    @Test
    fun `when the internet connection changes, it uploads all local drafts`() {
        // Given
        val site = SiteModel()
        val localDraftUploadStarter = mock<LocalDraftUploadStarter>()
        val connectionStatus = MutableLiveData<ConnectionStatus>().apply { value = AVAILABLE }

        val viewModel = createPostListMainViewModel(
                localDraftUploadStarter = localDraftUploadStarter,
                connectionStatus = connectionStatus
        )
        viewModel.start(site = site)

        // When
        connectionStatus.postValue(UNAVAILABLE)
        connectionStatus.postValue(AVAILABLE)

        // Then
        // The upload should be executed 3 times because we have 2 connections status changes plus the auto-upload
        // during `viewModel.start()`.
        verify(localDraftUploadStarter, times(3)).queueUpload(site = eq(site))
    }

    private companion object {
        fun createPostListMainViewModel(
            localDraftUploadStarter: LocalDraftUploadStarter,
            connectionStatus: LiveData<ConnectionStatus>
        ): PostListMainViewModel {
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
                    postListEventListenerFactory = mock()
            )
        }
    }
}
