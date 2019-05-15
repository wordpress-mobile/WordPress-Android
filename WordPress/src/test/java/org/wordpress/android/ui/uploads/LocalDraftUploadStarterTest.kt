package org.wordpress.android.ui.uploads

import android.arch.core.executor.testing.InstantTaskExecutorRule
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
    private val processLifecycleOwner = mock<ProcessLifecycleOwner> {
        on { lifecycle } doReturn mock()
    }

    @Test
    fun `when the internet connection is restored, it uploads all local drafts`() {
        // Given
        val networkUtilsWrapper = mock<NetworkUtilsWrapper> {
            on { isNetworkAvailable() } doReturn true
        }
        val connectionStatus = MutableLiveData<ConnectionStatus>().apply { value = UNAVAILABLE }
        val uploadServiceFacade = mock<UploadServiceFacade> {
            on { isPostUploadingOrQueued(any()) } doReturn false
        }

        val starter = LocalDraftUploadStarter(
                context = mock(),
                postStore = postStore,
                siteStore = siteStore,
                bgDispatcher = Dispatchers.Default,
                networkUtilsWrapper = networkUtilsWrapper,
                connectionStatus = connectionStatus,
                uploadServiceFacade = uploadServiceFacade
        )
        starter.startAutoUploads(processLifecycleOwner)

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

    private suspend fun LocalDraftUploadStarter.waitForAllCoroutinesToFinish() {
        val job = checkNotNull(coroutineContext[Job])
        job.children.forEach { it.join() }
    }
}
