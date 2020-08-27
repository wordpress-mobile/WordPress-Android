package org.wordpress.android.ui.reader.repository.usecases

import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.NetworkUnavailable
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Started
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.UpdateAction
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.UpdateAction.REQUEST_NEWER
import org.wordpress.android.ui.reader.services.post.wrapper.ReaderPostServiceStarterWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class FetchPostsForTagUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val contextProvider: ContextProvider,
    private val readerPostServiceStarterWrapper: ReaderPostServiceStarterWrapper
) {
    fun fetch(readerTag: ReaderTag, updateAction: UpdateAction = REQUEST_NEWER): ReaderRepositoryCommunication {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            return NetworkUnavailable
        }

        readerPostServiceStarterWrapper.startServiceForTag(
            contextProvider.getContext(),
            readerTag,
            updateAction
        )

        return Started
    }
}
