package org.wordpress.android.ui.uploads

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.Lifecycle.Event
import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ProcessLifecycleOwner
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import org.wordpress.android.viewmodel.helpers.ConnectionStatus.AVAILABLE
import org.wordpress.android.viewmodel.helpers.ConnectionStatus.UNAVAILABLE

@RunWith(MockitoJUnitRunner::class)
class LocalDraftUploadStarterTest {
    @get:Rule val rule = InstantTaskExecutorRule()

    private val sites = listOf(SiteModel(), SiteModel())
    private val sitesAndPosts: Map<SiteModel, List<PostModel>> = mapOf(
            sites[0] to listOf(PostModel(), PostModel()),
            sites[1] to listOf(PostModel(), PostModel(), PostModel())
    )
    private val posts = sitesAndPosts.values.flatten()

    private val siteStore = mock<SiteStore> {
        on { sites } doReturn sites
    }
    private val postStore = mock<PostStore> {
        sites.forEach {
            on { getLocalDraftPosts(eq(it)) } doReturn sitesAndPosts[it]
        }
    }

    @Test
    fun `when the internet connection is restored, it uploads all local drafts`() {
        // Given
        val connectionStatus = createConnectionStatusLiveData(UNAVAILABLE)
        val uploadServiceFacade = createMockedUploadServiceFacade()

        val starter = createLocalDraftUploadStarter(
                connectionStatus = connectionStatus,
                uploadServiceFacade = uploadServiceFacade
        )
        starter.activateAutoUploading(createMockedProcessLifecycleOwner())

        // When
        runBlocking {
            connectionStatus.postValue(AVAILABLE)

            starter.waitForAllCoroutinesToFinish()
        }

        // Then
        verify(uploadServiceFacade, times(posts.size)).uploadPost(
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

        val starter = createLocalDraftUploadStarter(
                connectionStatus = connectionStatus,
                uploadServiceFacade = uploadServiceFacade
        )
        starter.activateAutoUploading(createMockedProcessLifecycleOwner(lifecycle))

        // When
        runBlocking {
            lifecycle.handleLifecycleEvent(Event.ON_START)

            starter.waitForAllCoroutinesToFinish()
        }

        // Then
        verify(uploadServiceFacade, times(posts.size)).uploadPost(
                context = any(),
                post = any(),
                trackAnalytics = any(),
                publish = any(),
                isRetry = eq(true)
        )
    }

    private suspend fun LocalDraftUploadStarter.waitForAllCoroutinesToFinish() {
        val job = checkNotNull(coroutineContext[Job])
        job.children.forEach { it.join() }
    }

    private fun createLocalDraftUploadStarter(
        connectionStatus: LiveData<ConnectionStatus>,
        uploadServiceFacade: UploadServiceFacade
    ) = LocalDraftUploadStarter(
            context = mock(),
            postStore = postStore,
            siteStore = siteStore,
            bgDispatcher = Dispatchers.Default,
            networkUtilsWrapper = createMockedNetworkUtilsWrapper(),
            connectionStatus = connectionStatus,
            uploadServiceFacade = uploadServiceFacade
    )

    private companion object Fixtures {
        fun createMockedNetworkUtilsWrapper() = mock<NetworkUtilsWrapper> {
            on { isNetworkAvailable() } doReturn true
        }

        fun createConnectionStatusLiveData(initialValue: ConnectionStatus) = MutableLiveData<ConnectionStatus>().apply {
            value = initialValue
        }

        fun createMockedUploadServiceFacade() = mock<UploadServiceFacade> {
            on { isPostUploadingOrQueued(any()) } doReturn false
        }

        fun createMockedProcessLifecycleOwner(lifecycle: Lifecycle = mock()) = mock<ProcessLifecycleOwner> {
            on { this.lifecycle } doReturn lifecycle
        }
    }
}
