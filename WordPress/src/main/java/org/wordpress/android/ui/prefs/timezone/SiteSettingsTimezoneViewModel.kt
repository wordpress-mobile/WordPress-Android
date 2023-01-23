package org.wordpress.android.ui.prefs.timezone

import android.content.Context
import android.text.TextUtils
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.wordpress.android.Constants
import org.wordpress.android.R
import org.wordpress.android.networking.RestClientUtils
import org.wordpress.android.ui.prefs.timezone.TimezonesList.TimezoneHeader
import org.wordpress.android.ui.prefs.timezone.TimezonesList.TimezoneItem
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.SETTINGS
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.SingleLiveEvent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class SiteSettingsTimezoneViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val timezonesList = mutableListOf<TimezonesList>()

    private val _showEmptyView = SingleLiveEvent<Boolean>()
    val showEmptyView: LiveData<Boolean> = _showEmptyView

    private val _showProgressView = SingleLiveEvent<Boolean>()
    val showProgressView: LiveData<Boolean> = _showProgressView

    private val _dismissWithError = SingleLiveEvent<Unit>()
    val dismissWithError: LiveData<Unit> = _dismissWithError

    private val _dismissBottomSheet = SingleLiveEvent<Unit>()
    val dismissBottomSheet: LiveData<Unit> = _dismissBottomSheet

    private val _suggestedTimezone = MutableLiveData<String>()
    val suggestedTimezone = _suggestedTimezone

    private val _selectedTimezone = SingleLiveEvent<String>()
    val selectedTimezone = _selectedTimezone

    private val _timezones = MutableLiveData<List<TimezonesList>>()

    private val searchInput = MutableLiveData<String>()
    private val timezoneSearch: LiveData<List<TimezonesList>> = Transformations.switchMap(searchInput) { term ->
        filterTimezones(term)
    }

    val timezones = MediatorLiveData<List<TimezonesList>>().apply {
        addSource(_timezones) {
            value = it
        }
        addSource(timezoneSearch) {
            value = it
        }
    }.distinctUntilChanged()

    fun searchTimezones(query: CharSequence) {
        searchInput.value = query.toString()
    }

    fun onTimezoneSelected(timezone: String) {
        _selectedTimezone.value = timezone
        _dismissBottomSheet.asyncCall()
    }

    @VisibleForTesting
    fun filterTimezones(query: String): LiveData<List<TimezonesList>> {
        val filteredTimezones = MutableLiveData<List<TimezonesList>>()

        timezonesList.filter { timezone ->
            when (timezone) {
                is TimezoneItem -> {
                    timezone.label.contains(query, true) or timezone.offset.contains(query, true)
                }
                else -> false
            }
        }.also {
            _showEmptyView.value = it.isEmpty()
            filteredTimezones.value = it
        }

        return filteredTimezones
    }

    fun onSearchCancelled() {
        _showEmptyView.value = false
        _timezones.postValue(timezonesList)
    }

    fun getTimezones(context: Context) {
        _suggestedTimezone.postValue(TimeZone.getDefault().id)

        requestTimezones(context)
    }

    private fun requestTimezones(context: Context) {
        val listener = Response.Listener { response: String? ->
            AppLog.d(SETTINGS, "timezones requested")
            _showProgressView.postValue(false)

            if (!TextUtils.isEmpty(response)) {
                timezonesList.clear()
                loadTimezones(response)
            } else {
                AppLog.w(SETTINGS, "empty response requesting timezones")
                _dismissWithError.call()
            }
        }

        val errorListener = Response.ErrorListener { error: VolleyError? ->
            AppLog.e(SETTINGS, "Error requesting timezones", error)
            _dismissWithError.call()
        }

        val request: StringRequest = object : StringRequest(Constants.URL_TIMEZONE_ENDPOINT, listener, errorListener) {
            override fun getParams(): Map<String, String> {
                return RestClientUtils.getRestLocaleParams(context)
            }
        }

        _showProgressView.postValue(true)
        val queue = Volley.newRequestQueue(context)
        queue.add(request)
    }

    private fun loadTimezones(responseJson: String?) {
        try {
            val jsonResponse = JSONObject(responseJson.orEmpty())
            val jsonTimezonesByContinent = jsonResponse.getJSONObject("timezones_by_continent")
            val jsonTimezonesManualOffsets = jsonResponse.getJSONArray("manual_utc_offsets")

            jsonTimezonesByContinent.keys().forEach {
                timezonesList.add(TimezoneHeader(it))
                addTimezoneItems(jsonTimezonesByContinent.getJSONArray(it))
            }

            timezonesList.add(
                TimezoneHeader(
                    resourceProvider.getString(R.string.site_settings_timezones_manual_offsets)
                )
            )
            for (i in 0 until jsonTimezonesManualOffsets.length()) {
                val timezone = jsonTimezonesManualOffsets.getJSONObject(i)
                timezonesList.add(TimezoneItem(timezone.getString("label"), timezone.getString("value")))
            }

            AppLog.d(SETTINGS, timezonesList.toString())
            _timezones.postValue(timezonesList)
        } catch (e: JSONException) {
            AppLog.e(SETTINGS, "Error parsing timezones", e)
            _dismissWithError.call()
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

    private fun getZoneOffset(zone: String): String {
        val timezone = TimeZone.getTimeZone(zone)

        val cal = Calendar.getInstance(timezone)
        val sdf = SimpleDateFormat("z", Locale.getDefault())
        sdf.timeZone = timezone
        val offset = sdf.format(cal.time)

        return "${timezone.displayName} ($offset)"
    }

    private fun getTimeAtZone(zone: String): String {
        val timezone = TimeZone.getTimeZone(zone)

        val cal = Calendar.getInstance(timezone)
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.timeZone = timezone

        return sdf.format(cal.time)
    }
}
