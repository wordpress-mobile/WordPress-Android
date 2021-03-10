package org.wordpress.android.ui.prefs.timezone

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import org.wordpress.android.ui.prefs.timezone.TimezonesList.TimezoneHeader
import org.wordpress.android.ui.prefs.timezone.TimezonesList.TimezoneItem
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.SETTINGS
import java.time.DateTimeException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle.FULL
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

    fun getTimezones() {
        timezonesList.clear()

        ZoneId.getAvailableZoneIds().groupBy {
            it.split('/').first()
        }.entries.sortedBy {
            it.key
        }.filter {
            Continents.values().any { continent ->
                it.key == continent.s
            }
        }.map {
            timezonesList.add(TimezoneHeader(it.key))

            // sort by city with timezone
            val sortedList = it.value.sortedBy { zone ->
                zone.split("/").last()
            }

            sortedList.map { zone ->
                timezonesList.add(TimezoneItem(
                        zone.split("/").last().replace("_", " "),
                        zone,
                        getZoneOffset(zone),
                        getTimeAtZone(zone)
                ))
            }
        }

        AppLog.d(SETTINGS, timezonesList.toString())

        _timezones.postValue(timezonesList)
    }

    private fun getZoneOffset(zone: String): String {
        val zoneId = ZoneId.of(zone)
        val zoneDateTime = ZonedDateTime.now(zoneId)
        val offset = zoneDateTime.offset

        return "${zoneId.getDisplayName(FULL, Locale.getDefault())} (GMT$offset)"
    }

    private fun getTimeAtZone(zone: String): String {
        val dateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
        val zoneId = ZoneId.of(zone)
        val zoneDateTime = ZonedDateTime.now(zoneId)

        return try {
            zoneDateTime.format(dateTimeFormatter)
        } catch (e: DateTimeException) {
            AppLog.d(SETTINGS, e.localizedMessage)
            ""
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
        PACIFIC("Pacific")
    }
}

