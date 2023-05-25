package org.wordpress.android.ui.uploads

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ProcessLifecycleOwner
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.ui.posts.PostUtilsWrapper
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import java.util.Date

internal object UploadFixtures {
    private var postIdIndex = 0

    private fun makePostTitleFromId() = postIdIndex.toString().padStart(2, '0')

    fun resetTestPostIdIndex() { postIdIndex = 0 }

    fun createMockedNetworkUtilsWrapper() = mock<NetworkUtilsWrapper> { on { isNetworkAvailable() } doReturn true }

    fun createConnectionStatusLiveData(initialValue: ConnectionStatus?): MutableLiveData<ConnectionStatus> {
        return MutableLiveData<ConnectionStatus>().apply { value = initialValue }
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

    fun createLocallyChangedPostModel(postStatus: PostStatus = PostStatus.DRAFT, page: Boolean = false) =
        PostModel().apply {
            setId(++postIdIndex)
            setTitle(makePostTitleFromId())
            setStatus(postStatus.toString())
            setIsLocallyChanged(true)
            setDateLocallyChanged(DateTimeUtils.iso8601FromTimestamp(Date().time / 1000))
            setIsPage(page)
        }

    fun createSiteModel(isWpCom: Boolean = true) = SiteModel().apply { setIsWPCom(isWpCom) }
}
