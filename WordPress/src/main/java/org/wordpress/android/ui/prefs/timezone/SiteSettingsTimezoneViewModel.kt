package org.wordpress.android.ui.prefs.timezone

import android.app.Activity
import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.wordpress.android.Constants
import org.wordpress.android.networking.RestClientUtils
import org.wordpress.android.ui.prefs.timezone.TimezonesList.TimezoneHeader
import org.wordpress.android.ui.prefs.timezone.TimezonesList.TimezoneItem
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.SETTINGS
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle.FULL
import java.time.zone.ZoneRulesException
import java.util.Locale
import javax.inject.Inject

class SiteSettingsTimezoneViewModel @Inject constructor() : ViewModel() {
    private val timezonesList = mutableListOf<TimezonesList>()

    private val _showEmpty = MutableLiveData<Boolean>()
    val showEmptyView: LiveData<Boolean> = _showEmpty

    private val _showProgress = MutableLiveData<Boolean>()
    val showProgressView: LiveData<Boolean> = _showProgress

    private val _dismiss = MutableLiveData<Unit>()
    val dismissWithError: LiveData<Unit> = _dismiss

    private val searchInput = MutableLiveData<String>()
    val timezoneSearch: LiveData<List<TimezonesList>> = Transformations.switchMap(searchInput) { term ->
        filterTimezones(term)
    }

    private val _suggestedTimezone = MutableLiveData<String>()
    val suggestedTimezone = _suggestedTimezone

    private val _timezones = MutableLiveData<List<TimezonesList>>()
    val timezones = _timezones


    fun searchTimezones(query: CharSequence?) {
        searchInput.value = query.toString()
    }

    private fun filterTimezones(query: String): LiveData<List<TimezonesList>> {
        val filteredTimezones = MutableLiveData<List<TimezonesList>>()

        timezonesList.filter { timezone ->
            when (timezone) {
                is TimezoneItem -> {
                    timezone.label.contains(query, true) or timezone.offset.contains(query, true)
                }
                else -> false
            }
        }.also {
            filteredTimezones.value = it
        }

        return filteredTimezones
    }

    fun onSearchCancelled() {
        _timezones.postValue(timezonesList)
    }

    fun getTimezones(context: Activity) {
        _suggestedTimezone.postValue(ZoneId.systemDefault().id)

        requestTimezones(context)
    }

    private fun requestTimezones(context: Activity) {
        val listener = Response.Listener { response: String? ->
            AppLog.d(SETTINGS, "timezones requested")
            _showProgress.postValue(false)

            if (!TextUtils.isEmpty(response)) {
                timezonesList.clear()
                loadTimezones(response)
            } else {
                AppLog.w(SETTINGS, "empty response requesting timezones")
                _dismiss.postValue(Unit)
            }
        }

        val errorListener = Response.ErrorListener { error: VolleyError? ->
            AppLog.e(SETTINGS, "Error requesting timezones", error)
            _dismiss.postValue(Unit)
        }

        val request: StringRequest = object : StringRequest(Constants.URL_TIMEZONE_ENDPOINT, listener, errorListener) {
            override fun getParams(): Map<String, String> {
                return RestClientUtils.getRestLocaleParams(context)
            }
        }

        _showProgress.postValue(true)
        val queue = Volley.newRequestQueue(context)
        queue.add(request)
    }

    private fun loadTimezones(responseJson: String?) {
        try {
            val jsonResponse = JSONObject(responseJson.orEmpty())
            val jsonTimezonesByContinent = jsonResponse.getJSONObject("timezones_by_continent")
            val jsonTimezonesManualOffsets = jsonResponse.getJSONArray("manual_utc_offsets")

            Continents.values().map {
                timezonesList.add(TimezoneHeader(it.s))
                addTimezoneItems(jsonTimezonesByContinent.getJSONArray(it.s))
            }

            timezonesList.add(TimezoneHeader("Manual Offsets"))
            addManualTimezoneItems(jsonTimezonesManualOffsets)

            AppLog.d(SETTINGS, timezonesList.toString())
            _timezones.postValue(timezonesList)
        } catch (e: JSONException) {
            AppLog.e(SETTINGS, "Error parsing timezones", e)
            _dismiss.postValue(Unit)
        }
    }

    private fun addTimezoneItems(jsonTimezones: JSONArray) {
        for (i in 0 until jsonTimezones.length()) {
            val timezone = jsonTimezones.getJSONObject(i)
            val city = timezone.getString("label")
            val zone = timezone.getString("value")
            val zoneOffset = getZoneOffset(zone)
            val zoneTime = getTimeAtZone(zone)
            timezonesList.add(TimezoneItem(city, zone, zoneOffset, zoneTime))
        }
    }

    private fun addManualTimezoneItems(jsonTimezones: JSONArray) {
        for (i in 0 until jsonTimezones.length()) {
            val timezone = jsonTimezones.getJSONObject(i)
            timezonesList.add(TimezoneItem(timezone.getString("label"), timezone.getString("value")))
        }
    }

    private fun getZoneOffset(zone: String): String {
        val zoneId = getZoneId(zone)

        return if (zoneId != null) {
            val offset = ZonedDateTime.now(zoneId).offset
            val offsetDisplay = if (offset.id == "Z") "" else "(GMT$offset)"
            "${zoneId.getDisplayName(FULL, Locale.getDefault())} $offsetDisplay"
        } else {
            ""
        }
    }

    private fun getTimeAtZone(zone: String): String {
        val dateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
        val zoneId = getZoneId(zone)

        return if (zoneId != null) {
            val zoneDateTime = ZonedDateTime.now(zoneId)
            zoneDateTime.format(dateTimeFormatter)
        } else  {
            ""
        }
    }

    private fun getZoneId(zone: String): ZoneId? {
        return try {
            ZoneId.of(zone)
        } catch (e: ZoneRulesException) {
            AppLog.e(SETTINGS, "Error parsing zoneId", e)
            null
        }
    }

    enum class Continents(val s: String) {
        AFRICA("Africa"),
        AMERICA("America"),
        ANTARCTICA("Antarctica"),
        ARCTIC("Arctic"),
        ASIA("Asia"),
        ATLANTIC("Atlantic"),
        AUSTRALIA("Australia"),
        EUROPE("Europe"),
        INDIAN("Indian"),
        PACIFIC("Pacific"),
    }
}

