package org.wordpress.android.ui.posts.editor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.XPostSiteModel
import org.wordpress.android.fluxc.store.XPostsResult
import org.wordpress.android.fluxc.store.XPostsStore
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import java.util.Date
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class XPostsCapabilityCheckerTest : BaseUnitTest() {
    @Mock
    lateinit var mockXPostsStore: XPostsStore

    @Mock
    lateinit var mockSite: SiteModel

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var currentTimeProvider: CurrentTimeProvider

    @InjectMocks
    lateinit var xPostsCapabilityChecker: XPostsCapabilityChecker

    @Test
    fun `if has xposts in db, is capable without re-fetching`() = test {
        whenever(mockXPostsStore.getXPostsFromDb(mockSite)).thenReturn(XPostsResult.dbResult(listOf(XPostSiteModel())))

        assertEquals(true, xPostsCapabilityChecker.isCapable(mockSite))
        verify(mockXPostsStore, never()).fetchXPosts(any())
    }

    @Test
    fun `if the no-xposts marker was confirmed within the TTL, is not capable without re-fetching`() = test {
        whenever(mockXPostsStore.getXPostsFromDb(mockSite)).thenReturn(XPostsResult.dbResult(emptyList()))
        whenever(currentTimeProvider.currentDate()).thenReturn(Date(NOW))
        whenever(appPrefsWrapper.getXPostsNoResultCheckedTimestamp(mockSite)).thenReturn(NOW - ONE_HOUR_MS)

        assertEquals(false, xPostsCapabilityChecker.isCapable(mockSite))
        verify(mockXPostsStore, never()).fetchXPosts(any())
    }

    @Test
    fun `if the no-xposts marker is stale, re-fetches and reflects the new api result`() = test {
        whenever(mockXPostsStore.getXPostsFromDb(mockSite)).thenReturn(XPostsResult.dbResult(emptyList()))
        whenever(currentTimeProvider.currentDate()).thenReturn(Date(NOW))
        whenever(appPrefsWrapper.getXPostsNoResultCheckedTimestamp(mockSite)).thenReturn(NOW - TWO_DAYS_MS)
        whenever(mockXPostsStore.fetchXPosts(mockSite)).thenReturn(XPostsResult.apiResult(listOf(XPostSiteModel())))

        assertEquals(true, xPostsCapabilityChecker.isCapable(mockSite))
        verify(mockXPostsStore).fetchXPosts(mockSite)
    }

    @Test
    fun `if no xposts and never checked, re-fetches and records the timestamp when still empty`() = test {
        whenever(mockXPostsStore.getXPostsFromDb(mockSite)).thenReturn(XPostsResult.dbResult(emptyList()))
        whenever(currentTimeProvider.currentDate()).thenReturn(Date(NOW))
        // getXPostsNoResultCheckedTimestamp defaults to 0 (never checked)
        whenever(mockXPostsStore.fetchXPosts(mockSite)).thenReturn(XPostsResult.apiResult(emptyList()))

        assertEquals(false, xPostsCapabilityChecker.isCapable(mockSite))
        verify(appPrefsWrapper).setXPostsNoResultCheckedTimestamp(mockSite, NOW)
    }

    @Test
    fun `if the recheck falls back to cached data, is not capable but records no timestamp`() = test {
        whenever(mockXPostsStore.getXPostsFromDb(mockSite)).thenReturn(XPostsResult.dbResult(emptyList()))
        whenever(currentTimeProvider.currentDate()).thenReturn(Date(NOW))
        // An empty result sourced from the db means the network call failed and fell back to the cache,
        // so it must not be recorded as a confirmed "no xposts".
        whenever(mockXPostsStore.fetchXPosts(mockSite)).thenReturn(XPostsResult.dbResult(emptyList()))

        assertEquals(false, xPostsCapabilityChecker.isCapable(mockSite))
        verify(appPrefsWrapper, never()).setXPostsNoResultCheckedTimestamp(any(), any())
    }

    @Test
    fun `if the stored timestamp is in the future, treats it as stale and re-fetches`() = test {
        whenever(mockXPostsStore.getXPostsFromDb(mockSite)).thenReturn(XPostsResult.dbResult(emptyList()))
        whenever(currentTimeProvider.currentDate()).thenReturn(Date(NOW))
        whenever(appPrefsWrapper.getXPostsNoResultCheckedTimestamp(mockSite)).thenReturn(NOW + ONE_HOUR_MS)
        whenever(mockXPostsStore.fetchXPosts(mockSite)).thenReturn(XPostsResult.apiResult(listOf(XPostSiteModel())))

        assertEquals(true, xPostsCapabilityChecker.isCapable(mockSite))
        verify(mockXPostsStore).fetchXPosts(mockSite)
    }

    @Test
    fun `if unknown in db and xposts in api response, is capable`() = test {
        whenever(mockXPostsStore.getXPostsFromDb(mockSite)).thenReturn(XPostsResult.Unknown)
        whenever(currentTimeProvider.currentDate()).thenReturn(Date(NOW))
        whenever(mockXPostsStore.fetchXPosts(mockSite)).thenReturn(XPostsResult.apiResult(listOf(XPostSiteModel())))

        assertEquals(true, xPostsCapabilityChecker.isCapable(mockSite))
    }

    @Test
    fun `if unknown in db and api response shows no xposts, is not capable and records timestamp`() = test {
        whenever(mockXPostsStore.getXPostsFromDb(mockSite)).thenReturn(XPostsResult.Unknown)
        whenever(currentTimeProvider.currentDate()).thenReturn(Date(NOW))
        whenever(mockXPostsStore.fetchXPosts(mockSite)).thenReturn(XPostsResult.apiResult(emptyList()))

        assertEquals(false, xPostsCapabilityChecker.isCapable(mockSite))
        verify(appPrefsWrapper).setXPostsNoResultCheckedTimestamp(mockSite, NOW)
    }

    @Test
    fun `if unknown in db and unknown api response, is capable and records no timestamp`() = test {
        whenever(mockXPostsStore.getXPostsFromDb(mockSite)).thenReturn(XPostsResult.Unknown)
        whenever(currentTimeProvider.currentDate()).thenReturn(Date(NOW))
        whenever(mockXPostsStore.fetchXPosts(mockSite)).thenReturn(XPostsResult.Unknown)

        assertEquals(true, xPostsCapabilityChecker.isCapable(mockSite))
        verify(appPrefsWrapper, never()).setXPostsNoResultCheckedTimestamp(any(), any())
    }

    companion object {
        private const val NOW = 1_000_000_000_000L
        private val ONE_HOUR_MS = TimeUnit.HOURS.toMillis(1)
        private val TWO_DAYS_MS = TimeUnit.DAYS.toMillis(2)
    }
}
