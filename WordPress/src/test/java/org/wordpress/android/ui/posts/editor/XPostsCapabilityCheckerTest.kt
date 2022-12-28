package org.wordpress.android.ui.posts.editor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.XPostsResult
import org.wordpress.android.fluxc.store.XPostsResult.Companion
import org.wordpress.android.fluxc.store.XPostsStore

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class XPostsCapabilityCheckerTest : BaseUnitTest() {
    @Mock
    lateinit var mockXPostsStore: XPostsStore
    @Mock
    lateinit var mockSite: SiteModel
    @InjectMocks
    lateinit var xPostsCapabilityChecker: XPostsCapabilityChecker

    @Test
    fun `if has xposts in db, is capable`() = test {
        testCapability(XPostsResult.dbResult(listOf(mock())), null, true)
    }

    @Test
    fun `if no xposts in db and xposts in api response, is capable`() = test {
        testCapability(XPostsResult.dbResult(emptyList()), Companion.apiResult(listOf(mock())), true)
    }

    @Test
    fun `if no xposts in db and api response shows no xposts, is not capable`() = test {
        testCapability(XPostsResult.dbResult(emptyList()), XPostsResult.apiResult(emptyList()), false)
    }

    @Test
    fun `if no xposts in db and unknown xposts api response, capable`() = test {
        testCapability(XPostsResult.dbResult(emptyList()), XPostsResult.Unknown, true)
    }

    @Test
    fun `if unknown xposts in db and xposts in api response, is capable`() = test {
        testCapability(XPostsResult.Unknown, XPostsResult.apiResult(listOf(mock())), true)
    }

    @Test
    fun `if unknown xposts in db and api response shows no xposts, is not capable`() = test {
        testCapability(XPostsResult.Unknown, XPostsResult.apiResult(emptyList()), false)
    }

    @Test
    fun `if unknown xposts in db and unknown xposts api response, is capable`() = test {
        testCapability(XPostsResult.Unknown, XPostsResult.Unknown, true)
    }

    private suspend fun testCapability(
        dbResponse: XPostsResult,
        apiResponse: XPostsResult?,
        expectedCapability: Boolean
    ) {
        whenever(mockXPostsStore.getXPostsFromDb(mockSite)).thenReturn(dbResponse)
        if (apiResponse != null) {
            whenever(mockXPostsStore.fetchXPosts(mockSite)).thenReturn(apiResponse)
        }
        val actualCapability = xPostsCapabilityChecker.isCapable(mockSite)
        assertEquals(expectedCapability, actualCapability)
    }
}
