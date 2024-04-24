package org.wordpress.android.viewmodel.pages

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteHomepageSettings.ShowOnFront
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.SiteModel.ORIGIN_WPCOM_REST
import org.wordpress.android.ui.pages.PagesListAction.CANCEL_AUTO_UPLOAD
import org.wordpress.android.ui.pages.PagesListAction.COPY
import org.wordpress.android.ui.pages.PagesListAction.DELETE_PERMANENTLY
import org.wordpress.android.ui.pages.PagesListAction.MOVE_TO_DRAFT
import org.wordpress.android.ui.pages.PagesListAction.MOVE_TO_TRASH
import org.wordpress.android.ui.pages.PagesListAction.PROMOTE_WITH_BLAZE
import org.wordpress.android.ui.pages.PagesListAction.PUBLISH_NOW
import org.wordpress.android.ui.pages.PagesListAction.SET_AS_HOMEPAGE
import org.wordpress.android.ui.pages.PagesListAction.SET_AS_POSTS_PAGE
import org.wordpress.android.ui.pages.PagesListAction.SET_PARENT
import org.wordpress.android.ui.pages.PagesListAction.SHARE
import org.wordpress.android.ui.pages.PagesListAction.VIEW_PAGE
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.DRAFTS
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.PUBLISHED
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.SCHEDULED
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.TRASHED
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadWaitingForConnection
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadingPost

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
        val expectedActions = listOf(
            VIEW_PAGE,
            SET_PARENT,
            PUBLISH_NOW,
            COPY,
            SHARE,
            MOVE_TO_TRASH
        )

        // Act
        val draftActions = useCase.setupPageActions(DRAFTS, mock(), site, defaultRemoteId)

        // Assert
        assertThat(draftActions).isEqualTo(expectedActions)
    }

    @Test
    fun `Verify TRASH actions`() {
        // Arrange
        val expectedActions = listOf(MOVE_TO_DRAFT, DELETE_PERMANENTLY)

        // Act
        val trashedActions = useCase.setupPageActions(TRASHED, mock(), site, defaultRemoteId)

        // Assert
        assertThat(trashedActions).isEqualTo(expectedActions)
    }

    @Test
    fun `given no version conflicts, verify PUBLISHED actions`() {
        // Arrange
        val expectedActions = listOf(
            VIEW_PAGE,
            SET_PARENT,
            MOVE_TO_DRAFT,
            COPY,
            SHARE,
            MOVE_TO_TRASH,
        )

        // Act
        val publishedActions = useCase.setupPageActions(
            listType = PUBLISHED,
            uploadUiState = mock(),
            siteModel = site,
            remoteId = defaultRemoteId,
            isPageEligibleForBlaze = false,
            hasVersionConflict = false
        )

        // Assert
        assertThat(publishedActions).isEqualTo(expectedActions)
    }

    @Test
    fun `give version conflict, verify PUBLISHED actions`() {
        // Arrange
        val expectedActions = listOf(
            SET_PARENT,
            MOVE_TO_DRAFT,
            COPY,
            SHARE,
            MOVE_TO_TRASH,
        )

        // Act
        val publishedActions = useCase.setupPageActions(
            listType = PUBLISHED,
            uploadUiState = mock(),
            siteModel = site,
            remoteId = defaultRemoteId,
            isPageEligibleForBlaze = false,
            hasVersionConflict = true
        )

        // Assert
        assertThat(publishedActions).isEqualTo(expectedActions)
    }

    @Test
    fun `verify PUBLISHED actions contain HOMEPAGE settings when site has static homepage and is WPCom`() {
        // Arrange
        val expectedActions = listOf(
            VIEW_PAGE,
            SET_PARENT,
            SET_AS_HOMEPAGE,
            SET_AS_POSTS_PAGE,
            MOVE_TO_DRAFT,
            COPY,
            SHARE,
            MOVE_TO_TRASH
        )
        site.showOnFront = ShowOnFront.PAGE.value
        site.setIsWPCom(true)

        // Act
        val publishedActions = useCase.setupPageActions(PUBLISHED, mock(), site, defaultRemoteId)

        // Assert
        assertThat(publishedActions).isEqualTo(expectedActions)
    }

    @Test
    fun `verify PUBLISHED actions contain HOMEPAGE settings when site has static homepage and is Jetpack`() {
        // Arrange
        val expectedActions = listOf(
            VIEW_PAGE,
            SET_PARENT,
            SET_AS_HOMEPAGE,
            SET_AS_POSTS_PAGE,
            MOVE_TO_DRAFT,
            COPY,
            SHARE,
            MOVE_TO_TRASH,
        )
        site.showOnFront = ShowOnFront.PAGE.value
        site.setIsJetpackConnected(true)
        site.origin = ORIGIN_WPCOM_REST

        // Act
        val publishedActions = useCase.setupPageActions(PUBLISHED, mock(), site, defaultRemoteId)

        // Assert
        assertThat(publishedActions).isEqualTo(expectedActions)
    }

    @Test
    fun `verify PUBLISHED actions cannot set page as homepage when it is already set`() {
        // Arrange
        val expectedActions = listOf(
            VIEW_PAGE,
            SET_PARENT,
            SET_AS_POSTS_PAGE,
            COPY,
            SHARE
        )
        site.showOnFront = ShowOnFront.PAGE.value
        site.pageOnFront = defaultRemoteId
        site.setIsWPCom(true)

        // Act
        val publishedActions = useCase.setupPageActions(PUBLISHED, mock(), site, defaultRemoteId)

        // Assert
        assertThat(publishedActions).isEqualTo(expectedActions)
    }

    @Test
    fun `verify PUBLISHED actions cannot set page for posts when it is already set`() {
        // Arrange
        val expectedActions = listOf(
            VIEW_PAGE,
            SET_PARENT,
            SET_AS_HOMEPAGE,
            MOVE_TO_DRAFT,
            COPY,
            SHARE,
            MOVE_TO_TRASH
        )
        site.showOnFront = ShowOnFront.PAGE.value
        site.pageForPosts = defaultRemoteId
        site.setIsWPCom(true)

        // Act
        val publishedActions = useCase.setupPageActions(PUBLISHED, mock(), site, defaultRemoteId)

        // Assert
        assertThat(publishedActions).isEqualTo(expectedActions)
    }

    @Test
    fun `verify PUBLISHED actions does not contant HOMEPAGE settings when site has no remote id`() {
        // Arrange
        val expectedActions = listOf(
            VIEW_PAGE,
            SET_PARENT,
            COPY,
            SHARE
        )
        site.showOnFront = ShowOnFront.PAGE.value
        val remoteId = -1L

        // Act
        val publishedActions = useCase.setupPageActions(PUBLISHED, mock(), site, remoteId)

        // Assert
        assertThat(publishedActions).isEqualTo(expectedActions)
    }

    @Test
    fun `verify PUBLISHED actions does not contant HOMEPAGE settings when site is self hosted`() {
        // Arrange
        val expectedActions = listOf(
            VIEW_PAGE,
            SET_PARENT,
            MOVE_TO_DRAFT,
            COPY,
            SHARE,
            MOVE_TO_TRASH
        )
        site.showOnFront = ShowOnFront.PAGE.value
        site.setIsWPCom(false)
        site.setIsJetpackConnected(false)

        // Act
        val publishedActions = useCase.setupPageActions(PUBLISHED, mock(), site, defaultRemoteId)

        // Assert
        assertThat(publishedActions).isEqualTo(expectedActions)
    }

    @Test
    fun `verify SCHEDULED actions`() {
        // Arrange
        val expectedActions = listOf(
            VIEW_PAGE,
            SET_PARENT,
            MOVE_TO_DRAFT,
            SHARE,
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

    @Test
    fun `verify PUBLISHED actions contains PROMOTE_WITH_BLAZE when feature enabled`() {
        // Arrange
        val expectedActions = listOf(
            VIEW_PAGE,
            SET_PARENT,
            SET_AS_HOMEPAGE,
            SET_AS_POSTS_PAGE,
            MOVE_TO_DRAFT,
            COPY,
            SHARE,
            PROMOTE_WITH_BLAZE,
            MOVE_TO_TRASH
        )
        site.showOnFront = ShowOnFront.PAGE.value
        site.setIsWPCom(true)
        site.setIsJetpackConnected(true)

        // Act
        val publishedActions = useCase.setupPageActions(PUBLISHED, mock(), site, defaultRemoteId, true)

        // Assert
        assertThat(publishedActions).isEqualTo(expectedActions)
    }
}
