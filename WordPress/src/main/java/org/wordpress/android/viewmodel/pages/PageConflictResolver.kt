package org.wordpress.android.viewmodel.pages

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.PostConflictResolver

@Suppress("LongParameterList")
class PageConflictResolver(
    private val dispatcher: Dispatcher,
    private val site: SiteModel,
    private val postStore: PostStore,
    private val uploadStore: UploadStore,
    private val invalidateList: () -> Unit,
    private val checkNetworkConnection: () -> Boolean,
    private val showSnackBar: (SnackbarMessageHolder) -> Unit
) {
    private val postConflictResolver: PostConflictResolver by lazy {
        PostConflictResolver(
            dispatcher = dispatcher,
            site = site,
            getPostByLocalPostId = postStore::getPostByLocalPostId,
            invalidateList = invalidateList,
            checkNetworkConnection = checkNetworkConnection,
            showSnackBar = showSnackBar,
            uploadStore = uploadStore,
            postStore = postStore
        )
    }

    fun updateConflictedPageWithRemoteVersion(pageId: Int){
        postConflictResolver.updateConflictedPostWithRemoteVersion(pageId)
    }

    fun updateConflictedPageWithLocalVersion(pageId: Int){
        postConflictResolver.updateConflictedPostWithLocalVersion(pageId)
    }

    fun onPageSuccessfullyUpdated() {
        postConflictResolver.onPostSuccessfullyUpdated()
    }
}
