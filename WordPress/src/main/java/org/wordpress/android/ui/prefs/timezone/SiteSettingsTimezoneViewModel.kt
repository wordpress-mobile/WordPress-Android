package org.wordpress.android.ui.prefs.timezone

import android.content.Context
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import org.wordpress.android.ui.prefs.timezone.TimezonesList.TimezoneHeader
import org.wordpress.android.ui.prefs.timezone.TimezonesList.TimezoneItem
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.SETTINGS
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.TimeZone
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

    private val _timezones = MutableLiveData<List<TimezonesList>>()
    val timezones = _timezones


    fun searchTimezones(city: CharSequence?) {
        searchInput.value = city.toString()
    }

    private fun filterTimezones(city: String): LiveData<List<TimezonesList>> {
        val filteredTimezones = MutableLiveData<List<TimezonesList>>()

        timezonesList.filter { timezone ->
            when (timezone) {
                is TimezoneItem -> timezone.label.contains(city, true)
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

    @RequiresApi(VERSION_CODES.O) fun getTimezones(context: Context) {
        val zoneIds = TimeZone.getAvailableIDs().asList()

        val zones = zoneIds.groupBy {
            it.split('/').first()
        }.map {
            timezonesList.add(TimezoneHeader(it.key))
            it.value.map { zone ->
                timezonesList.add(
                        TimezoneItem(
                                zone.split("/").last(),
                                zone,
                                getZoneOffset(zone)
                        )
                )
            }
        }

        AppLog.d(SETTINGS, zones.toString())

        _timezones.postValue(timezonesList)
    }

    @RequiresApi(VERSION_CODES.O)
    private fun getZoneOffset(zone: String): String {
        val now = Instant.now()
        val fmt = SimpleDateFormat("Z", Locale.getDefault())

        fmt.timeZone = TimeZone.getTimeZone(zone)
        var offset: String = fmt.format(Date.from(now))
        offset = offset.substring(0, 3) + ":" + offset.substring(3)

        return "${zone.split('/').first()} Time (GMT${offset})"
    }
}
