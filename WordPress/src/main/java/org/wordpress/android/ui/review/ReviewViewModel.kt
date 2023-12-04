package org.wordpress.android.ui.review

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class ReviewViewModel @Inject constructor(private val appPrefsWrapper: AppPrefsWrapper) : ViewModel() {
    private val _launchReview = MutableLiveData<Event<Unit>>()
    val launchReview = _launchReview as LiveData<Event<Unit>>

    fun onPublishingPost(isFirstTimePublishing: Boolean) {
        if (!appPrefsWrapper.isInAppReviewsShown() && isFirstTimePublishing) {
            if (appPrefsWrapper.getPublishedPostCount() < TARGET_COUNT_POST_PUBLISHED) {
                appPrefsWrapper.incrementPublishedPostCount()
            }
            if (appPrefsWrapper.getPublishedPostCount() == TARGET_COUNT_POST_PUBLISHED) {
                _launchReview.value = Event(Unit)
                appPrefsWrapper.setInAppReviewsShown()
            }
        }
    }

    companion object {
        private const val TARGET_COUNT_POST_PUBLISHED = 2
    }
}
