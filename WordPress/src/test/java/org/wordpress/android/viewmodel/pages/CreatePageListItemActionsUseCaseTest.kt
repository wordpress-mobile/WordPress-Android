package org.wordpress.android.viewmodel.pages

import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.pages.PageItem.Action.CANCEL_AUTO_UPLOAD
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState.UploadWaitingForConnection
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState.UploadingPost
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.DRAFTS
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.PUBLISHED

@RunWith(MockitoJUnitRunner::class)
class CreatePageListItemActionsUseCaseTest {
    private lateinit var useCase: CreatePageListItemActionsUseCase

    @Before
    fun setUp() {
        useCase = CreatePageListItemActionsUseCase()
    }

    @Test
    fun `CANCEL_AUTO_UPLOAD is added to PublishedPage Actions if auto upload is pending`() {
        val actions = useCase.setupPageActions(PUBLISHED, UploadWaitingForConnection(mock()))

        assertThat(actions).contains(CANCEL_AUTO_UPLOAD)
    }

    @Test
    fun `CANCEL_AUTO_UPLOAD is not added to PublishedPage Actions if auto upload is not pending`() {
        val actions = useCase.setupPageActions(PUBLISHED, UploadingPost(false))

        assertThat(actions).doesNotContain(CANCEL_AUTO_UPLOAD)
    }

    @Test
    fun `CANCEL_AUTO_UPLOAD is added to DraftPage Actions if auto upload is pending`() {
        val actions = useCase.setupPageActions(DRAFTS, UploadWaitingForConnection(mock()))

        assertThat(actions).contains(CANCEL_AUTO_UPLOAD)
    }

    @Test
    fun `CANCEL_AUTO_UPLOAD is not added to DraftPage Actions if auto upload is not pending`() {
        val actions = useCase.setupPageActions(DRAFTS, UploadingPost(true))

        assertThat(actions).doesNotContain(CANCEL_AUTO_UPLOAD)
    }
}
