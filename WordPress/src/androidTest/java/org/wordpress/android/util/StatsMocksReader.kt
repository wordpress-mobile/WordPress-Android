package org.wordpress.android.util

import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject

class StatsMocksReader {
    private fun readDayStatsGenericFile(fileName: String, lastObjectName: String, keyName: String, valueName: String):
            MutableList<StatsKeyValueData> {
        val todayMarker = "{{now format='yyyy-MM-dd'}}"
        val readString = this.readAssetsFile("mocks/mappings/wpcom/stats/$fileName.json")
        val wireMockJSON = JSONObject(readString)
        val arrayRaw = wireMockJSON
            .getJSONObject("response")
            .getJSONObject("jsonBody")
            .getJSONObject("days")
            .getJSONObject(todayMarker)
            .getJSONArray(lastObjectName)

        val listStripped: MutableList<StatsKeyValueData> = ArrayList<StatsKeyValueData>()

        for (i in 0 until arrayRaw.length()) {
            val item: JSONObject = arrayRaw.optJSONObject(i)
            listStripped.add(
                StatsKeyValueData(
                    item.optString(keyName),
                    item.optString(valueName)
                )
            )
        }

        return listStripped
    }

    fun readDayTopPostsToList(): MutableList<StatsKeyValueData> {
        return readDayStatsGenericFile(
            "stats_top-posts-day", "postviews", "title", "views"
        )
    }

    fun readDayTopReferrersToList(): MutableList<StatsKeyValueData> {
        return readDayStatsGenericFile(
            "stats_referrers-day", "groups", "name", "total"
        )
    }

    fun readDayClicksToList(): MutableList<StatsKeyValueData> {
        return readDayStatsGenericFile(
            "stats_clicks-day", "clicks", "name", "views"
        )
    }

    fun readDayAuthorsToList(): MutableList<StatsKeyValueData> {
        return readDayStatsGenericFile(
            "stats_top-authors-day", "authors", "name", "views"
        )
    }

    fun readDayCountriesToList(): MutableList<StatsKeyValueData> {
        val countriesList = readDayStatsGenericFile(
            "stats_country-views-day", "views", "country_code", "views"
        )

        // We need to translate the country code (e.g. "DE") from json
        // to a full country name (e.g. "Germany")
        for (item in countriesList) {
            item.key = countriesMap[item.key].toString()
        }

        return countriesList
    }

    fun readDayVideoPlaysToList(): MutableList<StatsKeyValueData> {
        return readDayStatsGenericFile(
            "stats_video-plays-day", "plays", "title", "plays"
        )
    }

    fun readDayFileDownloadsToList(): MutableList<StatsKeyValueData> {
        return readDayStatsGenericFile(
            "stats_file-downloads-day", "files", "filename", "downloads"
        )
    }

    private fun readAssetsFile(fileName: String): String {
        val appContext = InstrumentationRegistry.getInstrumentation().context
        return appContext.assets.open(fileName).bufferedReader().use { it.readText() }
    }
}
