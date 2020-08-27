package org.wordpress.android.ui.reader.services.update

import org.wordpress.android.WordPress
import org.wordpress.android.networking.RestClientUtils
import javax.inject.Inject

class TagUpdateClientUtilsProvider @Inject constructor() {
    fun getRestClientForTagUpdate(): RestClientUtils {
        return WordPress.getRestClientUtilsV1_2()
    }

    fun getTagUpdateEndpointURL(): String {
        return WordPress.getRestClientUtilsV1_2().restClient.endpointURL
    }

    fun getRestClientForInterestTags(): RestClientUtils {
        return WordPress.getRestClientUtilsV2()
    }
}
