package org.wordpress.android.viewmodel.reader

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.news.NewsItem
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.reblog.NoSite
import org.wordpress.android.ui.reader.reblog.PostEditor
import org.wordpress.android.ui.reader.reblog.SitePicker
import org.wordpress.android.ui.reader.reblog.Unknown
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListViewModel
import org.wordpress.android.util.BuildConfig

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderPostListViewModelTest {
    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Mock private lateinit var observer: Observer<NewsItem>
    @Mock private lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock private lateinit var accountStore: AccountStore
    @Mock private lateinit var readerTracker: ReaderTracker
    @Mock private lateinit var siteStore: SiteStore

    private lateinit var viewModel: ReaderPostListViewModel
    private val liveData = MutableLiveData<NewsItem>()

    @Before
    fun setUp() {
        viewModel = ReaderPostListViewModel(readerTracker, siteStore)
    }

    @Test
    fun `when user has no visible WPCOM site the no site flow is triggered`() {
        val post = ReaderPost()
        val visibleWPComSites = listOf<SiteModel>() // No sites

        whenever(siteStore.visibleSitesAccessedViaWPCom).thenReturn(visibleWPComSites)

        viewModel.onReblogButtonClicked(post)

        val state = viewModel.reblogState.value?.peekContent()
        assertThat(state).isEqualTo(NoSite)
    }

    @Test
    fun `when user has only one visible WPCOM site the post editor is triggered`() {
        val site = SiteModel()
        val post = ReaderPost()
        val visibleWPComSites = listOf(site) // One site

        whenever(siteStore.visibleSitesAccessedViaWPCom).thenReturn(visibleWPComSites)

        viewModel.onReblogButtonClicked(post)

        val state = viewModel.reblogState.value?.peekContent()
        assertThat(state).isInstanceOf(PostEditor::class.java)

        val peState = state as? PostEditor
        assertThat(peState?.site).isEqualTo(site)
        assertThat(peState?.post).isEqualTo(post)
    }

    @Test
    fun `when user has more than one visible WPCOM sites the site picker is triggered`() {
        val site = SiteModel()
        val post = ReaderPost()
        val visibleWPComSites = listOf(site, site) // More sites

        whenever(siteStore.visibleSitesAccessedViaWPCom).thenReturn(visibleWPComSites)

        viewModel.onReblogButtonClicked(post)

        val state = viewModel.reblogState.value?.peekContent()
        assertThat(state).isInstanceOf(SitePicker::class.java)

        val spState = state as? SitePicker
        assertThat(spState?.site).isEqualTo(site)
        assertThat(spState?.post).isEqualTo(post)
    }

    @Test
    fun `when having more than one visible WPCOM sites and selecting site to reblog the post editor is triggered`() {
        val siteId = 1
        val site = SiteModel()
        val post = ReaderPost()
        val visibleWPComSites = listOf(site, site) // More sites

        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(site)
        whenever(siteStore.visibleSitesAccessedViaWPCom).thenReturn(visibleWPComSites)

        viewModel.onReblogButtonClicked(post)
        viewModel.onReblogSiteSelected(siteId)

        val state = viewModel.reblogState.value?.peekContent()
        assertThat(state).isInstanceOf(PostEditor::class.java)

        val peState = state as? PostEditor
        assertThat(peState?.site).isEqualTo(site)
        assertThat(peState?.post).isEqualTo(post)
    }

    @Test
    fun `when user has only one visible WPCOM site but the selected site is not retrieved an error occurs`() {
        val post = ReaderPost()
        val visibleWPComSites = listOf(null) // One site

        whenever(siteStore.visibleSitesAccessedViaWPCom).thenReturn(visibleWPComSites)

        viewModel.onReblogButtonClicked(post)

        val state = viewModel.reblogState.value?.peekContent()
        assertThat(state).isInstanceOf(Unknown::class.java)
    }

    @Test
    fun `when user has more than one visible WPCOM sites but the selected site is not retrieved an error occurs`() {
        val post = ReaderPost()
        val visibleWPComSites = listOf(null, null) // More sites

        whenever(siteStore.visibleSitesAccessedViaWPCom).thenReturn(visibleWPComSites)

        viewModel.onReblogButtonClicked(post)

        val state = viewModel.reblogState.value?.peekContent()
        assertThat(state).isInstanceOf(Unknown::class.java)
    }

    @Test
    fun `when user selects a visible WPCOM site and the state is unexpected an error is thrown`() {
        val reblog = { viewModel.onReblogSiteSelected(1) }
        if (BuildConfig.DEBUG) {
            assertThatIllegalStateException().isThrownBy(reblog)
        } else {
            reblog()
            val state = viewModel.reblogState.value?.peekContent()
            assertThat(state).isInstanceOf(Unknown::class.java)
        }
    }
}
