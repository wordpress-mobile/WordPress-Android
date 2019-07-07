package org.wordpress.android.ui.uploads

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ProcessLifecycleOwner
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.clearInvocations
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.ui.posts.PostUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import org.wordpress.android.viewmodel.helpers.ConnectionStatus.AVAILABLE
import org.wordpress.android.viewmodel.helpers.ConnectionStatus.UNAVAILABLE
import java.util.UUID
import kotlin.random.Random

@RunWith(MockitoJUnitRunner::class)
class LocalDraftUploadStarterTest {
    @get:Rule val rule = InstantTaskExecutorRule()

    private val sites = listOf(SiteModel(), SiteModel())
    private val sitesAndPosts: Map<SiteModel, List<PostModel>> = mapOf(
            sites[0] to listOf(createPostModel(), createPostModel()),
            sites[1] to listOf(
                    createPostModel(),
                    createPostModel(),
                    createPostModel(),
                    createPostModel(),
                    createPostModel()
            )
    )
    private val posts = sitesAndPosts.values.flatten()

    private val sitesAndPages: Map<SiteModel, List<PostModel>> = mapOf(
            sites[0] to listOf(createPostModel(), createPostModel()),
            sites[1] to listOf(createPostModel(), createPostModel(), createPostModel(), createPostModel())
    )
    private val pages = sitesAndPages.values.flatten()

    private val siteStore = mock<SiteStore> {
        on { sites } doReturn sites
    }
    private val postStore = mock<PostStore> {
        sites.forEach {
            on { getLocalDraftPosts(eq(it)) } doReturn sitesAndPosts.getValue(it)
        }
    }
    private val pageStore = mock<PageStore> {
        sites.forEach {
            onBlocking { getLocalDraftPages(eq(it)) } doReturn sitesAndPages.getValue(it)
        }
    }

    @Test
    fun `when the internet connection is restored and the app is in foreground, it uploads all local drafts`() {
        // Given
        val connectionStatus = createConnectionStatusLiveData(UNAVAILABLE)
        val uploadServiceFacade = createMockedUploadServiceFacade()

        // ON_RESUME -> app is in the foreground
        val lifecycle = LifecycleRegistry(mock()).apply { handleLifecycleEvent(Event.ON_RESUME) }

        val starter = createLocalDraftUploadStarter(connectionStatus, uploadServiceFacade)
        starter.activateAutoUploading(createMockedProcessLifecycleOwner(lifecycle))

        // we need to reset the uploadServiceFacade mock as when the app moves to ON_RESUME state (comes to foreground)
        // an automatic upload is initiated and we want to test whether changing connection while the app
        // is in the foreground initiates the upload.
        clearInvocations(uploadServiceFacade)
        // When
        connectionStatus.postValue(AVAILABLE)

        // Then
        verify(uploadServiceFacade, times(posts.size + pages.size)).uploadPost(
                context = any(),
                post = any(),
                trackAnalytics = any(),
                publish = any(),
                isRetry = eq(true)
        )
    }

    @Test
    fun `when the internet connection is restored and the app is in background it doesn't upload all local drafts`() {
        // Given
        val connectionStatus = createConnectionStatusLiveData(UNAVAILABLE)
        val uploadServiceFacade = createMockedUploadServiceFacade()

        // ON_CREATE -> app is in the background
        val lifecycle = LifecycleRegistry(mock()).apply { handleLifecycleEvent(Event.ON_CREATE) }

        val starter = createLocalDraftUploadStarter(connectionStatus, uploadServiceFacade)
        starter.activateAutoUploading(createMockedProcessLifecycleOwner(lifecycle))

        // When
        connectionStatus.postValue(AVAILABLE)

        // Then
        verify(uploadServiceFacade, times(0)).uploadPost(
                context = any(),
                post = any(),
                trackAnalytics = any(),
                publish = any(),
                isRetry = eq(true)
        )
    }

    @Test
    fun `when the app is placed in the foreground, it uploads all local drafts`() {
        // Given
        val connectionStatus = createConnectionStatusLiveData(AVAILABLE)
        val uploadServiceFacade = createMockedUploadServiceFacade()

        val lifecycle = LifecycleRegistry(mock()).apply { handleLifecycleEvent(Event.ON_CREATE) }

        val starter = createLocalDraftUploadStarter(connectionStatus, uploadServiceFacade)
        starter.activateAutoUploading(createMockedProcessLifecycleOwner(lifecycle))

        // When
        lifecycle.handleLifecycleEvent(Event.ON_START)

        // Then
        verify(uploadServiceFacade, times(posts.size + pages.size)).uploadPost(
                context = any(),
                post = any(),
                trackAnalytics = any(),
                publish = any(),
                isRetry = eq(true)
        )
    }

    @Test
    fun `when uploading a single site, only the local drafts of that site is uploaded`() {
        // Given
        val site: SiteModel = sites[1]

        val connectionStatus = createConnectionStatusLiveData(null)
        val uploadServiceFacade = createMockedUploadServiceFacade()

        val starter = createLocalDraftUploadStarter(connectionStatus, uploadServiceFacade)

        // When
        starter.queueUploadFromSite(site)

        // Then
        val expectedUploadPostExecutions = sitesAndPosts.getValue(site).size + sitesAndPages.getValue(site).size
        verify(uploadServiceFacade, times(expectedUploadPostExecutions)).uploadPost(
                context = any(),
                post = any(),
                trackAnalytics = any(),
                publish = any(),
                isRetry = eq(true)
        )
    }

    @Test
    fun `when uploading, it ignores local drafts that are not publishable`() {
        // Given
        val site: SiteModel = sites[1]

        val connectionStatus = createConnectionStatusLiveData(null)
        val uploadServiceFacade = createMockedUploadServiceFacade()
        val postUtilsWrapper = mock<PostUtilsWrapper> {
            on { isPublishable(any()) } doAnswer {
                // return isPublishable = false on the first post of the site
                it.getArgument<PostModel>(0) != sitesAndPosts[site]?.get(0)!!
            }
        }

        val starter = createLocalDraftUploadStarter(connectionStatus, uploadServiceFacade, postUtilsWrapper)

        // When
        starter.queueUploadFromSite(site)

        // Then
        // subtract - 1 as we've returned isPublishable = false for the first post of the site
        val expectedUploadPostExecutions = sitesAndPosts.getValue(site).size + sitesAndPages.getValue(site).size - 1
        verify(uploadServiceFacade, times(expectedUploadPostExecutions)).uploadPost(
                context = any(),
                post = any(),
                trackAnalytics = any(),
                publish = any(),
                isRetry = eq(true)
        )
    }

    @Test
    fun `when uploading, it ignores local drafts that are already queued`() {
        // Given
        val site: SiteModel = sites[1]
        val (expectedQueuedPosts, expectedUploadedPosts) = sitesAndPosts.getValue(site).let { posts ->
            // Split into halves of already queued and what should be uploaded
            return@let Pair(
                    posts.subList(0, posts.size / 2),
                    posts.subList(posts.size / 2, posts.size)
            )
        }
        val (expectedQueuedPages, expectedUploadedPages) = sitesAndPages.getValue(site).let { pages ->
            // Split into halves of already queued and what should be uploaded
            return@let Pair(
                    pages.subList(0, pages.size / 2),
                    pages.subList(pages.size / 2, pages.size)
            )
        }
        val expectedQueuedPostsAndPages = expectedQueuedPosts + expectedQueuedPages
        val expectedUploadPostsAndPages = expectedUploadedPosts + expectedUploadedPages

        val connectionStatus = createConnectionStatusLiveData(null)
        val uploadServiceFacade = mock<UploadServiceFacade> {
            on { isPostUploadingOrQueued(any()) } doAnswer {
                val post = it.arguments.first() as PostModel
                expectedQueuedPostsAndPages.contains(post)
            }
        }

        val starter = createLocalDraftUploadStarter(connectionStatus, uploadServiceFacade)

        // When
        starter.queueUploadFromSite(site)

        // Then
        verify(uploadServiceFacade, times(expectedUploadPostsAndPages.size)).uploadPost(
                context = any(),
                post = argWhere { expectedUploadPostsAndPages.contains(it) },
                trackAnalytics = any(),
                publish = any(),
                isRetry = eq(true)
        )
        verify(
                uploadServiceFacade,
                times(sitesAndPosts.getValue(site).size + sitesAndPages.getValue(site).size)
        ).isPostUploadingOrQueued(any())
        verifyNoMoreInteractions(uploadServiceFacade)
    }

    @Test
    fun `when uploading a single site, local drafts with too many errors or cancellations are not uploaded`() {
        // Given
        val site: SiteModel = sites[1]

        val connectionStatus = createConnectionStatusLiveData(null)
        val uploadServiceFacade = createMockedUploadServiceFacade()

        // This UploadStore.getNumberOfPostUploadErrorsOrCancellations mocked method will always return that
        // any post was cancelled 1000 times. The auto upload should not be started.
        val starter = createLocalDraftUploadStarter(connectionStatus, uploadServiceFacade,
                uploadStore = createMockedUploadStore(1000))

        // When
        starter.queueUploadFromSite(site)

        // Then
        // Make sure the uploadPost method is never called
        verify(uploadServiceFacade, never()).uploadPost(
                context = any(),
                post = any(),
                trackAnalytics = any(),
                publish = any(),
                isRetry = eq(true)
        )
    }

    @UseExperimental(ExperimentalCoroutinesApi::class)
    private fun createLocalDraftUploadStarter(
        connectionStatus: LiveData<ConnectionStatus>,
        uploadServiceFacade: UploadServiceFacade,
        postUtilsWrapper: PostUtilsWrapper = createMockedPostUtilsWrapper(),
        uploadStore: UploadStore = createMockedUploadStore(0)
    ) = LocalDraftUploadStarter(
            context = mock(),
            postStore = postStore,
            pageStore = pageStore,
            siteStore = siteStore,
            uploadStore = uploadStore,
            bgDispatcher = Dispatchers.Unconfined,
            ioDispatcher = Dispatchers.Unconfined,
            networkUtilsWrapper = createMockedNetworkUtilsWrapper(),
            postUtilsWrapper = postUtilsWrapper,
            connectionStatus = connectionStatus,
            uploadServiceFacade = uploadServiceFacade
    )

    private companion object Fixtures {
        fun createMockedNetworkUtilsWrapper() = mock<NetworkUtilsWrapper> {
            on { isNetworkAvailable() } doReturn true
        }

        fun createConnectionStatusLiveData(initialValue: ConnectionStatus?): MutableLiveData<ConnectionStatus> {
            return MutableLiveData<ConnectionStatus>().apply {
                value = initialValue
            }
        }

        fun createMockedPostUtilsWrapper() = mock<PostUtilsWrapper> {
            on { isPublishable(any()) } doReturn true
        }

        fun createMockedUploadStore(numberOfPostErrors: Int) = mock<UploadStore> {
            on { getNumberOfPostUploadErrorsOrCancellations(any()) } doReturn numberOfPostErrors
        }

        fun createMockedUploadServiceFacade() = mock<UploadServiceFacade> {
            on { isPostUploadingOrQueued(any()) } doReturn false
        }

        fun createMockedProcessLifecycleOwner(lifecycle: Lifecycle = mock()) = mock<ProcessLifecycleOwner> {
            on { this.lifecycle } doReturn lifecycle
        }

        fun createPostModel() = PostModel().apply {
            id = Random.nextInt()
            title = UUID.randomUUID().toString()
        }
    }
}
