package org.wordpress.android.ui.mysite.cards.post.mockdata

import com.google.gson.Gson
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.UTILS
import org.wordpress.android.viewmodel.ContextProvider
import java.io.IOException
import javax.inject.Inject

/**
 * This is a temporary json utils class to load mocked posts data from json file in the raw resource.
 */
class MockedDataJsonUtils @Inject constructor(
    private val contextProvider: ContextProvider
) {
    fun getJsonStringFromRawResource(resourceId: Int) = try {
        contextProvider.getContext().resources.openRawResource(resourceId)
                .bufferedReader().use { it.readText() }
    } catch (e: IOException) {
        AppLog.e(UTILS, "Could not load JSON file.")
        null
    }

    fun getMockedPostsDataFromJsonString(json: String): MockedPostsData {
        return Gson().fromJson(json, MockedPostsData::class.java) as MockedPostsData
    }
}
