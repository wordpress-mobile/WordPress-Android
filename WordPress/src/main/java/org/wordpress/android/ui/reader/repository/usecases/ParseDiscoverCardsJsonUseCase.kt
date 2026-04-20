package org.wordpress.android.ui.reader.repository.usecases

import dagger.Reusable
import org.json.JSONObject
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.ReaderConstants
import javax.inject.Inject

@Reusable
class ParseDiscoverCardsJsonUseCase @Inject constructor() {
    fun parsePostCard(postCardJson: JSONObject): ReaderPost {
        return ReaderPost.fromJson(postCardJson.getJSONObject(ReaderConstants.JSON_CARD_DATA))
    }

    fun parseNextPageHandle(jsonObject: JSONObject): String =
        jsonObject.optString(ReaderConstants.JSON_NEXT_PAGE_HANDLE)
}
