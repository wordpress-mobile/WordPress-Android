package org.wordpress.android.ui.rs

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.rs.contentlist.ContentItemUiModel
import org.wordpress.android.ui.rs.contentlist.RsMenuAction
import org.wordpress.android.ui.rs.data.RsSiteRestClient

@ExperimentalCoroutinesApi
class RsAuthorNamesTest : BaseUnitTest(StandardTestDispatcher()) {
    @Mock lateinit var restClient: RsSiteRestClient

    private val resolved = mutableListOf<Pair<String, Map<Long, String>>>()

    @Before
    fun setUp() {
        resolved.clear()
    }

    private fun createAuthorNames() = RsAuthorNames<String>(
        scope = testScope(),
        restClient = restClient,
        postType = POST_TYPE,
        onNamesResolved = { tab, names -> resolved.add(tab to names) },
        ioDispatcher = testDispatcher(),
    )

    private fun site(isSingleUserSite: Boolean?) = SiteModel().apply { setIsSingleUserSite(isSingleUserSite) }

    private fun model(authorId: Long, name: String? = null) = ContentItemUiModel<RsMenuAction>(
        remoteId = authorId * 10,
        title = "",
        excerpt = "",
        date = "",
        authorId = authorId,
        authorDisplayName = name,
    )

    private suspend fun answerNames(names: Map<Long, String>) {
        whenever(restClient.fetchUserDisplayNames(anyOrNull(), any())).doSuspendableAnswer { names }
    }

    private suspend fun answerMultipleAuthors(isMultiple: Boolean?) {
        whenever(restClient.hasMultipleAuthors(anyOrNull(), any())).doSuspendableAnswer { isMultiple }
    }

    @Test
    fun `a site known to be single-user never looks anything up`() = test {
        val authorNames = createAuthorNames()

        authorNames.resolve(TAB, site(isSingleUserSite = true), listOf(model(ONE)))
        advanceUntilIdle()

        verify(restClient, never()).hasMultipleAuthors(anyOrNull(), any())
        verify(restClient, never()).fetchUserDisplayNames(anyOrNull(), any())
        assertThat(resolved).isEmpty()
    }

    @Test
    fun `an unknown site with several published authors resolves only the missing names`() = test {
        answerMultipleAuthors(true)
        answerNames(mapOf(ONE to NAME))
        val authorNames = createAuthorNames()
        val site = site(isSingleUserSite = null)

        authorNames.resolve(TAB, site, listOf(model(ONE), model(ONE), model(0L)))
        advanceUntilIdle()

        verify(restClient).hasMultipleAuthors(site, POST_TYPE)
        verify(restClient).fetchUserDisplayNames(eq(site), eq(listOf(ONE)))
        assertThat(resolved).containsExactly(TAB to mapOf(ONE to NAME))
    }

    @Test
    fun `an unknown site with one published author resolves nothing`() = test {
        answerMultipleAuthors(false)
        val authorNames = createAuthorNames()

        authorNames.resolve(TAB, site(isSingleUserSite = null), listOf(model(ONE)))
        advanceUntilIdle()

        verify(restClient, never()).fetchUserDisplayNames(anyOrNull(), any())
        assertThat(resolved).isEmpty()
    }

    @Test
    fun `rows by two authors resolve names even when the site counts one published author`() = test {
        answerMultipleAuthors(false)
        answerNames(mapOf(ONE to NAME))
        val authorNames = createAuthorNames()
        val site = site(isSingleUserSite = null)

        authorNames.resolve(TAB, site, listOf(model(ONE), model(TWO, "Known")))
        advanceUntilIdle()

        verify(restClient, never()).hasMultipleAuthors(anyOrNull(), any())
        verify(restClient).fetchUserDisplayNames(eq(site), eq(listOf(ONE)))
    }

    @Test
    fun `a failed author count is asked again on the next load`() = test {
        answerMultipleAuthors(null)
        val authorNames = createAuthorNames()
        val site = site(isSingleUserSite = null)
        authorNames.resolve(TAB, site, listOf(model(ONE)))
        advanceUntilIdle()

        answerMultipleAuthors(true)
        answerNames(mapOf(ONE to NAME))
        authorNames.resolve(TAB, site, listOf(model(ONE)))
        advanceUntilIdle()

        verify(restClient, times(2)).hasMultipleAuthors(site, POST_TYPE)
        assertThat(resolved).containsExactly(TAB to mapOf(ONE to NAME))
    }

    private companion object {
        const val TAB = "published"
        const val POST_TYPE = "post"
        const val ONE = 1L
        const val TWO = 2L
        const val NAME = "Jane Doe"
    }
}
