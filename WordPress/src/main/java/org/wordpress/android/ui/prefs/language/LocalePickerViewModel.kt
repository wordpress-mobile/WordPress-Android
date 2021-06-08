package org.wordpress.android.ui.prefs.language

import android.content.res.Resources
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import org.wordpress.android.R.array
import org.wordpress.android.ui.prefs.language.LocalePickerListItem.ClickAction
import org.wordpress.android.ui.prefs.language.LocalePickerListItem.LocaleRow
import org.wordpress.android.ui.prefs.language.LocalePickerListItem.SubHeader
import org.wordpress.android.util.LocaleManager
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

class LocalePickerViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val localesList = mutableListOf<LocalePickerListItem>()

    private val _showEmptyView = SingleLiveEvent<Boolean>()
    val showEmptyView: LiveData<Boolean> = _showEmptyView

    private val _dismissBottomSheet = SingleLiveEvent<Unit>()
    val dismissBottomSheet: LiveData<Unit> = _dismissBottomSheet

    private val _suggestedLocale = MutableLiveData<SuggestedLocale>()
    val suggestedLocale = _suggestedLocale

    private val _selectedLocale = SingleLiveEvent<String>()
    val selectedLocale = _selectedLocale

    private val _locales = MutableLiveData<List<LocalePickerListItem>>()

    private val searchInput = MutableLiveData<String>()
    private val localeSearch: LiveData<List<LocalePickerListItem>> = Transformations.switchMap(searchInput) { term ->
        filterLocales(term)
    }

    val locales = MediatorLiveData<List<LocalePickerListItem>>().apply {
        addSource(_locales) {
            value = it
        }
        addSource(localeSearch) {
            value = it
        }
    }.distinctUntilChanged()

    fun searchLocales(query: CharSequence) {
        searchInput.value = query.toString()
    }

    fun onTimezoneSelected(timezone: String) {
        _selectedLocale.value = timezone
        _dismissBottomSheet.asyncCall()
    }

    @VisibleForTesting
    fun filterLocales(query: String): LiveData<List<LocalePickerListItem>> {
        val filteredTimezones = MutableLiveData<List<LocalePickerListItem>>()

        localesList.filter { timezone ->
            when (timezone) {
                is LocaleRow -> {
                    query.isBlank() or timezone.label.contains(query, true) or timezone.localizedLabel.contains(
                            query,
                            true
                    )
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
        _locales.postValue(localesList)
    }

    var started = false

    fun start() {
        if (started) {
            return
        }
        started = true
        _suggestedLocale.postValue(getDeviceLocale())
        loadLocales()
    }

    fun getDeviceLocale(): SuggestedLocale {
        val deviceLocale = Resources.getSystem().configuration.locale
        return SuggestedLocale(
                deviceLocale.language + "_" + deviceLocale.country,
                Resources.getSystem().configuration.locale.displayName
        )
    }

    data class SuggestedLocale(
        val label: String,
        val localeCode: String
    )

    private fun loadLocales() {
        val languageCode = resourceProvider.getConfiguration().locale.toString()

        val languageLocale = LocaleManager.languageLocale(languageCode)
        val availableLocales: Array<String> = resourceProvider.getStringArray(array.available_languages)

        val triple = LocaleManager.createSortedLanguageDisplayStrings(availableLocales, languageLocale) ?: return

        val sortedEntries = triple.first
        val sortedValues = triple.second
        val sortedLocalizedEntries = triple.third

        val appLocale = resourceProvider.getConfiguration().locale.toString()
        val indexOfCurrentLanguage = sortedValues.indexOf(appLocale)


        if (indexOfCurrentLanguage >= 0) {
            localesList.add(SubHeader("Current Language"))
            localesList.add(
                    LocaleRow(
                            sortedEntries[indexOfCurrentLanguage],
                            sortedLocalizedEntries[indexOfCurrentLanguage],
                            sortedValues[indexOfCurrentLanguage],
                            ClickAction(sortedValues[indexOfCurrentLanguage], this::clickItem)
                    )
            )

            localesList.add(SubHeader("All Languages"))
        }



        for (i in triple.first.indices) {
            val code = sortedValues[i]
            if (code != appLocale) {
                localesList.add(
                        LocaleRow(
                                sortedEntries[i],
                                sortedLocalizedEntries[i],
                                code,
                                ClickAction(sortedValues[i], this::clickItem)
                        )
                )
            }
        }



        _locales.postValue(localesList)
    }

    fun clickItem(localeCode: String) {
        _selectedLocale.postValue(localeCode)
    }
}
