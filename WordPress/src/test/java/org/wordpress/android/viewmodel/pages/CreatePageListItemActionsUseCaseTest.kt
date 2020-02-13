package org.wordpress.android.viewmodel.pages

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.pages.PageItem.Action.CANCEL_AUTO_UPLOAD
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState.UploadWaitingForConnection
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState.UploadingPost
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.DRAFTS
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.PUBLISHED

@RunWith(MockitoJUnitRunner::class)
class CreatePageListItemActionsUseCaseTest {
    @Mock private lateinit var postStore: PostStore
    @Mock private lateinit var createPageUploadUiStateUseCase: CreatePageUploadUiStateUseCase
    private lateinit var useCase: CreatePageListItemActionsUseCase

    @Before
    fun setUp() {
        useCase = CreatePageListItemActionsUseCase(
                postStore,
                createPageUploadUiStateUseCase
        )
        whenever(postStore.getPostByLocalPostId(anyInt())).thenReturn(mock())
    }

    @Test
    fun `CANCEL_AUTO_UPLOAD is added to PublishedPage Actions if auto upload is pending`() {
        whenever(createPageUploadUiStateUseCase.createUploadUiState(anyOrNull(), anyOrNull()))
                .thenReturn(UploadWaitingForConnection(mock()))

        val actions = useCase.setupPageActions(PUBLISHED, mock(), mock())

        assertThat(actions).contains(CANCEL_AUTO_UPLOAD)
    }

    @Test
    fun `CANCEL_AUTO_UPLOAD is not added to PublishedPage Actions if auto upload is not pending`() {
        whenever(createPageUploadUiStateUseCase.createUploadUiState(anyOrNull(), anyOrNull()))
                .thenReturn(UploadingPost(false))

        val actions = useCase.setupPageActions(PUBLISHED, mock(), mock())

        assertThat(actions).doesNotContain(CANCEL_AUTO_UPLOAD)
    }

    @Test
    fun `CANCEL_AUTO_UPLOAD is added to DraftPage Actions if auto upload is pending`() {
        whenever(createPageUploadUiStateUseCase.createUploadUiState(anyOrNull(), anyOrNull()))
                .thenReturn(UploadWaitingForConnection(mock()))

        val actions = useCase.setupPageActions(DRAFTS, mock(), mock())

        assertThat(actions).contains(CANCEL_AUTO_UPLOAD)
    }

    @Test
    fun `CANCEL_AUTO_UPLOAD is not added to DraftPage Actions if auto upload is not pending`() {
        whenever(createPageUploadUiStateUseCase.createUploadUiState(anyOrNull(), anyOrNull()))
                .thenReturn(UploadingPost(true))

        val actions = useCase.setupPageActions(DRAFTS, mock(), mock())

        assertThat(actions).doesNotContain(CANCEL_AUTO_UPLOAD)
    }
}
