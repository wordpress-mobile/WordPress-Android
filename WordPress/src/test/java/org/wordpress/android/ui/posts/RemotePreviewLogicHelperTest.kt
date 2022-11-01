package org.wordpress.android.ui.posts

import android.app.Activity
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncherWrapper
import org.wordpress.android.ui.WPWebViewUsageCategory
import org.wordpress.android.ui.posts.RemotePreviewLogicHelper.RemotePreviewHelperFunctions
import org.wordpress.android.ui.uploads.UploadActionUseCase
import org.wordpress.android.util.NetworkUtilsWrapper

@RunWith(MockitoJUnitRunner::class)
class RemotePreviewLogicHelperTest {
    @Mock
    private lateinit var activity: Activity

    @Mock
    private lateinit var site: SiteModel
    @Mock
    private lateinit var post: PostModel

    @Mock
    private lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock
    private lateinit var activityLauncherWrapper: ActivityLauncherWrapper

    @Mock
    private lateinit var helperFunctions: RemotePreviewHelperFunctions

    @Mock
    private lateinit var postUtilsWrapper: PostUtilsWrapper

    private var uploadActionUseCase = UploadActionUseCase(mock(), mock(), mock())

    private lateinit var remotePreviewLogicHelper: RemotePreviewLogicHelper

    @Before
    fun setup() {
        remotePreviewLogicHelper = RemotePreviewLogicHelper(
                networkUtilsWrapper,
                activityLauncherWrapper,
                postUtilsWrapper,
                uploadActionUseCase
        )

        doReturn(true).whenever(site).isUsingWpComRestApi

        doReturn(true).whenever(networkUtilsWrapper).isNetworkAvailable()

        doReturn(false).whenever(helperFunctions).notifyUploadInProgress(any())

        doReturn(true).whenever(postUtilsWrapper).isPublishable(post)

        doReturn(true).whenever(post).isLocallyChanged
        doReturn("Test title for test purposes").whenever(post).title
        doReturn(999999).whenever(post).contentHashcode()
        doReturn("").whenever(post).dateCreated
    }

    @Test
    fun `preview available for self hosted sites not using WPComRestApi on a draft post with modifications`() {
        // Given
        doReturn(false).whenever(site).isUsingWpComRestApi
        doReturn(true).whenever(post).isLocallyChanged
        doReturn("draft").whenever(post).status

        // When
        val result = remotePreviewLogicHelper.runPostPreviewLogic(activity, site, post, helperFunctions)

        // Then
        assertThat(result).isEqualTo(RemotePreviewLogicHelper.PreviewLogicOperationResult.GENERATING_PREVIEW)
        verify(helperFunctions, times(1)).startUploading(true, post)
    }

    @Test
    fun `preview not available for self hosted sites not using WPComRestApi on a non-draft post with modifications`() {
        // Given
        doReturn(false).whenever(site).isUsingWpComRestApi
        doReturn(true).whenever(post).isLocallyChanged
        doReturn("publish").whenever(post).status

        // When
        val result = remotePreviewLogicHelper.runPostPreviewLogic(activity, site, post, mock())

        // Then
        assertThat(result).isEqualTo(RemotePreviewLogicHelper.PreviewLogicOperationResult.PREVIEW_NOT_AVAILABLE)
        verify(activityLauncherWrapper, times(1)).showActionableEmptyView(
                activity,
                WPWebViewUsageCategory.REMOTE_PREVIEW_NOT_AVAILABLE,
                post.title
        )
    }

    @Test
    fun `preview not available for self hosted sites not using WPComRestApi`() {
        // Given
        // next stub not used (made lenient) in case we update future logic.
        lenient().doReturn(false).whenever(site).isUsingWpComRestApi

        // When
        val result = remotePreviewLogicHelper.runPostPreviewLogic(activity, site, post, helperFunctions)

        // Then
        assertThat(result).isEqualTo(RemotePreviewLogicHelper.PreviewLogicOperationResult.PREVIEW_NOT_AVAILABLE)
        verify(activityLauncherWrapper, times(1)).showActionableEmptyView(
                activity,
                WPWebViewUsageCategory.REMOTE_PREVIEW_NOT_AVAILABLE,
                post.title
        )
    }

    @Test
    fun `preview not available if network not available`() {
        // Given
        doReturn(false).whenever(networkUtilsWrapper).isNetworkAvailable()

        // When
        val result = remotePreviewLogicHelper.runPostPreviewLogic(activity, site, post, helperFunctions)

        // Then
        assertThat(result).isEqualTo(RemotePreviewLogicHelper.PreviewLogicOperationResult.NETWORK_NOT_AVAILABLE)
        verify(activityLauncherWrapper, times(1)).showActionableEmptyView(
                activity,
                WPWebViewUsageCategory.REMOTE_PREVIEW_NO_NETWORK,
                post.title
        )
    }

    @Test
    fun `preview not available if media upload is in progress`() {
        // Given
        doReturn(true).whenever(helperFunctions).notifyUploadInProgress(any())

        // When
        val result = remotePreviewLogicHelper.runPostPreviewLogic(activity, site, mock(), helperFunctions)

        // Then
        assertThat(result).isEqualTo(RemotePreviewLogicHelper.PreviewLogicOperationResult.MEDIA_UPLOAD_IN_PROGRESS)
        verify(helperFunctions, times(1)).notifyUploadInProgress(any())
    }

    @Test
    fun `cannot save empty local draft for preview`() {
        // Given
        doReturn(false).whenever(postUtilsWrapper).isPublishable(post)
        doReturn(true).whenever(post).isLocalDraft

        // When
        val result = remotePreviewLogicHelper.runPostPreviewLogic(activity, site, post, helperFunctions)

        // Then
        assertThat(result).isEqualTo(RemotePreviewLogicHelper.PreviewLogicOperationResult.CANNOT_SAVE_EMPTY_DRAFT)
        verify(helperFunctions, times(1)).notifyEmptyDraft()
    }

    @Test
    fun `cannot save empty draft for preview`() {
        // Given
        doReturn(false).whenever(postUtilsWrapper).isPublishable(post)

        // When
        val result = remotePreviewLogicHelper.runPostPreviewLogic(activity, site, post, helperFunctions)

        // Then
        assertThat(result)
                .isEqualTo(RemotePreviewLogicHelper.PreviewLogicOperationResult.CANNOT_REMOTE_AUTO_SAVE_EMPTY_POST)
        verify(helperFunctions, times(1)).notifyEmptyPost()
    }

    @Test
    fun `upload local draft for preview`() {
        // Given
        doReturn(true).whenever(post).isLocalDraft

        // When
        val result = remotePreviewLogicHelper.runPostPreviewLogic(activity, site, post, helperFunctions)

        // Then
        assertThat(result).isEqualTo(RemotePreviewLogicHelper.PreviewLogicOperationResult.GENERATING_PREVIEW)
        verify(helperFunctions, times(1)).startUploading(false, post)
    }

    @Test
    fun `cannot remote auto save empty post for preview`() {
        // Given
        doReturn(false).whenever(postUtilsWrapper).isPublishable(post)

        // When
        val result = remotePreviewLogicHelper.runPostPreviewLogic(activity, site, post, helperFunctions)

        // Then
        assertThat(result).isEqualTo(
                RemotePreviewLogicHelper.PreviewLogicOperationResult.CANNOT_REMOTE_AUTO_SAVE_EMPTY_POST
        )
        verify(helperFunctions, times(1)).notifyEmptyPost()
    }

    @Test
    fun `remote auto save post with local changes for preview`() {
        // Given

        // When
        val result = remotePreviewLogicHelper.runPostPreviewLogic(activity, site, post, helperFunctions)

        // Then
        assertThat(result).isEqualTo(RemotePreviewLogicHelper.PreviewLogicOperationResult.GENERATING_PREVIEW)
        verify(helperFunctions, times(1)).startUploading(true, post)
    }

    @Test
    fun `launch remote preview with no uploading for a post without local changes`() {
        // Given
        doReturn(false).whenever(post).isLocallyChanged

        // When
        val result = remotePreviewLogicHelper.runPostPreviewLogic(activity, site, post, helperFunctions)

        // Then
        assertThat(result).isEqualTo(RemotePreviewLogicHelper.PreviewLogicOperationResult.OPENING_PREVIEW)
        verify(helperFunctions, never()).startUploading(any(), any())
        verify(activityLauncherWrapper, times(1)).previewPostOrPageForResult(
                activity,
                site,
                post,
                RemotePreviewLogicHelper.RemotePreviewType.REMOTE_PREVIEW
        )
    }

    @Test
    fun `remote auto save a post with local changes for preview`() {
        // Given

        // When
        val result = remotePreviewLogicHelper.runPostPreviewLogic(activity, site, post, helperFunctions)

        // Then
        assertThat(result).isEqualTo(RemotePreviewLogicHelper.PreviewLogicOperationResult.GENERATING_PREVIEW)
        verify(helperFunctions, times(1)).startUploading(true, post)
    }

    @Test
    fun `preview available for Jetpack sites on a post post without modification`() {
        // Given
        // next stub not used (made lenient) in case we update future logic
        lenient().doReturn(true).whenever(site).isJetpackConnected
        doReturn(false).whenever(post).isLocallyChanged

        // When
        val result = remotePreviewLogicHelper.runPostPreviewLogic(activity, site, post, helperFunctions)

        // Then
        assertThat(result).isEqualTo(RemotePreviewLogicHelper.PreviewLogicOperationResult.OPENING_PREVIEW)
        verify(helperFunctions, never()).startUploading(false, post)
    }

    @Test
    fun `preview available for Jetpack sites on a post without modification`() {
        // Given
        lenient().doReturn(true).whenever(site).isJetpackConnected
        doReturn(false).whenever(post).isLocallyChanged

        // When
        val result = remotePreviewLogicHelper.runPostPreviewLogic(activity, site, post, helperFunctions)

        // Then
        assertThat(result).isEqualTo(RemotePreviewLogicHelper.PreviewLogicOperationResult.OPENING_PREVIEW)
        verify(helperFunctions, never()).startUploading(false, post)
    }
}
