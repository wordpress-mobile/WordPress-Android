package org.wordpress.android.ui.posts

import android.content.Context
import android.text.TextUtils
import android.text.format.DateUtils
import android.util.Log
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class PostSettingsUtils
@Inject constructor(private val resourceProvider: ResourceProvider) {
    fun getPublishDateLabel(
        postModel: PostModel,
        context: Context
    ): String {
        val labelToUse: String
        val dateCreated = postModel.dateCreated
        if (!TextUtils.isEmpty(dateCreated)) {
            val formattedDate = DateUtils.formatDateTime(
                    context,
                    DateTimeUtils.timestampFromIso8601Millis(dateCreated),
                    getDateTimeFlags()
            )

            val status = PostStatus.fromPost(postModel)
            if (status == PostStatus.SCHEDULED) {
                labelToUse = resourceProvider.getString(R.string.scheduled_for, formattedDate)
            } else if (status == PostStatus.PUBLISHED || status == PostStatus.PRIVATE) {
                labelToUse = resourceProvider.getString(R.string.published_on, formattedDate)
            } else if (postModel.isLocalDraft) {
                if (PostUtils.isPublishDateInThePast(postModel)) {
                    labelToUse = resourceProvider.getString(R.string.backdated_for, formattedDate)
                } else if (PostUtils.shouldPublishImmediately(postModel)) {
                    labelToUse = resourceProvider.getString(R.string.immediately)
                } else {
                    labelToUse = resourceProvider.getString(R.string.publish_on, formattedDate)
                }
            } else if (PostUtils.isPublishDateInTheFuture(postModel)) {
                labelToUse = resourceProvider.getString(R.string.schedule_for, formattedDate)
            } else {
                labelToUse = resourceProvider.getString(R.string.publish_on, formattedDate)
            }
        } else if (PostUtils.shouldPublishImmediatelyOptionBeAvailable(postModel)) {
            labelToUse = resourceProvider.getString(R.string.immediately)
        } else {
            // TODO: What should the label be if there is no specific date and this is not a DRAFT?
            labelToUse = ""
        }
        Log.d("vojta", "Updated label: $labelToUse")
        return labelToUse
    }

    private fun getDateTimeFlags(): Int {
        var flags = 0
        flags = flags or DateUtils.FORMAT_SHOW_DATE
        flags = flags or DateUtils.FORMAT_ABBREV_MONTH
        flags = flags or DateUtils.FORMAT_SHOW_YEAR
        flags = flags or DateUtils.FORMAT_SHOW_TIME
        return flags
    }
}
