package org.wordpress.android.viewmodel.pages

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.page.PageStatus.DRAFT
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.PENDING
import org.wordpress.android.fluxc.model.post.PostStatus.PRIVATE
import org.wordpress.android.fluxc.model.post.PostStatus.PUBLISHED
import org.wordpress.android.fluxc.model.post.PostStatus.SCHEDULED
import org.wordpress.android.fluxc.store.MediaStore.MediaError
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.store.PostStore.PostError
import org.wordpress.android.fluxc.store.PostStore.PostErrorType
import org.wordpress.android.fluxc.store.PostStore.PostErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.UploadStore.UploadError
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState

@RunWith(MockitoJUnitRunner::class)
class CreatePageListItemLabelsUseCaseTest {
    @Mock private lateinit var pageConflictResolver: PageConflictResolver
    @Mock private lateinit var labelColorUseCase: ResolvePageListItemsColorUseCase
    private lateinit var useCase: CreatePageListItemLabelsUseCase

    @Before
    fun setUp() {
        useCase = CreatePageListItemLabelsUseCase(
                pageConflictResolver,
                labelColorUseCase
        )
    }

    @Test
    fun `private label shown for private pages`() {
        val (labels, _) = useCase.createLabels(
                PostModel().apply { setStatus(PRIVATE.toString()) },
                mock()
        )
        assertThat(labels).contains(UiStringRes(R.string.page_status_page_private))
    }

    @Test
    fun `pending review label shown for pages pending review`() {
        val (labels, _) = useCase.createLabels(
                PostModel().apply { setStatus(PENDING.toString()) },
                mock()
        )
        assertThat(labels).contains(UiStringRes(R.string.page_status_pending_review))
    }

    //
    @Test
    fun `local draft label shown for local pages`() {
        val (labels, _) = useCase.createLabels(PostModel().apply { setIsLocalDraft(true) }, mock())
        assertThat(labels).contains(UiStringRes(R.string.page_local_draft))
    }

    @Test
    fun `locally changed label shown for locally changed pages`() {
        val (labels, _) = useCase.createLabels(PostModel().apply { setIsLocallyChanged(true) }, mock())
        assertThat(labels).contains(UiStringRes(R.string.page_local_changes))
    }

    @Test
    fun `unhandled auto-save label shown for pages with existing auto-save`() {
        whenever(pageConflictResolver.hasUnhandledAutoSave(anyOrNull())).thenReturn(true)
        val (labels, _) = useCase.createLabels(PostModel(), mock())
        assertThat(labels).contains(UiStringRes(R.string.local_page_autosave_revision_available))
    }

    @Test
    fun `uploading page label shown when the page is being uploaded`() {
        val (labels, _) = useCase.createLabels(PostModel(), PostUploadUiState.UploadingPost(false))
        assertThat(labels).contains(UiStringRes(R.string.page_uploading))
    }

    @Test
    fun `uploading draft label shown when the draft is being uploaded`() {
        val (labels, _) = useCase.createLabels(PostModel(), PostUploadUiState.UploadingPost(true))
        assertThat(labels).contains(UiStringRes(R.string.page_uploading_draft))
    }

    @Test
    fun `uploading media label shown when the page's media is being uploaded`() {
        val (labels, _) = useCase.createLabels(PostModel(), PostUploadUiState.UploadingMedia(0))
        assertThat(labels).contains(UiStringRes(R.string.uploading_media))
    }

    @Test
    fun `queued page label shown when the page was enqueued for upload`() {
        val (labels, _) = useCase.createLabels(PostModel(), PostUploadUiState.UploadQueued)
        assertThat(labels).contains(UiStringRes(R.string.page_queued))
    }

    @Test
    fun `error uploading media label shown when the media upload fails`() {
        val (labels, _) = useCase.createLabels(
                PostModel(),
                PostUploadUiState.UploadFailed(
                        error = UploadError(MediaError(AUTHORIZATION_REQUIRED)),
                        isEligibleForAutoUpload = false,
                        retryWillPushChanges = false
                )
        )
        assertThat(labels).contains(UiStringRes(R.string.error_media_recover_page))
    }

    @Test
    fun `generic error message shown when upload fails from unknown reason`() {
        val (labels, _) = useCase.createLabels(
                PostModel(),
                PostUploadUiState.UploadFailed(
                        error = UploadError(PostError(PostErrorType.GENERIC_ERROR, "testing error message")),
                        isEligibleForAutoUpload = false,
                        retryWillPushChanges = false
                )
        )
        assertThat(labels).contains(UiStringRes(R.string.error_generic_error))
    }

    @Test
    fun `given a mix of info and error statuses, only the error status is shown`() {
        val (labels, _) = useCase.createLabels(
                PostModel().apply { setIsLocallyChanged(true) },
                PostUploadUiState.UploadFailed(
                        error = UploadError(MediaError(AUTHORIZATION_REQUIRED)),
                        isEligibleForAutoUpload = false,
                        retryWillPushChanges = false
                )
        )
        assertThat(labels)
                .containsOnly(UiStringRes(R.string.error_media_recover_page))
    }

    @Test
    fun `media upload error shown with specific message for pending page eligible for auto-upload`() {
        val (labels, _) = useCase.createLabels(
                PostModel().apply {
                    setIsLocallyChanged(true)
                    setStatus(PENDING.toString())
                },
                PostUploadUiState.UploadFailed(
                        error = UploadError(MediaError(AUTHORIZATION_REQUIRED)),
                        isEligibleForAutoUpload = true,
                        retryWillPushChanges = true
                )
        )
        assertThat(labels).containsOnly(UiStringRes(R.string.error_media_recover_page_not_submitted_retrying))
    }

    @Test
    fun `media upload error shown with specific message for pending page not eligible for auto-upload`() {
        val (labels, _) = useCase.createLabels(
                PostModel().apply {
                    setIsLocallyChanged(true)
                    setStatus(PENDING.toString())
                },
                PostUploadUiState.UploadFailed(
                        error = UploadError(MediaError(AUTHORIZATION_REQUIRED)),
                        isEligibleForAutoUpload = false,
                        retryWillPushChanges = true
                )
        )
        assertThat(labels).containsOnly(UiStringRes(R.string.error_media_recover_page_not_submitted))
    }

    @Test
    fun `media upload error shown with specific message for scheduled page eligible for auto-upload`() {
        val (labels, _) = useCase.createLabels(
                PostModel().apply {
                    setIsLocallyChanged(true)
                    setStatus(SCHEDULED.toString())
                },
                PostUploadUiState.UploadFailed(
                        error = UploadError(MediaError(AUTHORIZATION_REQUIRED)),
                        isEligibleForAutoUpload = true,
                        retryWillPushChanges = true
                )
        )
        assertThat(labels)
                .containsOnly(UiStringRes(R.string.error_media_recover_page_not_scheduled_retrying))
    }

    @Test
    fun `media upload error shown with specific message for scheduled page not eligible for auto-upload`() {
        val (labels, _) = useCase.createLabels(
                PostModel().apply {
                    setIsLocallyChanged(true)
                    setStatus(SCHEDULED.toString())
                },
                PostUploadUiState.UploadFailed(
                        error = UploadError(MediaError(AUTHORIZATION_REQUIRED)),
                        isEligibleForAutoUpload = false,
                        retryWillPushChanges = true
                )
        )
        assertThat(labels).containsOnly(UiStringRes(R.string.error_media_recover_page_not_scheduled))
    }

    @Test
    fun `retrying media upload shown for draft eligible for auto-upload`() {
        val (labels, _) = useCase.createLabels(
                PostModel().apply {
                    setIsLocallyChanged(true)
                    setStatus(DRAFT.toString())
                },
                PostUploadUiState.UploadFailed(
                        error = UploadError(MediaError(AUTHORIZATION_REQUIRED)),
                        isEligibleForAutoUpload = true,
                        retryWillPushChanges = true
                )
        )
        assertThat(labels).containsOnly(UiStringRes(R.string.error_generic_error_retrying))
    }

    @Test
    fun `base media upload error shown for draft not eligible for auto-upload`() {
        val (labels, _) = useCase.createLabels(
                PostModel().apply {
                    setIsLocallyChanged(true)
                    setStatus(DRAFT.toString())
                },
                PostUploadUiState.UploadFailed(
                        error = UploadError(MediaError(AUTHORIZATION_REQUIRED)),
                        isEligibleForAutoUpload = false,
                        retryWillPushChanges = false
                )
        )
        assertThat(labels).containsOnly(UiStringRes(R.string.error_media_recover_page))
    }

    @Test
    fun `base upload error shown on GENERIC ERROR and not eligible for auto upload`() {
        val (labels, _) = useCase.createLabels(
                PostModel().apply {
                    setIsLocallyChanged(true)
                    setStatus(DRAFT.toString())
                },
                PostUploadUiState.UploadFailed(
                        error = UploadError(PostError(GENERIC_ERROR)),
                        isEligibleForAutoUpload = false,
                        retryWillPushChanges = false
                )
        )
        assertThat(labels)
                .containsOnly(UiStringRes(R.string.error_generic_error))
    }

    @Test
    fun `retrying upload shown for draft eligible for auto-upload`() {
        val (labels, _) = useCase.createLabels(
                PostModel().apply {
                    setIsLocallyChanged(true)
                    setStatus(DRAFT.toString())
                },
                PostUploadUiState.UploadFailed(
                        error = UploadError(PostError(GENERIC_ERROR)),
                        isEligibleForAutoUpload = true,
                        retryWillPushChanges = true
                )
        )
        assertThat(labels)
                .containsOnly(UiStringRes(R.string.error_generic_error_retrying))
    }

    @Test
    fun `multiple info labels are being shown together`() {
        val (labels, _) = useCase.createLabels(
                PostModel().apply {
                    setIsLocallyChanged(true)
                    setStatus(PRIVATE.toString())
                },
                mock()
        )
        assertThat(labels).contains(UiStringRes(R.string.page_local_changes))
        assertThat(labels).contains(UiStringRes(R.string.page_status_page_private))
    }

    @Test
    fun `pending publish page label shown when page eligible for auto-upload`() {
        val (labels, _) = useCase.createLabels(
                PostModel().apply {
                    setIsLocallyChanged(true)
                    setStatus(PUBLISHED.toString())
                },
                PostUploadUiState.UploadWaitingForConnection(PUBLISHED)
        )

        assertThat((labels[0] as UiStringRes).stringRes).isEqualTo(R.string.page_waiting_for_connection_publish)
    }

    @Test
    fun `pending schedule label shown when page eligible for auto-upload`() {
        val (labels, _) = useCase.createLabels(
                PostModel().apply {
                    setIsLocallyChanged(true)
                    setStatus(SCHEDULED.toString())
                },
                PostUploadUiState.UploadWaitingForConnection(SCHEDULED)
        )

        assertThat((labels[0] as UiStringRes).stringRes).isEqualTo(R.string.page_waiting_for_connection_scheduled)
    }

    @Test
    fun `pending publish private page label shown when page eligible for auto-upload`() {
        val (labels, _) = useCase.createLabels(
                PostModel().apply {
                    setIsLocallyChanged(true)
                    setStatus(PRIVATE.toString())
                },
                PostUploadUiState.UploadWaitingForConnection(PRIVATE)
        )

        assertThat((labels[0] as UiStringRes).stringRes).isEqualTo(R.string.page_waiting_for_connection_private)
    }

    @Test
    fun `pending submit page label shown when page eligible for auto-upload`() {
        val (labels, _) = useCase.createLabels(
                PostModel().apply {
                    setIsLocallyChanged(true)
                    setStatus(PENDING.toString())
                },
                PostUploadUiState.UploadWaitingForConnection(PENDING)
        )

        assertThat((labels[0] as UiStringRes).stringRes).isEqualTo(R.string.page_waiting_for_connection_pending)
    }

    @Test
    fun `local changes page label shown when draft eligible for auto-upload`() {
        val (labels, _) = useCase.createLabels(
                PostModel().apply {
                    setIsLocallyChanged(true)
                    setStatus(PostStatus.DRAFT.toString())
                },
                PostUploadUiState.UploadWaitingForConnection(PostStatus.DRAFT)
        )
        assertThat((labels[0] as UiStringRes).stringRes).isEqualTo(R.string.page_waiting_for_connection_draft)
    }

    @Test
    fun `when a page is locally changed and is local draft only "Local draft" label is displayed`() {
        val (labels, _) = useCase.createLabels(
                PostModel().apply {
                    setIsLocallyChanged(true)
                    setIsLocalDraft(true)
                },
                mock()
        )

        assertThat(labels).containsOnly(UiStringRes(R.string.page_local_draft))
    }
}
