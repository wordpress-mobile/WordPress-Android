package org.wordpress.android.viewmodel.pages

import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteHomepageSettings.ShowOnFront
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.pages.PageItem.Action.CANCEL_AUTO_UPLOAD
import org.wordpress.android.ui.pages.PageItem.Action.DELETE_PERMANENTLY
import org.wordpress.android.ui.pages.PageItem.Action.MOVE_TO_DRAFT
import org.wordpress.android.ui.pages.PageItem.Action.MOVE_TO_TRASH
import org.wordpress.android.ui.pages.PageItem.Action.PUBLISH_NOW
import org.wordpress.android.ui.pages.PageItem.Action.SET_AS_HOMEPAGE
import org.wordpress.android.ui.pages.PageItem.Action.SET_AS_POSTS_PAGE
import org.wordpress.android.ui.pages.PageItem.Action.SET_PARENT
import org.wordpress.android.ui.pages.PageItem.Action.VIEW_PAGE
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadWaitingForConnection
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadingPost
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.DRAFTS
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.PUBLISHED
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.SCHEDULED
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.TRASHED

@RunWith(MockitoJUnitRunner::class)
class CreatePageListItemActionsUseCaseTest {
    private lateinit var site: SiteModel
    private lateinit var useCase: CreatePageListItemActionsUseCase
    private val defaultRemoteId: Long = 1

    @Before
    fun setUp() {
        useCase = CreatePageListItemActionsUseCase()
        site = SiteModel()
    }

    @Test
    fun `Verify DRAFT actions`() {
        // Arrange
        val expectedActions = setOf(VIEW_PAGE, SET_PARENT, PUBLISH_NOW, MOVE_TO_TRASH)

        // Act
        val draftActions = useCase.setupPageActions(DRAFTS, mock(), site, defaultRemoteId)

        // Assert
        assertThat(draftActions).isEqualTo(expectedActions)
    }

    @Test
    fun `Verify TRASH actions`() {
        // Arrange
        val expectedActions = setOf(MOVE_TO_DRAFT, DELETE_PERMANENTLY)

        // Act
        val trashedActions = useCase.setupPageActions(TRASHED, mock(), site, defaultRemoteId)

        // Assert
        assertThat(trashedActions).isEqualTo(expectedActions)
    }

    @Test
    fun `verify PUBLISHED actions`() {
        // Arrange
        val expectedActions = setOf(
                VIEW_PAGE,
                SET_PARENT,
                MOVE_TO_DRAFT,
                MOVE_TO_TRASH
        )

        // Act
        val publishedActions = useCase.setupPageActions(PUBLISHED, mock(), site, defaultRemoteId)

        // Assert
        assertThat(publishedActions).isEqualTo(expectedActions)
    }

    @Test
    fun `verify PUBLISHED actions contain HOMEPAGE settings when site has static homepage`() {
        // Arrange
        val expectedActions = setOf(
                VIEW_PAGE,
                SET_PARENT,
                SET_AS_HOMEPAGE,
                SET_AS_POSTS_PAGE,
                MOVE_TO_DRAFT,
                MOVE_TO_TRASH
        )
        site.showOnFront = ShowOnFront.PAGE.value

        // Act
        val publishedActions = useCase.setupPageActions(PUBLISHED, mock(), site, defaultRemoteId)

        // Assert
        assertThat(publishedActions).isEqualTo(expectedActions)
    }

    @Test
    fun `verify PUBLISHED actions does not contant HOMEPAGE settings when site has no remote id`() {
        // Arrange
        val expectedActions = setOf(
                VIEW_PAGE,
                SET_PARENT,
                MOVE_TO_DRAFT,
                MOVE_TO_TRASH
        )
        site.showOnFront = ShowOnFront.PAGE.value
        val remoteId = -1L

        // Act
        val publishedActions = useCase.setupPageActions(PUBLISHED, mock(), site, remoteId)

        // Assert
        assertThat(publishedActions).isEqualTo(expectedActions)
    }

    @Test
    fun `verify SCHEDULED actions`() {
        // Arrange
        val expectedActions = setOf(
                VIEW_PAGE,
                SET_PARENT,
                MOVE_TO_DRAFT,
                MOVE_TO_TRASH
        )

        // Act
        val scheduledActions = useCase.setupPageActions(SCHEDULED, mock(), site, defaultRemoteId)

        // Assert
        assertThat(scheduledActions).isEqualTo(expectedActions)
    }

    @Test
    fun `CANCEL_AUTO_UPLOAD is added to PublishedPage Actions if auto upload is pending`() {
        val actions = useCase.setupPageActions(PUBLISHED, UploadWaitingForConnection(mock()), site, defaultRemoteId)
        assertThat(actions).contains(CANCEL_AUTO_UPLOAD)
    }

    @Test
    fun `CANCEL_AUTO_UPLOAD is not added to PublishedPage Actions if auto upload is not pending`() {
        val actions = useCase.setupPageActions(PUBLISHED, UploadingPost(false), site, defaultRemoteId)
        assertThat(actions).doesNotContain(CANCEL_AUTO_UPLOAD)
    }

    @Test
    fun `CANCEL_AUTO_UPLOAD is added to DraftPage Actions if auto upload is pending`() {
        val actions = useCase.setupPageActions(DRAFTS, UploadWaitingForConnection(mock()), site, defaultRemoteId)
        assertThat(actions).contains(CANCEL_AUTO_UPLOAD)
    }

    @Test
    fun `CANCEL_AUTO_UPLOAD is not added to DraftPage Actions if auto upload is not pending`() {
        val actions = useCase.setupPageActions(DRAFTS, UploadingPost(true), site, defaultRemoteId)
        assertThat(actions).doesNotContain(CANCEL_AUTO_UPLOAD)
    }
}
