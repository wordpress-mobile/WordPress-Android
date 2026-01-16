@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.posts.editor

import android.content.Context
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.ui.posts.ProgressDialogHelper
import org.wordpress.android.ui.posts.RemotePreviewLogicHelper.RemotePreviewType
import org.wordpress.android.ui.utils.UiHelpers

@RunWith(MockitoJUnitRunner::class)
class PostLoadingStateManagerTest {
    @Mock
    private lateinit var progressDialogHelper: ProgressDialogHelper

    @Mock
    private lateinit var uiHelpers: UiHelpers

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var listener: PostLoadingStateManager.StateChangeListener

    @Mock
    private lateinit var post: PostImmutableModel

    private lateinit var manager: PostLoadingStateManager

    @Before
    fun setUp() {
        manager = PostLoadingStateManager(progressDialogHelper, uiHelpers)
        manager.setListener(listener)
    }

    // region state property tests
    @Test
    fun `initial state is NONE`() {
        assertThat(manager.state).isEqualTo(PostLoadingState.NONE)
    }
    // endregion

    // region isRemotePreviewingFromEditor tests
    @Test
    fun `isRemotePreviewingFromEditor is false for NONE state`() {
        assertThat(manager.isRemotePreviewingFromEditor).isFalse()
    }

    @Test
    fun `isRemotePreviewingFromEditor is true for UPLOADING_FOR_PREVIEW state`() {
        manager.transitionTo(context, PostLoadingState.UPLOADING_FOR_PREVIEW)
        assertThat(manager.isRemotePreviewingFromEditor).isTrue()
    }

    @Test
    fun `isRemotePreviewingFromEditor is true for REMOTE_AUTO_SAVING_FOR_PREVIEW state`() {
        manager.transitionTo(context, PostLoadingState.REMOTE_AUTO_SAVING_FOR_PREVIEW)
        assertThat(manager.isRemotePreviewingFromEditor).isTrue()
    }

    @Test
    fun `isRemotePreviewingFromEditor is true for PREVIEWING state`() {
        manager.transitionTo(context, PostLoadingState.PREVIEWING)
        assertThat(manager.isRemotePreviewingFromEditor).isTrue()
    }

    @Test
    fun `isRemotePreviewingFromEditor is true for REMOTE_AUTO_SAVE_PREVIEW_ERROR state`() {
        manager.transitionTo(context, PostLoadingState.REMOTE_AUTO_SAVE_PREVIEW_ERROR)
        assertThat(manager.isRemotePreviewingFromEditor).isTrue()
    }

    @Test
    fun `isRemotePreviewingFromEditor is false for LOADING_REVISION state`() {
        manager.transitionTo(context, PostLoadingState.LOADING_REVISION)
        assertThat(manager.isRemotePreviewingFromEditor).isFalse()
    }
    // endregion

    // region isUploadingPostForPreview tests
    @Test
    fun `isUploadingPostForPreview is false for NONE state`() {
        assertThat(manager.isUploadingPostForPreview).isFalse()
    }

    @Test
    fun `isUploadingPostForPreview is true for UPLOADING_FOR_PREVIEW state`() {
        manager.transitionTo(context, PostLoadingState.UPLOADING_FOR_PREVIEW)
        assertThat(manager.isUploadingPostForPreview).isTrue()
    }

    @Test
    fun `isUploadingPostForPreview is true for REMOTE_AUTO_SAVING_FOR_PREVIEW state`() {
        manager.transitionTo(context, PostLoadingState.REMOTE_AUTO_SAVING_FOR_PREVIEW)
        assertThat(manager.isUploadingPostForPreview).isTrue()
    }

    @Test
    fun `isUploadingPostForPreview is false for PREVIEWING state`() {
        manager.transitionTo(context, PostLoadingState.PREVIEWING)
        assertThat(manager.isUploadingPostForPreview).isFalse()
    }
    // endregion

    // region isRemoteAutoSaveError tests
    @Test
    fun `isRemoteAutoSaveError is false for NONE state`() {
        assertThat(manager.isRemoteAutoSaveError).isFalse()
    }

    @Test
    fun `isRemoteAutoSaveError is true for REMOTE_AUTO_SAVE_PREVIEW_ERROR state`() {
        manager.transitionTo(context, PostLoadingState.REMOTE_AUTO_SAVE_PREVIEW_ERROR)
        assertThat(manager.isRemoteAutoSaveError).isTrue()
    }

    @Test
    fun `isRemoteAutoSaveError is false for UPLOADING_FOR_PREVIEW state`() {
        manager.transitionTo(context, PostLoadingState.UPLOADING_FOR_PREVIEW)
        assertThat(manager.isRemoteAutoSaveError).isFalse()
    }
    // endregion

    // region transitionTo tests
    @Test
    fun `transitionTo returns true for actual state change`() {
        val result = manager.transitionTo(context, PostLoadingState.UPLOADING_FOR_PREVIEW)
        assertThat(result).isTrue()
    }

    @Test
    fun `transitionTo returns false when already in requested state`() {
        manager.transitionTo(context, PostLoadingState.UPLOADING_FOR_PREVIEW)
        val result = manager.transitionTo(context, PostLoadingState.UPLOADING_FOR_PREVIEW)
        assertThat(result).isFalse()
    }

    @Test
    fun `transitionTo updates state property`() {
        manager.transitionTo(context, PostLoadingState.LOADING_REVISION)
        assertThat(manager.state).isEqualTo(PostLoadingState.LOADING_REVISION)
    }

    @Test
    fun `transitionTo notifies listener of dialog change`() {
        manager.transitionTo(context, PostLoadingState.LOADING_REVISION)
        verify(listener).onProgressDialogChanged(anyOrNull())
    }
    // endregion

    // region handleRemotePreviewUploadResult tests
    @Test
    fun `handleRemotePreviewUploadResult launches preview on success when uploading`() {
        // Set up uploading state first
        manager.transitionTo(context, PostLoadingState.UPLOADING_FOR_PREVIEW)

        // Handle successful upload
        val result = manager.handleRemotePreviewUploadResult(
            context,
            isError = false,
            RemotePreviewType.REMOTE_PREVIEW,
            post
        )

        assertThat(result).isEqualTo(PostLoadingStateManager.RemotePreviewResult.PREVIEW_LAUNCHED)
        verify(listener).onPreviewUploadSuccess()
        verify(listener).onLaunchPreview(RemotePreviewType.REMOTE_PREVIEW)
        assertThat(manager.state).isEqualTo(PostLoadingState.PREVIEWING)
    }

    @Test
    fun `handleRemotePreviewUploadResult shows error on upload failure`() {
        manager.transitionTo(context, PostLoadingState.UPLOADING_FOR_PREVIEW)

        val result = manager.handleRemotePreviewUploadResult(
            context,
            isError = true,
            RemotePreviewType.REMOTE_PREVIEW,
            post
        )

        assertThat(result).isEqualTo(PostLoadingStateManager.RemotePreviewResult.ERROR)
        verify(listener).onPreviewUploadError()
        assertThat(manager.state).isEqualTo(PostLoadingState.NONE)
    }

    @Test
    fun `handleRemotePreviewUploadResult shows error on remote auto save error state`() {
        manager.transitionTo(context, PostLoadingState.REMOTE_AUTO_SAVE_PREVIEW_ERROR)

        val result = manager.handleRemotePreviewUploadResult(
            context,
            isError = false,
            RemotePreviewType.REMOTE_PREVIEW,
            post
        )

        assertThat(result).isEqualTo(PostLoadingStateManager.RemotePreviewResult.ERROR)
        verify(listener).onPreviewUploadError()
    }

    @Test
    fun `handleRemotePreviewUploadResult returns NO_ACTION when not in preview state`() {
        // Manager is in NONE state
        val result = manager.handleRemotePreviewUploadResult(
            context,
            isError = false,
            RemotePreviewType.REMOTE_PREVIEW,
            post
        )

        assertThat(result).isEqualTo(PostLoadingStateManager.RemotePreviewResult.NO_ACTION)
        verify(listener, never()).onLaunchPreview(any())
        verify(listener, never()).onPreviewUploadError()
    }
    // endregion

    // region getStateValue tests
    @Test
    fun `getStateValue returns current state value`() {
        manager.transitionTo(context, PostLoadingState.LOADING_REVISION)
        assertThat(manager.getStateValue()).isEqualTo(PostLoadingState.LOADING_REVISION.value)
    }
    // endregion

    // region clear tests
    @Test
    fun `clear resets state to NONE`() {
        manager.transitionTo(context, PostLoadingState.PREVIEWING)
        manager.clear()
        assertThat(manager.state).isEqualTo(PostLoadingState.NONE)
    }
    // endregion

    // region setListener tests
    @Test
    fun `setListener with null removes listener`() {
        manager.setListener(null)
        // Should not throw when transitioning without listener
        manager.transitionTo(context, PostLoadingState.LOADING_REVISION)
    }
    // endregion
}
