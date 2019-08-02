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
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.ActivityLauncherWrapper
import org.wordpress.android.ui.WPWebViewUsageCategory
import org.wordpress.android.ui.posts.RemotePreviewLogicHelper.RemotePreviewHelperFunctions
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

    private lateinit var remotePreviewLogicHelper: RemotePreviewLogicHelper

    @Before
    fun setup() {
        remotePreviewLogicHelper = RemotePreviewLogicHelper(
                networkUtilsWrapper,
                activityLauncherWrapper,
                postUtilsWrapper
        )

        doReturn(true).whenever(site).isUsingWpComRestApi

        doReturn(true).whenever(networkUtilsWrapper).isNetworkAvailable()

        doReturn(false).whenever(helperFunctions).notifyUploadInProgress(any())

        doReturn(true).whenever(postUtilsWrapper).isPublishable(post)

        doReturn(PostStatus.DRAFT.toString()).whenever(post).status
        doReturn("2018-06-23T15:45:16+00:00").whenever(post).dateCreated
        doReturn(true).whenever(post).isLocallyChanged
        doReturn("Test title for test purposes").whenever(post).title
    }

    @Test
    fun `preview not available for self hosted sites not using WPComRestApi on published post with modifications`() {
        // Given
        doReturn(false).whenever(site).isUsingWpComRestApi
        doReturn(PostStatus.PUBLISHED.toString()).whenever(post).status
        doReturn(true).whenever(post).isLocallyChanged

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
    fun `preview available for self hosted sites not using WPComRestApi on drafts`() {
        // Given
        // next stub not used (made lenient) in case we update future logic.
        lenient().doReturn(false).whenever(site).isUsingWpComRestApi

        // When
        val result = remotePreviewLogicHelper.runPostPreviewLogic(activity, site, post, helperFunctions)

        // Then
        assertThat(result).isEqualTo(RemotePreviewLogicHelper.PreviewLogicOperationResult.GENERATING_PREVIEW)
        verify(helperFunctions, times(1)).startUploading(false, post)
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
    fun `cannot save empty draft for preview`() {
        // Given
        doReturn(false).whenever(postUtilsWrapper).isPublishable(post)

        // When
        val result = remotePreviewLogicHelper.runPostPreviewLogic(activity, site, post, helperFunctions)

        // Then
        assertThat(result).isEqualTo(RemotePreviewLogicHelper.PreviewLogicOperationResult.CANNOT_SAVE_EMPTY_DRAFT)
        verify(helperFunctions, times(1)).notifyEmptyDraft()
    }

    @Test
    fun `upload new draft for preview`() {
        // Given
        // standard setup conditions are fine

        // When
        val result = remotePreviewLogicHelper.runPostPreviewLogic(activity, site, post, helperFunctions)

        // Then
        assertThat(result).isEqualTo(RemotePreviewLogicHelper.PreviewLogicOperationResult.GENERATING_PREVIEW)
        verify(helperFunctions, times(1)).startUploading(false, post)
    }

    @Test
    fun `cannot remote auto save empty published post for preview`() {
        // Given
        doReturn(false).whenever(postUtilsWrapper).isPublishable(post)
        doReturn(PostStatus.PUBLISHED.toString()).whenever(post).status

        // When
        val result = remotePreviewLogicHelper.runPostPreviewLogic(activity, site, post, helperFunctions)

        // Then
        assertThat(result).isEqualTo(
                RemotePreviewLogicHelper.PreviewLogicOperationResult.CANNOT_REMOTE_AUTO_SAVE_EMPTY_POST
        )
        verify(helperFunctions, times(1)).notifyEmptyPost()
    }

    @Test
    fun `remote auto save published post with local changes for preview`() {
        // Given
        doReturn(PostStatus.PUBLISHED.toString()).whenever(post).status

        // When
        val result = remotePreviewLogicHelper.runPostPreviewLogic(activity, site, post, helperFunctions)

        // Then
        assertThat(result).isEqualTo(RemotePreviewLogicHelper.PreviewLogicOperationResult.GENERATING_PREVIEW)
        verify(helperFunctions, times(1)).startUploading(true, post)
    }

    @Test
    fun `launch remote preview with no uploading for published post without local changes`() {
        // Given
        doReturn(PostStatus.PUBLISHED.toString()).whenever(post).status
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
    fun `cannot remote auto save empty scheduled post for preview`() {
        // Given
        doReturn(false).whenever(postUtilsWrapper).isPublishable(post)
        doReturn(PostStatus.SCHEDULED.toString()).whenever(post).status

        // When
        val result = remotePreviewLogicHelper.runPostPreviewLogic(activity, site, post, helperFunctions)

        // Then
        assertThat(result).isEqualTo(
                RemotePreviewLogicHelper.PreviewLogicOperationResult.CANNOT_REMOTE_AUTO_SAVE_EMPTY_POST
        )
        verify(helperFunctions, times(1)).notifyEmptyPost()
    }

    @Test
    fun `remote auto save scheduled post with local changes for preview`() {
        // Given
        doReturn(PostStatus.SCHEDULED.toString()).whenever(post).status

        // When
        val result = remotePreviewLogicHelper.runPostPreviewLogic(activity, site, post, helperFunctions)

        // Then
        assertThat(result).isEqualTo(RemotePreviewLogicHelper.PreviewLogicOperationResult.GENERATING_PREVIEW)
        verify(helperFunctions, times(1)).startUploading(true, post)
    }

    @Test
    fun `launch remote preview with no uploading for scheduled post without local changes`() {
        // Given
        doReturn(PostStatus.SCHEDULED.toString()).whenever(post).status
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
}
