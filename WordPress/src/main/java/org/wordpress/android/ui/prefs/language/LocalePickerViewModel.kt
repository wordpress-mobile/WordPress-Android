package org.wordpress.android.ui.prefs.language

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import org.wordpress.android.R.array
import org.wordpress.android.ui.prefs.language.LocalePickerListItem.ClickAction
import org.wordpress.android.ui.prefs.language.LocalePickerListItem.LocaleRow
import org.wordpress.android.util.LocaleProvider
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

class LocalePickerViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val localeProvider: LocaleProvider
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
    private val _suggestedLocale = MutableLiveData<CurrentLocale>()

    private val _selectedLocale = SingleLiveEvent<String>()
    val selectedLocale = _selectedLocale

    private val _loadedLocales = MutableLiveData<List<LocalePickerListItem>>()

    private val searchInput = MutableLiveData<String>()
    private val _filteredLocales: LiveData<List<LocalePickerListItem>> =
        Transformations.switchMap(searchInput) { term ->
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

    private var started = false

    fun start() {
        if (started) {
            return
        }
        started = true

        loadLocales()
    }

    fun onSearchQueryChanged(query: CharSequence?) {
        if (query.isNullOrBlank()) {
            clearSearch()
        } else {
            searchInput.value = query.toString()
        }
    }

    fun onCurrentLocaleSelected() {
        val localeCode = _suggestedLocale.value?.localeCode
        localeCode?.let {
            clickItem(localeCode)
        }
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

    private fun clickItem(localeCode: String) {
        _selectedLocale.postValue(localeCode)
        _dismissBottomSheet.asyncCall()
    }

    private fun clearSearch() {
        _isEmptyViewVisible.value = false
        _loadedLocales.postValue(cachedLocales)
    }

    private fun filterLocales(query: String): LiveData<List<LocalePickerListItem>> {
        val filteredLocales = MutableLiveData<List<LocalePickerListItem>>()

        cachedLocales.filter { locale ->
            when (locale) {
                is LocaleRow -> {
                    query.isBlank() or locale.label.contains(query, true) or locale.localizedLabel.contains(
                        query,
                        true
                    )
                }
            }
        }.also {
            _isEmptyViewVisible.value = it.isEmpty()
            filteredLocales.value = it
        }

        return filteredLocales
    }

    private fun loadLocales() {
        val appLocale = localeProvider.getAppLocale()

        val displayLabel = localeProvider.getAppLanguageDisplayString()
        _suggestedLocale.postValue(CurrentLocale(displayLabel, appLocale.toString()))

        val availableLocales = resourceProvider.getStringArray(array.available_languages).distinct()

        val availableLocalesData = localeProvider.createSortedLocalizedLanguageDisplayStrings(
            availableLocales.toTypedArray(),
            appLocale
        ) ?: return

        val sortedEntries = availableLocalesData.first
        val sortedValues = availableLocalesData.second
        val sortedLocalizedEntries = availableLocalesData.third

        for (i in availableLocalesData.first.indices) {
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

    data class CurrentLocale(
        val label: String,
        val localeCode: String
    )

    data class LocalePickerUiState(
        val listData: List<LocalePickerListItem>?,
        val currentLocale: CurrentLocale?,
        val isEmptyViewVisible: Boolean
    )
}
