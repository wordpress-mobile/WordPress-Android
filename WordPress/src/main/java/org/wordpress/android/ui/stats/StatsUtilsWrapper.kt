package org.wordpress.android.ui.stats

import android.content.Context
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class StatsUtilsWrapper
@Inject constructor(val resourceProvider: ResourceProvider) {
    fun getSinceLabelLowerCase(date: Date): String {
        return StatsUtils.getSinceLabel(resourceProvider, date).toLowerCase(Locale.getDefault())
    }

    fun openPostInReaderOrInAppWebview(context: Context, siteId: Long, postId: String, itemURL: String) {
        StatsUtils.openPostInReaderOrInAppWebview(
                context,
                siteId,
                postId,
                StatsConstants.ITEM_TYPE_POST,
                itemURL
        )
    }
}
