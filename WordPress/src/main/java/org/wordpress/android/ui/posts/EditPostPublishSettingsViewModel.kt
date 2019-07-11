package org.wordpress.android.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.model.post.PostStatus.PUBLISHED
import org.wordpress.android.fluxc.model.post.PostStatus.SCHEDULED
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Calendar
import javax.inject.Inject

class EditPostPublishSettingsViewModel
@Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val postSettingsUtils: PostSettingsUtils,
    private val localeManagerWrapper: LocaleManagerWrapper,
    private val postModelProvider: EditPostModelProvider
) : ViewModel() {
    var canPublishImmediately: Boolean = false

    var year: Int? = null
        private set
    var month: Int? = null
        private set
    var day: Int? = null
        private set
    var hour: Int? = null
        private set
    var minute: Int? = null
        private set

    private val _onDatePicked = MutableLiveData<Event<Unit>>()
    val onDatePicked: LiveData<Event<Unit>> = _onDatePicked
    private val _onPostStatusChanged = MutableLiveData<PostStatus>()
    val onPostStatusChanged: LiveData<PostStatus> = _onPostStatusChanged
    private val _onPublishedLabelChanged = MutableLiveData<String>()
    val onPublishedLabelChanged: LiveData<String> = _onPublishedLabelChanged
    private val _onToast = MutableLiveData<Event<String>>()
    val onToast: LiveData<Event<String>> = _onToast

    fun onPostChanged(post: PostModel?) {
        val startCalendar = post?.let { getCurrentPublishDateAsCalendar() }
                ?: localeManagerWrapper.getCurrentCalendar()
        year = startCalendar.get(Calendar.YEAR)
        month = startCalendar.get(Calendar.MONTH)
        day = startCalendar.get(Calendar.DAY_OF_MONTH)
        hour = startCalendar.get(Calendar.HOUR_OF_DAY)
        minute = startCalendar.get(Calendar.MINUTE)
        canPublishImmediately = post?.let { PostUtils.shouldPublishImmediatelyOptionBeAvailable(it) } ?: false
        post?.let {
            _onPublishedLabelChanged.postValue(postSettingsUtils.getPublishDateLabel(it))
        }
    }

    fun publishNow() {
        updatePost(localeManagerWrapper.getCurrentCalendar())
    }

    fun onTimeSelected(selectedHour: Int, selectedMinute: Int) {
        this.hour = selectedHour
        this.minute = selectedMinute
        val calendar = localeManagerWrapper.getCurrentCalendar()
        calendar.set(year!!, month!!, day!!, hour!!, minute!!)
        updatePost(calendar)
    }

    fun onDateSelected(year: Int, month: Int, dayOfMonth: Int) {
        this.year = year
        this.month = month
        this.day = dayOfMonth
        _onDatePicked.postValue(Event(Unit))
    }

    private fun getCurrentPublishDateAsCalendar(): Calendar {
        val calendar = localeManagerWrapper.getCurrentCalendar()
        val dateCreated = postModelProvider.postModel?.dateCreated
        // Set the currently selected time if available
        if (!dateCreated.isNullOrEmpty()) {
            calendar.time = DateTimeUtils.dateFromIso8601(dateCreated)
        }
        return calendar
    }

    private fun updatePost(updatedDate: Calendar) {
        postModelProvider.setDateCreated(DateTimeUtils.iso8601FromDate(updatedDate.time))
        val initialPostStatus = postModelProvider.getStatus()
        val isPublishDateInTheFuture = PostUtils.isPublishDateInTheFuture(postModelProvider.postModel)
        var finalPostStatus = initialPostStatus
        if (initialPostStatus == DRAFT && isPublishDateInTheFuture) {
            // The previous logic was setting the status twice, once from draft to published and when the user
            // picked the time, it set it from published to scheduled. This is now done in one step.
            finalPostStatus = SCHEDULED
        } else if (initialPostStatus == PUBLISHED && postModelProvider.postModel?.isLocalDraft == true) {
            // if user was changing dates for a local draft (not saved yet), only way to have it set to PUBLISH
            // is by running into the if case above. So, if they're updating the date again by calling
            // `updatePublishDate()`, get it back to DRAFT.
            finalPostStatus = DRAFT
        } else if (initialPostStatus == SCHEDULED && !isPublishDateInTheFuture) {
            // if this is a SCHEDULED post and the user is trying to Back-date it now, let's update it to DRAFT.
            // The other option was to make it published immediately but, let the user actively do that rather than
            // having the app be smart about it - we don't want to accidentally publish a post.
            finalPostStatus = DRAFT
            // show toast only once, when time is shown
            _onToast.postValue(Event(resourceProvider.getString(R.string.editor_post_converted_back_to_draft)))
        }
        postModelProvider.setStatus(finalPostStatus)
        _onPostStatusChanged.postValue(finalPostStatus)
        val publishDateLabel = postModelProvider.postModel?.let { postSettingsUtils.getPublishDateLabel(it) }
        _onPublishedLabelChanged.postValue(publishDateLabel)
    }
}
