package org.wordpress.android.ui.reader.repository.usecases

import com.automattic.android.tracks.crashlogging.CrashLogging
import dagger.Reusable
import org.json.JSONArray
import org.json.JSONObject
import org.wordpress.android.BuildConfig
import org.wordpress.android.models.ReaderBlog
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.models.ReaderTagType.DEFAULT
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.JSONUtils
import org.wordpress.android.util.crashlogging.sendReportWithTag
import javax.inject.Inject

@Reusable
class ParseDiscoverCardsJsonUseCase @Inject constructor(
    private val crashLogging: CrashLogging
) {
    fun parsePostCard(postCardJson: JSONObject): ReaderPost {
        return ReaderPost.fromJson(postCardJson.getJSONObject(ReaderConstants.JSON_CARD_DATA))
    }

    fun parseSimplifiedPostCard(simplifiedPostCardjson: JSONObject): Pair<Long, Long> {
        val postDataJson = simplifiedPostCardjson.getJSONObject(ReaderConstants.JSON_CARD_DATA)
        val postId = postDataJson.optLong(ReaderConstants.POST_ID)
        val blogId = postDataJson.optLong(ReaderConstants.POST_SITE_ID)
        return Pair(blogId, postId)
    }

    fun parseSimplifiedRecommendedBlogsCard(recommendedBlogsCardJson: JSONObject): List<Pair<Long, Long>> {
        return recommendedBlogsCardJson.optJSONArray(ReaderConstants.JSON_CARD_DATA)
            ?.let { recommendedBlogsJson ->
                List(recommendedBlogsJson.length()) { index ->
                    val recommendedBlog = recommendedBlogsJson.getJSONObject(index)
                    Pair(
                        recommendedBlog.optLong(ReaderConstants.RECOMMENDED_BLOG_ID),
                        recommendedBlog.optLong(ReaderConstants.RECOMMENDED_FEED_ID)
                    )
                }
            }
            ?: emptyList()
    }

    fun parseInterestCard(interestCardJson: JSONObject?): ReaderTagList {
        val interestTags = ReaderTagList()
        if (interestCardJson == null) {
            return interestTags
        }
        val jsonInterests = interestCardJson.optJSONArray(ReaderConstants.JSON_CARD_DATA) ?: return interestTags
        for (i in 0 until jsonInterests.length()) {
            val interestJSONTag = jsonInterests.optJSONObject(i)
            if (interestJSONTag != null) {
                interestTags.add(parseInterestTag(interestJSONTag))
            } else {
                val errorMsg = "Error parsing reader interests $jsonInterests at index $i."
                if (BuildConfig.DEBUG) {
                    throw NullPointerException("Debug build crash: $errorMsg")
                } else {
                    crashLogging.sendReportWithTag(exception = NullPointerException(errorMsg), tag = AppLog.T.READER)
                }
            }
        }
        return interestTags
    }

    fun parseRecommendedBlogsCard(cardJson: JSONObject): List<ReaderBlog> {
        return cardJson.optJSONArray(ReaderConstants.JSON_CARD_DATA)
            ?.let { jsonRecommendedBlogs ->
                List(jsonRecommendedBlogs.length()) { index ->
                    ReaderBlog.fromJson(jsonRecommendedBlogs.getJSONObject(index))
                }
            }
            ?: emptyList()
    }

    fun parseNextPageHandle(jsonObject: JSONObject): String =
        jsonObject.getString(ReaderConstants.JSON_NEXT_PAGE_HANDLE)

    fun convertListOfJsonArraysIntoSingleJsonArray(jsons: List<String>): JSONArray {
        val arrays = jsons.map { JSONArray(it) }
        return concatArrays(arrays)
    }

    private fun parseInterestTag(interestJsonCard: JSONObject): ReaderTag {
        val tagTitle = JSONUtils.getStringDecoded(interestJsonCard, ReaderConstants.JSON_TAG_TITLE)
        val tagSlug = JSONUtils.getStringDecoded(interestJsonCard, ReaderConstants.JSON_TAG_SLUG)
        return ReaderTag(tagSlug, tagTitle, tagTitle, "", DEFAULT)
    }

    private fun concatArrays(arrays: List<JSONArray>): JSONArray {
        val result = JSONArray()
        for (item in arrays) {
            for (i in 0 until item.length()) {
                result.put(item[i])
            }
        }
        return result
    }
}
