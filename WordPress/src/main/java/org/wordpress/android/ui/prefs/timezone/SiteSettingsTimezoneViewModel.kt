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

    fun getTimezones() {
        _suggestedTimezone.postValue(ZoneId.systemDefault().id)

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

//        timezonesList.addAll(getManualOffsets())

        AppLog.d(SETTINGS, timezonesList.toString())

        _timezones.postValue(timezonesList)
    }

    private fun getZoneOffset(zone: String): String {
        val zoneId = ZoneId.of(zone)
        val offset = ZonedDateTime.now(zoneId).offset
        val offsetDisplay = if (offset.id == "Z") "" else  "(GMT$offset)"

        return "${zoneId.getDisplayName(FULL, Locale.getDefault())} $offsetDisplay"
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
        PACIFIC("Pacific"),
        UTC("Etc")
    }

    // Manual offsets that are return from api and accepted by backend are different from that of TimeZone class,
    // hence adding the list manually.
    private fun getManualOffsets(): List<TimezonesList> {
        val manualOffsets = mutableListOf<TimezonesList>()

        manualOffsets.add(TimezoneHeader("Manual Offsets"))

        manualOffsets.add(TimezoneItem(value = "UTC-12", label = "UTC-12"))
        manualOffsets.add(TimezoneItem(value = "UTC-11.5", label = "UTC-11:30"))
        manualOffsets.add(TimezoneItem(value = "UTC-11", label = "UTC-11"))
        manualOffsets.add(TimezoneItem(value = "UTC-10.5", label = "UTC-10:30"))
        manualOffsets.add(TimezoneItem(value = "UTC-10", label = "UTC-10"))
        manualOffsets.add(TimezoneItem(value = "UTC-9.5", label = "UTC-9:30"))
        manualOffsets.add(TimezoneItem(value = "UTC-9", label = "UTC-9"))
        manualOffsets.add(TimezoneItem(value = "UTC-8.5", label = "UTC-8:30"))
        manualOffsets.add(TimezoneItem(value = "UTC-8", label = "UTC-8"))
        manualOffsets.add(TimezoneItem(value = "UTC-7.5", label = "UTC-7:30"))
        manualOffsets.add(TimezoneItem(value = "UTC-7", label = "UTC-7"))
        manualOffsets.add(TimezoneItem(value = "UTC-6.5", label = "UTC-6:30"))
        manualOffsets.add(TimezoneItem(value = "UTC-6", label = "UTC-6"))
        manualOffsets.add(TimezoneItem(value = "UTC-5.5", label = "UTC-5:30"))
        manualOffsets.add(TimezoneItem(value = "UTC-5", label = "UTC-5"))
        manualOffsets.add(TimezoneItem(value = "UTC-4.5", label = "UTC-4:30"))
        manualOffsets.add(TimezoneItem(value = "UTC-4", label = "UTC-4"))
        manualOffsets.add(TimezoneItem(value = "UTC-3.5", label = "UTC-3:30"))
        manualOffsets.add(TimezoneItem(value = "UTC-3", label = "UTC-3"))
        manualOffsets.add(TimezoneItem(value = "UTC-2.5", label = "UTC-2:30"))
        manualOffsets.add(TimezoneItem(value = "UTC-2", label = "UTC-2"))
        manualOffsets.add(TimezoneItem(value = "UTC-1.5", label = "UTC-1:30"))
        manualOffsets.add(TimezoneItem(value = "UTC-1", label = "UTC-1"))
        manualOffsets.add(TimezoneItem(value = "UTC-0.5", label = "UTC-0:30"))
        manualOffsets.add(TimezoneItem(value = "UTC+0", label = "UTC+0"))
        manualOffsets.add(TimezoneItem(value = "UTC+0.5", label = "UTC+0:30"))
        manualOffsets.add(TimezoneItem(value = "UTC+1", label = "UTC+1"))
        manualOffsets.add(TimezoneItem(value = "UTC+1.5", label = "UTC+1:30"))
        manualOffsets.add(TimezoneItem(value = "UTC+2", label = "UTC+2"))
        manualOffsets.add(TimezoneItem(value = "UTC+2.5", label = "UTC+2:30"))
        manualOffsets.add(TimezoneItem(value = "UTC+3", label = "UTC+3"))
        manualOffsets.add(TimezoneItem(value = "UTC+3.5", label = "UTC+3:30"))
        manualOffsets.add(TimezoneItem(value = "UTC+4", label = "UTC+4"))
        manualOffsets.add(TimezoneItem(value = "UTC+4.5", label = "UTC+4:30"))
        manualOffsets.add(TimezoneItem(value = "UTC+5", label = "UTC+5"))
        manualOffsets.add(TimezoneItem(value = "UTC+5.5", label = "UTC+5:30"))
        manualOffsets.add(TimezoneItem(value = "UTC+6", label = "UTC+6"))
        manualOffsets.add(TimezoneItem(value = "UTC+6.5", label = "UTC+6:30"))
        manualOffsets.add(TimezoneItem(value = "UTC+7", label = "UTC+7"))
        manualOffsets.add(TimezoneItem(value = "UTC+7.5", label = "UTC+7:30"))
        manualOffsets.add(TimezoneItem(value = "UTC+8", label = "UTC+8"))
        manualOffsets.add(TimezoneItem(value = "UTC+8.5", label = "UTC+8:30"))
        manualOffsets.add(TimezoneItem(value = "UTC+9", label = "UTC+9"))
        manualOffsets.add(TimezoneItem(value = "UTC+9.5", label = "UTC+9:30"))
        manualOffsets.add(TimezoneItem(value = "UTC+10", label = "UTC+10"))
        manualOffsets.add(TimezoneItem(value = "UTC+10.5", label = "UTC+10:30"))
        manualOffsets.add(TimezoneItem(value = "UTC+11", label = "UTC+11"))
        manualOffsets.add(TimezoneItem(value = "UTC+11.5", label = "UTC+11:30"))
        manualOffsets.add(TimezoneItem(value = "UTC+12", label = "UTC+12"))
        manualOffsets.add(TimezoneItem(value = "UTC+12.75", label = "UTC+12:45"))
        manualOffsets.add(TimezoneItem(value = "UTC+13", label = "UTC+13"))
        manualOffsets.add(TimezoneItem(value = "UTC+13.75", label = "UTC+13:45"))
        manualOffsets.add(TimezoneItem(value = "UTC+14", label = "UTC+14"))

        return manualOffsets
    }
}

