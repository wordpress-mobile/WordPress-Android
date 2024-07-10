package org.wordpress.android.ui.comments.unified

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class UnrepliedCommentsUtilsTest : BaseUnitTest() {
    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    private lateinit var utils: UnrepliedCommentsUtils

    @Before
    fun setup() {
        utils = UnrepliedCommentsUtils(accountStore, selectedSiteRepository)
    }

    @Test
    fun `WHEN the selected site cannot be retrieved THEN return false`() {
        val comment: CommentEntity = mock()
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        val result = utils.isMyComment(comment)

        assertFalse(result)
    }

    @Test
    fun `WHEN a comment's author email matches a non-wpcom site's email THEN return true`() {
        val authorEmail = "author@email.com"
        val comment: CommentEntity = mock()
        val site: SiteModel = mock()
        whenever(comment.authorEmail).thenReturn(authorEmail)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(site.isUsingWpComRestApi).thenReturn(false)
        whenever(site.email).thenReturn(authorEmail)

        val result = utils.isMyComment(comment)

        assertTrue(result)
    }

    @Test
    fun `WHEN a non-wpcom site's email is null THEN return false`() {
        val comment: CommentEntity = mock()
        val site: SiteModel = mock()
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(site.isUsingWpComRestApi).thenReturn(false)
        whenever(site.email).thenReturn(null)

        val result = utils.isMyComment(comment)

        assertFalse(result)
    }

    @Test
    fun `WHEN a comment's author email matches a wpcom account email THEN return true`() {
        val authorEmail = "author@email.com"
        val comment: CommentEntity = mock()
        val site: SiteModel = mock()
        val account: AccountModel = mock()
        whenever(comment.authorEmail).thenReturn(authorEmail)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(site.isUsingWpComRestApi).thenReturn(true)
        whenever(accountStore.account).thenReturn(account)
        whenever(account.email).thenReturn(authorEmail)

        val result = utils.isMyComment(comment)

        assertTrue(result)
    }

    @Test
    fun `WHEN a wpcom account fails to be retrieved THEN return false`() {
        val comment: CommentEntity = mock()
        val site: SiteModel = mock()
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(site.isUsingWpComRestApi).thenReturn(true)
        whenever(accountStore.account).thenReturn(null)

        val result = utils.isMyComment(comment)

        assertFalse(result)
    }

    @Test
    fun `WHEN a wpcom account email is null THEN return false`() {
        val comment: CommentEntity = mock()
        val site: SiteModel = mock()
        val account: AccountModel = mock()
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(site.isUsingWpComRestApi).thenReturn(true)
        whenever(accountStore.account).thenReturn(account)
        whenever(account.email).thenReturn(null)

        val result = utils.isMyComment(comment)

        assertFalse(result)
    }
}
