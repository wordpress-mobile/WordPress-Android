package org.wordpress.android.ui.posts

import android.content.Context
import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.model.post.PostStatus.PUBLISHED
import org.wordpress.android.fluxc.model.post.PostStatus.SCHEDULED
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Named

class EditPostPublishSettingsViewModel
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val resourceProvider: ResourceProvider,
    private val postSettingsUtils: PostSettingsUtils,
    private val context: Context
) : ScopedViewModel(mainDispatcher) {
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
    private val _onPublishedDateChanged = MutableLiveData<Calendar>()
    val onPublishedDateChanged: LiveData<Calendar> = _onPublishedDateChanged
    private val _onPostStatusChanged = MutableLiveData<PostStatus>()
    val onPostStatusChanged: LiveData<PostStatus> = _onPostStatusChanged
    private val _onPublishedLabelChanged = MutableLiveData<String>()
    val onPublishedLabelChanged: LiveData<String> = _onPublishedLabelChanged
    private val _onToast = MutableLiveData<String>()
    val onToast: LiveData<String> = _onToast

    fun start(postModel: PostModel?) {
        val startCalendar = postModel?.let { getCurrentPublishDateAsCalendar(postModel) } ?: Calendar.getInstance()
        year = startCalendar.get(Calendar.YEAR)
        month = startCalendar.get(Calendar.MONTH)
        day = startCalendar.get(Calendar.DAY_OF_MONTH)
        hour = startCalendar.get(Calendar.HOUR_OF_DAY)
        minute = startCalendar.get(Calendar.MINUTE)
        canPublishImmediately = PostUtils.shouldPublishImmediatelyOptionBeAvailable(postModel)
        postModel?.let {
            _onPublishedLabelChanged.postValue(postSettingsUtils.getPublishDateLabel(postModel, context))
        }
    }

    fun publishNow() {
        _onPublishedDateChanged.postValue(Calendar.getInstance())
    }

    fun onTimeSelected(selectedHour: Int, selectedMinute: Int) {
        this.hour = selectedHour
        this.minute = selectedMinute
        val calendar = Calendar.getInstance()
        calendar.set(year!!, month!!, day!!, hour!!, minute!!)
        _onPublishedDateChanged.postValue(calendar)
    }

    fun onDateSelected(year: Int, month: Int, dayOfMonth: Int) {
        this.year = year
        this.month = month
        this.day = dayOfMonth
        _onDatePicked.postValue(Event(Unit))
    }

    private fun getCurrentPublishDateAsCalendar(postModel: PostModel): Calendar {
        val calendar = Calendar.getInstance()
        val dateCreated = postModel.dateCreated
        // Set the currently selected time if available
        if (!TextUtils.isEmpty(dateCreated)) {
            calendar.time = DateTimeUtils.dateFromIso8601(dateCreated)
        }
        return calendar
    }

    fun updatePost(updatedDate: Calendar, post: PostModel?, context: Context) {
        post?.let {
            post.dateCreated = DateTimeUtils.iso8601FromDate(updatedDate.time)
            val initialPostStatus = PostStatus.fromPost(post)
            val isPublishDateInTheFuture = PostUtils.isPublishDateInTheFuture(post)
            var finalPostStatus = initialPostStatus
            if (initialPostStatus == DRAFT && isPublishDateInTheFuture) {
                // Posts that are scheduled have a `future` date for REST but their status should be set to `published` as
                // there is no `future` entry in XML-RPC (see PostStatus in FluxC for more info)
                finalPostStatus = PUBLISHED
            } else if (initialPostStatus == PUBLISHED && post.isLocalDraft()) {
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
                _onToast.postValue(resourceProvider.getString(string.editor_post_converted_back_to_draft))
            }
            post.status = finalPostStatus.toString()
            _onPostStatusChanged.postValue(finalPostStatus)
            val publishDateLabel = postSettingsUtils.getPublishDateLabel(post, context)
            _onPublishedLabelChanged.postValue(publishDateLabel)
        }
    }
}
