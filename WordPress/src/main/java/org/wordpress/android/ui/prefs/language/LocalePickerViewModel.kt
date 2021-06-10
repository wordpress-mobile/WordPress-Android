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
import org.wordpress.android.util.LanguageUtils
import org.wordpress.android.util.LocaleManager
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

class LocalePickerViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val cachedLocales = mutableListOf<LocalePickerListItem>()

    private val _expandBottomSheet = SingleLiveEvent<Unit>()
    val expandBottomSheet: LiveData<Unit> = _expandBottomSheet

    private val _hideKeyboard = SingleLiveEvent<Unit>()
    val hideKeyboard: LiveData<Unit> = _hideKeyboard

    private val _clearSearchField = SingleLiveEvent<Unit>()
    val clearSearchField: LiveData<Unit> = _clearSearchField

    private val _dismissBottomSheet = SingleLiveEvent<Unit>()
    val dismissBottomSheet: LiveData<Unit> = _dismissBottomSheet

    private val _isEmptyViewVisible = SingleLiveEvent<Boolean>()
    private val _suggestedLocale = MutableLiveData<SuggestedLocale>()

    private val _selectedLocale = SingleLiveEvent<String>()
    val selectedLocale = _selectedLocale

    private val _loadedLocales = MutableLiveData<List<LocalePickerListItem>>()

    private val searchInput = MutableLiveData<String>()
    private val _filteredLocales: LiveData<List<LocalePickerListItem>> = Transformations.switchMap(searchInput) { term ->
        filterLocales(term)
    }

    private val locales = MediatorLiveData<List<LocalePickerListItem>>().apply {
        addSource(_loadedLocales) {
            value = it
        }
        addSource(_filteredLocales) {
            value = it
        }
    }.distinctUntilChanged()

    val uiState: LiveData<LocalePickerUiState> = merge(
            locales,
            _suggestedLocale,
            _isEmptyViewVisible
    ) { locales, suggestedLocale, emptyViewVisible ->
        LocalePickerUiState(
                locales,
                suggestedLocale,
                emptyViewVisible ?: false
        )
    }

    fun requestSearch(query: CharSequence?) {
        if (query.isNullOrBlank()) {
            clearSearch()
        } else {
            searchInput.value = query.toString()
        }
    }

    private fun clearSearch() {
        _isEmptyViewVisible.value = false
        _loadedLocales.postValue(cachedLocales)
    }

    fun onSuggestedLocaleSelected() {
        _selectedLocale.value = _suggestedLocale.value?.localeCode
        _dismissBottomSheet.asyncCall()
    }

    @VisibleForTesting
    fun filterLocales(query: String): LiveData<List<LocalePickerListItem>> {
        val filteredTimezones = MutableLiveData<List<LocalePickerListItem>>()

        cachedLocales.filter { timezone ->
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
            _isEmptyViewVisible.value = it.isEmpty()
            filteredTimezones.value = it
        }

        return filteredTimezones
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
        val displayLabel = LocaleManager.getLanguageString(deviceLocale.toString(), deviceLocale)

        return SuggestedLocale(displayLabel, deviceLocale.language + "_" + deviceLocale.country)
    }

    data class SuggestedLocale(
        val label: String,
        val localeCode: String
    )

    private fun loadLocales() {
        val appLanguageCode = LanguageUtils.getCurrentDeviceLanguageCode()

        val languageLocale = LocaleManager.languageLocale(appLanguageCode)
        val availableLocales = resourceProvider.getStringArray(array.available_languages).distinct()

        val triple = LocaleManager.createSortedLanguageDisplayStrings(availableLocales.toTypedArray(), languageLocale)
                ?: return

        val sortedEntries = triple.first
        val sortedValues = triple.second
        val sortedLocalizedEntries = triple.third

        for (i in triple.first.indices) {
            cachedLocales.add(
                    LocaleRow(
                            sortedEntries[i],
                            sortedLocalizedEntries[i],
                            sortedValues[i],
                            ClickAction(sortedValues[i], this::clickItem)
                    )
            )
        }

        _loadedLocales.postValue(cachedLocales)
    }

    fun clickItem(localeCode: String) {
        _selectedLocale.postValue(localeCode)
    }

    fun onListScrolled() {
        _hideKeyboard.call()
    }

    fun onSearchFieldFocused() {
        _expandBottomSheet.call()
    }

    fun onClearSearchFieldButtonClicked() {
        clearSearch()
        _hideKeyboard.call()
        _clearSearchField.call()
    }

    data class LocalePickerUiState(
        val listData: List<LocalePickerListItem>?,
        val suggestedLocale: SuggestedLocale?,
        val isEmptyViewVisible: Boolean
    )
}
