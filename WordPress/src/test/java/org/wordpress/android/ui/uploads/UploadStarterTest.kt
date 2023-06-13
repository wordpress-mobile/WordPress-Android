package org.wordpress.android.ui.uploads

import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.UploadAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.model.post.PostStatus.PENDING
import org.wordpress.android.fluxc.model.post.PostStatus.PRIVATE
import org.wordpress.android.fluxc.model.post.PostStatus.PUBLISHED
import org.wordpress.android.fluxc.model.post.PostStatus.SCHEDULED
import org.wordpress.android.fluxc.model.post.PostStatus.UNKNOWN
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.ui.posts.PostUtilsWrapper
import org.wordpress.android.ui.uploads.UploadFixtures.createConnectionStatusLiveData
import org.wordpress.android.ui.uploads.UploadFixtures.createLocallyChangedPostModel
import org.wordpress.android.ui.uploads.UploadFixtures.createMockedNetworkUtilsWrapper
import org.wordpress.android.ui.uploads.UploadFixtures.createMockedPostUtilsWrapper
import org.wordpress.android.ui.uploads.UploadFixtures.createMockedProcessLifecycleOwner
import org.wordpress.android.ui.uploads.UploadFixtures.createMockedUploadServiceFacade
import org.wordpress.android.ui.uploads.UploadFixtures.createMockedUploadStore
import org.wordpress.android.ui.uploads.UploadFixtures.createSiteModel
import org.wordpress.android.ui.uploads.UploadFixtures.resetTestPostIdIndex
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import org.wordpress.android.viewmodel.helpers.ConnectionStatus.AVAILABLE
import org.wordpress.android.viewmodel.helpers.ConnectionStatus.UNAVAILABLE
import java.util.Date

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class UploadStarterTest : BaseUnitTest() {
    private val uploadServiceFacade = createMockedUploadServiceFacade()

    private val sites = listOf(createSiteModel(), createSiteModel())
    private val sitesAndDraftPosts: Map<SiteModel, List<PostModel>> = mapOf(
        sites[0] to listOf(
            createLocallyChangedPostModel(DRAFT),
            createLocallyChangedPostModel(PUBLISHED),
            createLocallyChangedPostModel(SCHEDULED),
            createLocallyChangedPostModel(SCHEDULED),
            createLocallyChangedPostModel(PENDING),
            createLocallyChangedPostModel(PRIVATE),
            createLocallyChangedPostModel(PUBLISHED),
            createLocallyChangedPostModel(UNKNOWN)
        ),
        sites[1] to listOf(
            createLocallyChangedPostModel(DRAFT),
            createLocallyChangedPostModel(DRAFT),
            createLocallyChangedPostModel(PUBLISHED),
            createLocallyChangedPostModel(SCHEDULED),
            createLocallyChangedPostModel(PENDING),
            createLocallyChangedPostModel(PRIVATE),
            createLocallyChangedPostModel(PRIVATE),
            createLocallyChangedPostModel(UNKNOWN)
        )
    )
    private val draftPosts = sitesAndDraftPosts.values.flatten()

    private val sitesAndDraftPages: Map<SiteModel, List<PostModel>> = mapOf(
        sites[0] to listOf(
            createLocallyChangedPostModel(DRAFT, page = true),
            createLocallyChangedPostModel(DRAFT, page = true),
            createLocallyChangedPostModel(PUBLISHED, page = true),
            createLocallyChangedPostModel(SCHEDULED, page = true),
            createLocallyChangedPostModel(PENDING, page = true),
            createLocallyChangedPostModel(PENDING, page = true),
            createLocallyChangedPostModel(PRIVATE, page = true),
            createLocallyChangedPostModel(UNKNOWN, page = true)
        ),
        sites[1] to listOf(
            createLocallyChangedPostModel(DRAFT, page = true),
            createLocallyChangedPostModel(PUBLISHED, page = true),
            createLocallyChangedPostModel(PUBLISHED, page = true),
            createLocallyChangedPostModel(SCHEDULED, page = true),
            createLocallyChangedPostModel(PENDING, page = true),
            createLocallyChangedPostModel(PRIVATE, page = true),
            createLocallyChangedPostModel(PRIVATE, page = true),
            createLocallyChangedPostModel(UNKNOWN, page = true)
        )
    )
    private val draftPages = sitesAndDraftPages.values.flatten()

    private val siteStoreDefaultMock = mock<SiteStore> { on { sites } doReturn sites }
    private val postStoreDefaultMock = mock<PostStore> {
        sites.forEach { on { getPostsWithLocalChanges(eq(it)) } doReturn sitesAndDraftPosts.getValue(it) }
    }

    private val pageStoreDefaultMock = mock<PageStore> {
        sites.forEach { onBlocking { getPagesWithLocalChanges(eq(it)) } doReturn sitesAndDraftPages.getValue(it) }
    }

    private lateinit var mutex: Mutex

    @Before
    fun setUp() {
        mutex = Mutex()
        resetTestPostIdIndex()
    }

    @Test
    fun `when the internet connection is restored and the app is in foreground, it uploads changed posts & pages`() {
        // Given
        val connectionStatus = createConnectionStatusLiveData(UNAVAILABLE)
        val uploadServiceFacade = createMockedUploadServiceFacade()

        // ON_RESUME -> app is in the foreground
        val lifecycle = LifecycleRegistry(mock()).apply { handleLifecycleEvent(Event.ON_RESUME) }

        val starter = createUploadStarter(connectionStatus, uploadServiceFacade)
        starter.activateAutoUploading(createMockedProcessLifecycleOwner(lifecycle))

        // we need to reset the uploadServiceFacade mock as when the app moves to ON_RESUME state (comes to foreground)
        // an automatic upload is initiated and we want to test whether changing connection while the app
        // is in the foreground initiates the upload.
        clearInvocations(uploadServiceFacade)
        // When
        connectionStatus.postValue(AVAILABLE)

        // Then
        verify(uploadServiceFacade, times(draftPosts.size + draftPages.size)).uploadPost(
            context = any(),
            post = any(),
            trackAnalytics = any()
        )
    }

    @Test
    fun `when the internet connection is restored and the app is in background it doesn't upload anything`() {
        // Given
        val connectionStatus = createConnectionStatusLiveData(UNAVAILABLE)
        val uploadServiceFacade = createMockedUploadServiceFacade()

        // ON_CREATE -> app is in the background
        val lifecycle = LifecycleRegistry(mock()).apply { handleLifecycleEvent(Event.ON_CREATE) }

        val starter = createUploadStarter(connectionStatus, uploadServiceFacade)
        starter.activateAutoUploading(createMockedProcessLifecycleOwner(lifecycle))

        // When
        connectionStatus.postValue(AVAILABLE)

        // Then
        verify(uploadServiceFacade, times(0)).uploadPost(
            context = any(),
            post = any(),
            trackAnalytics = any()
        )
    }

    @Test
    fun `when the app is placed in the foreground, it uploads locally changed posts & pages`() {
        // Given
        val connectionStatus = createConnectionStatusLiveData(AVAILABLE)
        val uploadServiceFacade = createMockedUploadServiceFacade()

        val lifecycle = LifecycleRegistry(mock()).apply { handleLifecycleEvent(Event.ON_CREATE) }

        val starter = createUploadStarter(connectionStatus, uploadServiceFacade)
        starter.activateAutoUploading(createMockedProcessLifecycleOwner(lifecycle))

        // When
        lifecycle.handleLifecycleEvent(Event.ON_START)

        // Then
        verify(uploadServiceFacade, times(draftPosts.size + draftPages.size)).uploadPost(
            context = any(),
            post = any(),
            trackAnalytics = any()
        )
    }

    @Test
    fun `when uploading all sites, posts & pages of the sites are uploaded`() {
        val starter = createUploadStarter()

        starter.queueUploadFromAllSites()

        val expectedUploadPostExecutions = draftPosts.size + draftPages.size
        verify(uploadServiceFacade, times(expectedUploadPostExecutions)).uploadPost(
            context = any(),
            post = any(),
            trackAnalytics = any()
        )
    }

    @Test
    fun `given a failure, when uploading all sites, all other posts & pages are uploaded`() = test {
        val starter = createUploadStarter()
        draftPages.first().let {
            whenever(uploadServiceFacade.uploadPost(any(), eq(it), any()))
                .thenThrow(CancellationException("Upload error in test for post: " + it.title))
        }

        starter.queueUploadFromAllSites()

        val expectedUploadPostExecutions = draftPosts.size + draftPages.size
        verify(uploadServiceFacade, times(expectedUploadPostExecutions)).uploadPost(
            context = any(),
            post = any(),
            trackAnalytics = any()
        )
    }

    @Test
    fun `given an unexpected mutex unlock, when uploading, then all other sites are uploaded`() = test {
        val sites = createSiteModel() to createSiteModel()
        val post = createLocallyChangedPostModel()
        val starter = createUploadStarter(
            postStore = mock {
                val posts = post to createLocallyChangedPostModel()
                on { getPostsWithLocalChanges(sites.first) } doReturn listOf(posts.first)
                on { getPostsWithLocalChanges(sites.second) } doReturn listOf(posts.second)
            },
            pageStore = mock { onBlocking { getPagesWithLocalChanges(any()) } doReturn emptyList() },
            siteStore = mock { on { this@on.sites } doReturn sites.toList() },
        )
        val jobs = mutableListOf<Job>()
         whenever(uploadServiceFacade.uploadPost(any(), eq(post), any())).thenAnswer { mutex.unlock() }
        jobs += launch {
            starter.queueUploadFromSite(sites.first)
        } // 0
        jobs += launch {
            delay(1500)
            starter.queueUploadFromSite(sites.second)
            delay(500)
        } // 1
        jobs[1].cancel()
        jobs += launch {
            starter.queueUploadFromAllSites()
        } // 2
        advanceUntilIdle()

        verify(uploadServiceFacade, times(4)).uploadPost(any(), any<PostModel>(), any())
    }

    @Test
    fun `when uploading a single site, only posts & pages of that site are uploaded`() {
        // Given
        val site: SiteModel = sites[1]

        val connectionStatus = createConnectionStatusLiveData(null)
        val uploadServiceFacade = createMockedUploadServiceFacade()

        val starter = createUploadStarter(connectionStatus, uploadServiceFacade)

        // When
        starter.queueUploadFromSite(site)

        // Then
        val expectedUploadPostExecutions = sitesAndDraftPosts.getValue(site).size + sitesAndDraftPages
            .getValue(site).size
        verify(uploadServiceFacade, times(expectedUploadPostExecutions)).uploadPost(
            context = any(),
            post = any(),
            trackAnalytics = any()
        )
    }

    @Test
    fun `when uploading, it ignores locally changed posts & pages that are not publishable`() {
        // Given
        val site: SiteModel = sites[1]

        val connectionStatus = createConnectionStatusLiveData(null)
        val uploadServiceFacade = createMockedUploadServiceFacade()
        val postUtilsWrapper = mock<PostUtilsWrapper> {
            on { isPublishable(any()) } doAnswer {
                // return isPublishable = false on the first post of the site
                it.getArgument<PostModel>(0) != sitesAndDraftPosts[site]?.get(0)!!
            }
        }

        val starter = createUploadStarter(connectionStatus, uploadServiceFacade, postUtilsWrapper)

        // When
        starter.queueUploadFromSite(site)

        // Then
        // subtract - 1 as we've returned isPublishable = false for the first post of the site
        val expectedUploadPostExecutions = sitesAndDraftPosts.getValue(site).size + sitesAndDraftPages
            .getValue(site).size - 1
        verify(uploadServiceFacade, times(expectedUploadPostExecutions)).uploadPost(
            context = any(),
            post = any(),
            trackAnalytics = any()
        )
    }

    @Test
    fun `when uploading, it ignores posts & pages that are already queued`() {
        // Given
        val site: SiteModel = sites[1]
        val (expectedQueuedPosts, expectedUploadedPosts) = sitesAndDraftPosts.getValue(site).let { posts ->
            // Split into halves of already queued and what should be uploaded
            return@let Pair(
                posts.subList(0, posts.size / 2),
                posts.subList(posts.size / 2, posts.size)
            )
        }
        val (expectedQueuedPages, expectedUploadedPages) = sitesAndDraftPages.getValue(site).let { pages ->
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

        val starter = createUploadStarter(connectionStatus, uploadServiceFacade)

        // When
        starter.queueUploadFromSite(site)

        // Then
        verify(uploadServiceFacade, times(expectedUploadPostsAndPages.size)).uploadPost(
            context = any(),
            post = argWhere { expectedUploadPostsAndPages.contains(it) },
            trackAnalytics = any()
        )
        verify(
            uploadServiceFacade,
            times(sitesAndDraftPosts.getValue(site).size + sitesAndDraftPages.getValue(site).size)
        ).isPostUploadingOrQueued(any())
        verifyNoMoreInteractions(uploadServiceFacade)
    }

    @Test
    fun `when uploading a single site, posts with too many errors or cancellations are not uploaded`() {
        // Given
        val site: SiteModel = sites[1]

        val connectionStatus = createConnectionStatusLiveData(null)
        val uploadServiceFacade = createMockedUploadServiceFacade()

        // This UploadStore.getNumberOfAutoUploadAttempts mocked method will always return that
        // any post was cancelled 1000 times. The auto upload should not be started.
        val starter = createUploadStarter(
            connectionStatus, uploadServiceFacade,
            uploadStore = createMockedUploadStore(1000)
        )

        // When
        starter.queueUploadFromSite(site)

        // Then
        // Make sure the uploadPost method is never called
        verify(uploadServiceFacade, never()).uploadPost(
            context = any(),
            post = any(),
            trackAnalytics = any()
        )
    }

    @Test
    fun `Do not invoke remote-auto-save on self-hosted sites`() = test {
        // Given
        val siteModel = createSiteModel(isWpCom = false)
        val postModel = createLocallyChangedPostModel()
        defaultSetup(siteModel, postModel)

        // When
        createUploadStarter().queueUploadFromSite(siteModel)

        // Then
        verify(uploadServiceFacade, never()).uploadPost(
            context = any(),
            post = any(),
            trackAnalytics = any()
        )
    }

    @Test
    fun `Invoke remote-auto-save on wp-com sites`() = test {
        // Given
        val siteModel = createSiteModel(isWpCom = true)
        val postModel = createLocallyChangedPostModel()
        defaultSetup(siteModel, postModel)

        // When
        createUploadStarter().queueUploadFromSite(siteModel)

        // Then
        verify(uploadServiceFacade, times(1)).uploadPost(
            context = any(),
            post = any(),
            trackAnalytics = any()
        )
    }

    @Test
    fun `Do not invoke remote auto save on posts older than 2 days`() = test {
        // Given
        val siteModel = createSiteModel()
        val postModel = createLocallyChangedPostModel()
        defaultSetup(siteModel, postModel)

        val twoDaysInSeconds = 60 * 60 * 24 * 2
        val twoDaysAgo = (Date().time / 1000) - twoDaysInSeconds
        postModel.setDateLocallyChanged(DateTimeUtils.iso8601FromTimestamp(twoDaysAgo))

        // When
        createUploadStarter().queueUploadFromSite(siteModel)

        // Then
        verify(uploadServiceFacade, never()).uploadPost(
            context = any(),
            post = any(),
            trackAnalytics = any()
        )
    }

    @Test
    fun `Invoke remote auto save on a post changed 1,99days ago`() = test {
        // Given
        val siteModel = createSiteModel()
        val postModel = createLocallyChangedPostModel()
        defaultSetup(siteModel, postModel)

        val twoDaysInSeconds = 60 * 60 * 24 * 2
        val twoDaysAgo = (Date().time / 1000) - twoDaysInSeconds
        postModel.setDateLocallyChanged(DateTimeUtils.iso8601FromTimestamp(twoDaysAgo + 99))

        // When
        createUploadStarter().queueUploadFromSite(siteModel)

        // Then
        verify(uploadServiceFacade, times(1)).uploadPost(
            context = any(),
            post = any(),
            trackAnalytics = any()
        )
    }

    @Test
    fun `Do not auto-upload a post which is in conflict with remote`() = test {
        // Given
        val siteModel = createSiteModel()
        val postModel = createLocallyChangedPostModel()
        defaultSetup(siteModel, postModel)

        val postUtilsWrapper = createMockedPostUtilsWrapper()
        whenever(postUtilsWrapper.isPostInConflictWithRemote(any())).thenReturn(true)

        // When
        createUploadStarter(postUtilsWrapper = postUtilsWrapper).queueUploadFromSite(siteModel)

        // Then
        verify(uploadServiceFacade, never()).uploadPost(
            context = any(),
            post = any(),
            trackAnalytics = any()
        )
    }

    @Test
    fun `Do not auto-upload a post which is being uploaded or pending upload`() = test {
        // Given
        val siteModel = createSiteModel()
        val postModel = createLocallyChangedPostModel()
        defaultSetup(siteModel, postModel)

        whenever(uploadServiceFacade.isPostUploadingOrQueued(any())).thenReturn(true)

        // When
        createUploadStarter().queueUploadFromSite(siteModel)

        // Then
        verify(uploadServiceFacade, never()).uploadPost(
            context = any(),
            post = any(),
            trackAnalytics = any()
        )
    }

    @Test
    fun `Do not remote-auto-save a post which has already been remote-auto=saved`() = test {
        // Given
        val siteModel = createSiteModel()
        val postModel = createLocallyChangedPostModel()
        defaultSetup(siteModel, postModel)

        // Set autoSaveModified to a newer date than dateLocallyChanged to indicate the changes were remotely-auto-saved
        postModel.setAutoSaveModified(
            DateTimeUtils.iso8601FromTimestamp(
                DateTimeUtils.timestampFromIso8601(
                    postModel.dateLocallyChanged
                ) + 99
            )
        )

        // When
        createUploadStarter().queueUploadFromSite(siteModel)

        // Then
        verify(uploadServiceFacade, never()).uploadPost(
            context = any(),
            post = any(),
            trackAnalytics = any()
        )
    }

    @Test
    fun `verify number of auto upload attempts count is incremented when upload invoked`() = test {
        // Given
        val siteModel = createSiteModel()
        val postModel = createLocallyChangedPostModel()
        defaultSetup(siteModel, postModel)
        val dispatcher: Dispatcher = mock()

        // When
        createUploadStarter(dispatcher = dispatcher).queueUploadFromSite(siteModel)

        // Then
        argumentCaptor<Action<PostModel>>().apply {
            verify(dispatcher, times(1)).dispatch(capture())
            Assertions.assertThat(allValues[0].payload).isEqualTo(postModel)
            Assertions.assertThat(allValues[0].type).isEqualTo(UploadAction.INCREMENT_NUMBER_OF_AUTO_UPLOAD_ATTEMPTS)
        }
    }

    private fun defaultSetup(siteModel: SiteModel, postModel: PostModel) = test {
        whenever(postStoreDefaultMock.getPostsWithLocalChanges(any())).thenReturn(listOf(postModel))
        whenever(pageStoreDefaultMock.getPagesWithLocalChanges(siteModel)).thenReturn(listOf())
    }

    private fun createUploadStarter(
        connectionStatus: LiveData<ConnectionStatus> = createConnectionStatusLiveData(null),
        uploadServiceFacade: UploadServiceFacade = this.uploadServiceFacade,
        postUtilsWrapper: PostUtilsWrapper = createMockedPostUtilsWrapper(),
        uploadStore: UploadStore = createMockedUploadStore(0),
        dispatcher: Dispatcher = mock(),
        postStore: PostStore = postStoreDefaultMock,
        pageStore: PageStore = pageStoreDefaultMock,
        siteStore: SiteStore = siteStoreDefaultMock,
    ) = run {
        UploadStarter(
            appContext = mock(),
            postStore = postStore,
            pageStore = pageStore,
            siteStore = siteStore,
            bgDispatcher = testDispatcher(),
            ioDispatcher = testDispatcher(),
            networkUtilsWrapper = createMockedNetworkUtilsWrapper(),
            connectionStatus = connectionStatus,
            uploadServiceFacade = uploadServiceFacade,
            uploadActionUseCase = UploadActionUseCase(uploadStore, postUtilsWrapper, uploadServiceFacade),
            tracker = mock(),
            dispatcher = dispatcher,
            mutex = mutex,
        )
    }
}
