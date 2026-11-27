package org.wordpress.android.ui.commentsrs

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource.RsResult
import org.wordpress.android.ui.rs.WpRsRouting

@ExperimentalCoroutinesApi
class RsCommentQuickActionsTest : BaseUnitTest() {
    @Mock lateinit var wpRsRouting: WpRsRouting

    @Mock lateinit var commentsRsDataSource: CommentsRsDataSource

    private lateinit var quickActions: RsCommentQuickActions

    private val site = SiteModel().apply { id = SITE_LOCAL_ID }

    @Before
    fun setUp() {
        quickActions = RsCommentQuickActions(
            wpRsRouting = wpRsRouting,
            commentsRsDataSource = commentsRsDataSource,
            bgDispatcher = testDispatcher(),
            mainDispatcher = testDispatcher()
        )
    }

    @Test
    fun `canHandle follows the routing decision the comment screens make`() {
        whenever(wpRsRouting.canUseWpRs(site)).thenReturn(true)

        assertThat(quickActions.canHandle(site)).isTrue
    }

    @Test
    fun `canHandle is false for a site the rs screens do not serve`() {
        whenever(wpRsRouting.canUseWpRs(site)).thenReturn(false)

        assertThat(quickActions.canHandle(site)).isFalse
    }

    @Test
    fun `reply posts the text as a child of the notified comment`() = test {
        whenever(commentsRsDataSource.createReply(site, POST_ID, COMMENT_ID, REPLY_TEXT))
            .thenReturn(RsResult.Success)
        var result: Boolean? = null

        quickActions.reply(site, POST_ID, COMMENT_ID, REPLY_TEXT) { result = it }

        assertThat(result).isTrue
        verify(commentsRsDataSource).createReply(site, POST_ID, COMMENT_ID, REPLY_TEXT)
    }

    @Test
    fun `reply reports failure when the write fails`() = test {
        whenever(commentsRsDataSource.createReply(site, POST_ID, COMMENT_ID, REPLY_TEXT))
            .thenReturn(RsResult.Error(null))
        var result: Boolean? = null

        quickActions.reply(site, POST_ID, COMMENT_ID, REPLY_TEXT) { result = it }

        assertThat(result).isFalse
    }

    companion object {
        private const val SITE_LOCAL_ID = 1
        private const val COMMENT_ID = 77L
        private const val POST_ID = 12L
        private const val REPLY_TEXT = "thanks!"
    }
}
