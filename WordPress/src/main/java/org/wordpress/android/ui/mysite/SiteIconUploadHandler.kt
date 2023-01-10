package org.wordpress.android.ui.mysite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MY_SITE_ICON_UPLOADED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MY_SITE_ICON_UPLOAD_UNSUCCESSFUL
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.SiteIconUploadHandler.ItemUploadedModel.MediaUploaded
import org.wordpress.android.ui.mysite.SiteIconUploadHandler.ItemUploadedModel.PostUploaded
import org.wordpress.android.ui.uploads.UploadService.UploadErrorEvent
import org.wordpress.android.ui.uploads.UploadService.UploadMediaSuccessEvent
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MAIN
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class SiteIconUploadHandler
@Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val eventBusWrapper: EventBusWrapper
) {
    private val _onUploadedItem = MutableLiveData<Event<ItemUploadedModel>>()
    val onUploadedItem = _onUploadedItem as LiveData<Event<ItemUploadedModel>>

    init {
        eventBusWrapper.register(this)
    }

    fun clear() {
        eventBusWrapper.unregister(this)
    }

    sealed class ItemUploadedModel(open val site: SiteModel?, open val errorMessage: String? = null) {
        data class PostUploaded(
            val post: PostModel,
            override val site: SiteModel?,
            override val errorMessage: String? = null
        ) : ItemUploadedModel(site, errorMessage)

        data class MediaUploaded(
            val media: List<MediaModel>,
            override val site: SiteModel?,
            override val errorMessage: String? = null
        ) : ItemUploadedModel(site, errorMessage)
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: UploadErrorEvent) {
        analyticsTrackerWrapper.track(MY_SITE_ICON_UPLOAD_UNSUCCESSFUL)
        eventBusWrapper.removeStickyEvent(event)
        selectedSiteRepository.showSiteIconProgressBar(false)
        val selectedSite = selectedSiteRepository.getSelectedSite()
        if (selectedSite != null && event.post != null && event.post.localSiteId == selectedSite.id) {
            _onUploadedItem.postValue(Event(PostUploaded(event.post, selectedSite, event.errorMessage)))
        } else if (event.mediaModelList != null && event.mediaModelList.isNotEmpty()) {
            _onUploadedItem.postValue(Event(MediaUploaded(event.mediaModelList, selectedSite, event.errorMessage)))
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: UploadMediaSuccessEvent) {
        analyticsTrackerWrapper.track(MY_SITE_ICON_UPLOADED)
        eventBusWrapper.removeStickyEvent(event)
        val selectedSite = selectedSiteRepository.getSelectedSite()
        if (selectedSite != null) {
            if (selectedSiteRepository.isSiteIconUploadInProgress()) {
                if (event.mediaModelList.size > 0) {
                    val media = event.mediaModelList[0]
                    selectedSiteRepository.updateSiteIconMediaId(media.mediaId.toInt(), true)
                } else {
                    AppLog.w(
                        MAIN,
                        "Site icon upload completed, but mediaList is empty."
                    )
                }
            } else if (event.mediaModelList != null && event.mediaModelList.isNotEmpty()) {
                _onUploadedItem.postValue(
                    Event(
                        MediaUploaded(
                            event.mediaModelList,
                            selectedSite,
                            event.successMessage
                        )
                    )
                )
            }
        }
    }
}
