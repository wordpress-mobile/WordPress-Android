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
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.clearInvocations
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.UploadAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
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
import org.wordpress.android.test
import org.wordpress.android.ui.posts.PostUtilsWrapper
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import org.wordpress.android.viewmodel.helpers.ConnectionStatus.AVAILABLE
import org.wordpress.android.viewmodel.helpers.ConnectionStatus.UNAVAILABLE
import java.util.Date
import java.util.UUID
import kotlin.random.Random

@RunWith(MockitoJUnitRunner::class)
class UploadStarterTest {
    @get:Rule val rule = InstantTaskExecutorRule()

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

    private val siteStore = mock<SiteStore> {
        on { sites } doReturn sites
    }
    private val postStore = mock<PostStore> {
        sites.forEach {
            on { getPostsWithLocalChanges(eq(it)) } doReturn sitesAndDraftPosts.getValue(it)
        }
    }

    private val pageStore = mock<PageStore> {
        sites.forEach {
            onBlocking { getPagesWithLocalChanges(eq(it)) } doReturn sitesAndDraftPages.getValue(it)
        }
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

        // Set autosaveModified to a newer date than dateLocallyChanged to indicate the changes were remotely-auto-saved
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
        whenever(postStore.getPostsWithLocalChanges(any())).thenReturn(listOf(postModel))
        whenever(pageStore.getPagesWithLocalChanges(siteModel)).thenReturn(listOf())
    }

    @UseExperimental(ExperimentalCoroutinesApi::class)
    private fun createUploadStarter(
        connectionStatus: LiveData<ConnectionStatus> = createConnectionStatusLiveData(null),
        uploadServiceFacade: UploadServiceFacade = this.uploadServiceFacade,
        postUtilsWrapper: PostUtilsWrapper = createMockedPostUtilsWrapper(),
        uploadStore: UploadStore = createMockedUploadStore(0),
        dispatcher: Dispatcher = mock()
    ) = UploadStarter(
            context = mock(),
            postStore = postStore,
            pageStore = pageStore,
            siteStore = siteStore,
            bgDispatcher = Dispatchers.Unconfined,
            ioDispatcher = Dispatchers.Unconfined,
            networkUtilsWrapper = createMockedNetworkUtilsWrapper(),
            connectionStatus = connectionStatus,
            uploadServiceFacade = uploadServiceFacade,
            uploadActionUseCase = UploadActionUseCase(uploadStore, postUtilsWrapper, uploadServiceFacade),
            tracker = mock(),
            dispatcher = dispatcher
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
            on { isPostInConflictWithRemote(any()) } doReturn false
        }

        fun createMockedUploadStore(numberOfAutoUploadAttempts: Int) = mock<UploadStore> {
            on { getNumberOfPostAutoUploadAttempts(any()) } doReturn numberOfAutoUploadAttempts
        }

        fun createMockedUploadServiceFacade() = mock<UploadServiceFacade> {
            on { isPostUploadingOrQueued(any()) } doReturn false
        }

        fun createMockedProcessLifecycleOwner(lifecycle: Lifecycle = mock()) = mock<ProcessLifecycleOwner> {
            on { this.lifecycle } doReturn lifecycle
        }

        fun createLocallyChangedPostModel(postStatus: PostStatus = DRAFT, page: Boolean = false) = PostModel().apply {
            setId(Random.nextInt())
            setTitle(UUID.randomUUID().toString())
            setStatus(postStatus.toString())
            setIsLocallyChanged(true)
            setDateLocallyChanged(DateTimeUtils.iso8601FromTimestamp(Date().time / 1000))
            setIsPage(page)
        }

        fun createSiteModel(isWpCom: Boolean = true) = SiteModel().apply {
            setIsWPCom(isWpCom)
        }
    }
}
