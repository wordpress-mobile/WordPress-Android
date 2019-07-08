package org.wordpress.android.ui.posts

import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
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
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration.SHORT
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Named

class EditPostPublishedSettingsViewModel
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val resourceProvider: ResourceProvider,
    private val postSettingsUtils: PostSettingsUtils,
    private val context: Context
) : ScopedViewModel(mainDispatcher) {
    var canPublishImmediately: Boolean = false

    var year: Int? = null
    var month: Int? = null
    var day: Int? = null
    var hour: Int? = null
    var minute: Int? = null

    private val _onDatePicked = MutableLiveData<Unit>()
    val onDatePicked: LiveData<Unit> = _onDatePicked
    private val _onPublishedDateChanged = MutableLiveData<Calendar>()
    val onPublishedDateChanged: LiveData<Calendar> = _onPublishedDateChanged
    private val _onPostStatusChanged = MutableLiveData<PostStatus>()
    val onPostStatusChanged: LiveData<PostStatus> = _onPostStatusChanged
    private val _onPublishedLabelChanged = MutableLiveData<String>()
    val onPublishedLabelChanged: LiveData<String> = _onPublishedLabelChanged

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

    fun onTimeSelected() {
        val calendar = Calendar.getInstance()
        calendar.set(year!!, month!!, day!!, hour!!, minute!!)
        Log.d("vojta", "onTimeSelected: $hour:$minute")
        _onPublishedDateChanged.postValue(calendar)
    }

    fun onDateSelected() {
        Log.d("vojta", "onDateSelected: $day. $month. $year")
        _onDatePicked.postValue(Unit)
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
        Log.d("vojta", "updatePost $post")
        post?.let {
            Log.d("vojta", "updatePost")
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
                ToastUtils.showToast(
                        context,
                        resourceProvider.getString(string.editor_post_converted_back_to_draft),
                        SHORT,
                        Gravity.TOP
                )
            }
            post.status = finalPostStatus.toString()
            _onPostStatusChanged.postValue(finalPostStatus)
            val publishDateLabel = postSettingsUtils.getPublishDateLabel(post, context)
            Log.d("vojta", "updating label: $publishDateLabel")
            _onPublishedLabelChanged.postValue(publishDateLabel)
        }
    }
}
