package org.wordpress.android.ui.stats.refresh.utils

import org.wordpress.android.util.RtlUtils
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class ContentDescriptionHelper
@Inject constructor(private val resourceProvider: ResourceProvider, private val rtlUtils: RtlUtils) {
    fun buildContentDescription(keyLabel: Int, key: Long): String {
        return when (rtlUtils.isRtl) {
            true -> "$key :${resourceProvider.getString(keyLabel)}"
            false -> "${resourceProvider.getString(keyLabel)}: $key"
        }
    }
}
