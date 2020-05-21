package org.wordpress.android.ui.uploads

import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.model.post.PostStatus.PENDING
import org.wordpress.android.fluxc.model.post.PostStatus.PRIVATE
import org.wordpress.android.fluxc.model.post.PostStatus.PUBLISHED
import org.wordpress.android.fluxc.model.post.PostStatus.SCHEDULED
import org.wordpress.android.fluxc.store.PostStore.PostError
import org.wordpress.android.fluxc.store.PostStore.PostErrorType
import org.wordpress.android.fluxc.store.PostStore.PostErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.PostStore.PostErrorType.UNAUTHORIZED
import org.wordpress.android.fluxc.store.PostStore.PostErrorType.UNKNOWN_POST
import org.wordpress.android.fluxc.store.PostStore.PostErrorType.UNKNOWN_POST_TYPE
import org.wordpress.android.ui.utils.UiString.UiStringRes

@RunWith(MockitoJUnitRunner::class)
class UploadUtilsTest {
    @Test
    fun `getErrorMessageResIdFromPostError for unknown post`() {
        verifyGenericError(UNKNOWN_POST, false, resId = R.string.error_unknown_post)
    }

    @Test
    fun `getErrorMessageResIdFromPostError for unknown page`() {
        verifyGenericError(UNKNOWN_POST, true, resId = R.string.error_unknown_page)
    }

    @Test
    fun `getErrorMessageResIdFromPostError for unknown post type`() {
        verifyGenericError(UNKNOWN_POST_TYPE, false, resId = R.string.error_unknown_post_type)
    }

    @Test
    fun `getErrorMessageResIdFromPostError for unknown page type`() {
        verifyGenericError(UNKNOWN_POST_TYPE, true, resId = R.string.error_unknown_page_type)
    }

    @Test
    fun `getErrorMessageResIdFromPostError when user unauthorized for uploading post`() {
        verifyGenericError(UNAUTHORIZED, false, resId = R.string.error_refresh_unauthorized_posts)
    }

    @Test
    fun `getErrorMessageResIdFromPostError when user unauthorized for uploading page`() {
        verifyGenericError(UNAUTHORIZED, true, resId = R.string.error_refresh_unauthorized_pages)
    }

    @Test
    fun `getErrorMessageResIdFromPostError on generic error and published post eligible for auto upload`() {
        verifyGenericError(
                GENERIC_ERROR,
                isPage = false,
                postStatus = PUBLISHED,
                isEligibleForAutoUpload = true,
                resId = R.string.error_post_not_published_retrying
        )
    }

    @Test
    fun `getErrorMessageResIdFromPostError on generic error and published page eligible for auto upload`() {
        verifyGenericError(
                GENERIC_ERROR,
                isPage = true,
                postStatus = PUBLISHED,
                isEligibleForAutoUpload = true,
                resId = R.string.error_page_not_published_retrying
        )
    }

    @Test
    fun `getErrorMessageResIdFromPostError on generic error and private post eligible for auto upload`() {
        verifyGenericError(
                GENERIC_ERROR,
                isPage = false,
                postStatus = PRIVATE,
                isEligibleForAutoUpload = true,
                resId = R.string.error_post_not_published_retrying_private
        )
    }

    @Test
    fun `getErrorMessageResIdFromPostError on generic error and private page eligible for auto upload`() {
        verifyGenericError(
                GENERIC_ERROR,
                isPage = true,
                postStatus = PRIVATE,
                isEligibleForAutoUpload = true,
                resId = R.string.error_page_not_published_retrying_private
        )
    }

    @Test
    fun `getErrorMessageResIdFromPostError on generic error and DRAFT post eligible for auto upload`() {
        verifyGenericError(
                GENERIC_ERROR,
                isPage = false,
                postStatus = DRAFT,
                isEligibleForAutoUpload = true,
                resId = R.string.error_generic_error_retrying
        )
    }

    @Test
    fun `getErrorMessageResIdFromPostError on generic error and DRAFT page eligible for auto upload`() {
        verifyGenericError(
                GENERIC_ERROR,
                isPage = true,
                postStatus = DRAFT,
                isEligibleForAutoUpload = true,
                resId = R.string.error_generic_error_retrying
        )
    }

    @Test
    fun `getErrorMessageResIdFromPostError on generic error and SCHEDULED post eligible for auto upload`() {
        verifyGenericError(
                GENERIC_ERROR,
                isPage = false,
                postStatus = SCHEDULED,
                isEligibleForAutoUpload = true,
                resId = R.string.error_post_not_scheduled_retrying
        )
    }

    @Test
    fun `getErrorMessageResIdFromPostError on generic error and SCHEDULED page eligible for auto upload`() {
        verifyGenericError(
                GENERIC_ERROR,
                isPage = true,
                postStatus = SCHEDULED,
                isEligibleForAutoUpload = true,
                resId = R.string.error_page_not_scheduled_retrying
        )
    }

    @Test
    fun `getErrorMessageResIdFromPostError on generic error and PENDING post eligible for auto upload`() {
        verifyGenericError(
                GENERIC_ERROR,
                isPage = false,
                postStatus = PENDING,
                isEligibleForAutoUpload = true,
                resId = R.string.error_post_not_submitted_retrying
        )
    }

    @Test
    fun `getErrorMessageResIdFromPostError on generic error and PENDING page eligible for auto upload`() {
        verifyGenericError(
                GENERIC_ERROR,
                isPage = true,
                postStatus = PENDING,
                isEligibleForAutoUpload = true,
                resId = R.string.error_page_not_submitted_retrying
        )
    }

    @Test
    fun `getErrorMessageResIdFromPostError on generic error and published post NOT eligible for auto upload`() {
        verifyGenericError(
                GENERIC_ERROR,
                isPage = false,
                postStatus = PUBLISHED,
                isEligibleForAutoUpload = false,
                resId = R.string.error_post_not_published
        )
    }

    @Test
    fun `getErrorMessageResIdFromPostError on generic error and published page NOT eligible for auto upload`() {
        verifyGenericError(
                GENERIC_ERROR,
                isPage = true,
                postStatus = PUBLISHED,
                isEligibleForAutoUpload = false,
                resId = R.string.error_page_not_published
        )
    }

    @Test
    fun `getErrorMessageResIdFromPostError on generic error and private post NOT eligible for auto upload`() {
        verifyGenericError(
                GENERIC_ERROR,
                isPage = false,
                postStatus = PRIVATE,
                isEligibleForAutoUpload = false,
                resId = R.string.error_post_not_published_private
        )
    }

    @Test
    fun `getErrorMessageResIdFromPostError on generic error and private page NOT eligible for auto upload`() {
        verifyGenericError(
                GENERIC_ERROR,
                isPage = true,
                postStatus = PRIVATE,
                isEligibleForAutoUpload = false,
                resId = R.string.error_page_not_published_private
        )
    }

    @Test
    fun `getErrorMessageResIdFromPostError on generic error and DRAFT post NOT eligible for auto upload`() {
        verifyGenericError(
                GENERIC_ERROR,
                isPage = false,
                postStatus = DRAFT,
                isEligibleForAutoUpload = false,
                resId = R.string.error_generic_error
        )
    }

    @Test
    fun `getErrorMessageResIdFromPostError on generic error and DRAFT page NOT eligible for auto upload`() {
        verifyGenericError(
                GENERIC_ERROR,
                isPage = true,
                postStatus = DRAFT,
                isEligibleForAutoUpload = false,
                resId = R.string.error_generic_error
        )
    }

    @Test
    fun `getErrorMessageResIdFromPostError on generic error and SCHEDULED post NOT eligible for auto upload`() {
        verifyGenericError(
                GENERIC_ERROR,
                isPage = false,
                postStatus = SCHEDULED,
                isEligibleForAutoUpload = false,
                resId = R.string.error_post_not_scheduled
        )
    }

    @Test
    fun `getErrorMessageResIdFromPostError on generic error and SCHEDULED page NOT eligible for auto upload`() {
        verifyGenericError(
                GENERIC_ERROR,
                isPage = true,
                postStatus = SCHEDULED,
                isEligibleForAutoUpload = false,
                resId = R.string.error_page_not_scheduled
        )
    }

    @Test
    fun `getErrorMessageResIdFromPostError on generic error and PENDING post NOT eligible for auto upload`() {
        verifyGenericError(
                GENERIC_ERROR,
                isPage = false,
                postStatus = PENDING,
                isEligibleForAutoUpload = false,
                resId = R.string.error_post_not_submitted
        )
    }

    @Test
    fun `getErrorMessageResIdFromPostError on generic error and PENDING page NOT eligible for auto upload`() {
        verifyGenericError(
                GENERIC_ERROR,
                isPage = true,
                postStatus = PENDING,
                isEligibleForAutoUpload = false,
                resId = R.string.error_page_not_submitted
        )
    }

    private fun verifyGenericError(
        errorType: PostErrorType,
        isPage: Boolean,
        postStatus: PostStatus = mock(),
        isEligibleForAutoUpload: Boolean = false,
        resId: Int
    ) {
        // Arrange
        val error = PostError(errorType)
        // Act
        val msgId = UploadUtils.getErrorMessageResIdFromPostError(
                postStatus,
                isPage,
                error,
                isEligibleForAutoUpload
        )
        // Assert
        assertThat(msgId).isEqualTo(UiStringRes(resId))
    }
}
