package org.wordpress.android.ui.prefs.locale

import android.content.Context
import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.prefs.language.LocalePickerListItem.LocaleRow
import org.wordpress.android.ui.prefs.language.LocalePickerViewModel
import org.wordpress.android.ui.prefs.language.LocalePickerViewModel.LocalePickerUiState
import org.wordpress.android.util.LocaleProvider
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Locale

@ExperimentalCoroutinesApi
class LocalePickerViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var resourceProvider: ResourceProvider
    @Mock
    lateinit var localeProvider: LocaleProvider
    @Mock
    lateinit var context: Context

    @Mock
    lateinit var currentLocale: Locale

    @Mock
    lateinit var selectedLocaleObserver: Observer<String>

    @Mock
    lateinit var dismissBottomSheetObserver: Observer<Unit>
    @Mock
    lateinit var expandBottomSheetObserver: Observer<Unit>
    @Mock
    lateinit var hideKeyboardObserver: Observer<Unit>
    @Mock
    lateinit var clearSearchFieldObserver: Observer<Unit>

    private lateinit var viewModel: LocalePickerViewModel

    private var uiStates = mutableListOf<LocalePickerUiState>()

    private val languageNames = arrayOf("English (United States)", "Italian", "Russian")
    private val languageCodes = arrayOf("en_US", "it", "ru")
    private val localizedLanguageNames = arrayOf("English (United States)", "Italiano", "Русский")

    private val dummyLocales = Triple(
        languageNames,
        languageCodes,
        localizedLanguageNames
    )

    @Before
    fun setUp() {
        uiStates.clear()

        viewModel = LocalePickerViewModel(resourceProvider, localeProvider)

        viewModel.uiState.observeForever {
            if (it != null) {
                uiStates.add(it)
            }
        }

        whenever(currentLocale.toString()).thenReturn("en_US")
        whenever(localeProvider.getAppLocale()).thenReturn(currentLocale)
        whenever(localeProvider.getAppLanguageDisplayString()).thenReturn("English (United States)")
        whenever(localeProvider.createSortedLocalizedLanguageDisplayStrings(any(), any())).thenReturn(dummyLocales)
        whenever(resourceProvider.getStringArray(any())).thenReturn(languageCodes)

        viewModel.selectedLocale.observeForever(selectedLocaleObserver)
        viewModel.dismissBottomSheet.observeForever(dismissBottomSheetObserver)
        viewModel.expandBottomSheet.observeForever(expandBottomSheetObserver)
        viewModel.hideKeyboard.observeForever(hideKeyboardObserver)
        viewModel.clearSearchField.observeForever(clearSearchFieldObserver)
    }

    @Test
    fun `starting VM displays current locale and loads supported locales`() = test {
        viewModel.start()
        assertThat(uiStates).hasSize(3)

        val lastState = uiStates.last()

        assertThat(lastState.currentLocale).isNotNull
        assertThat(lastState.currentLocale?.label).isEqualTo("English (United States)")
        assertThat(lastState.currentLocale?.localeCode).isEqualTo("en_US")

        assertThat(lastState.isEmptyViewVisible).isFalse()

        assertThat(lastState.listData).isNotNull
        assertThat(lastState.listData?.size).isEqualTo(3)

        val firstLocale = lastState.listData?.get(0)

        assertThat(firstLocale is LocaleRow).isTrue()
        assertThat((firstLocale as LocaleRow).label).isEqualTo("English (United States)")
        assertThat(firstLocale.localeCode).isEqualTo("en_US")
        assertThat(firstLocale.localizedLabel).isEqualTo("English (United States)")

        val secondLocale = lastState.listData?.get(1)
        assertThat(secondLocale is LocaleRow).isTrue()
        assertThat((secondLocale as LocaleRow).label).isEqualTo("Italian")
        assertThat(secondLocale.localeCode).isEqualTo("it")
        assertThat(secondLocale.localizedLabel).isEqualTo("Italiano")

        val thirdLocale = lastState.listData?.get(2)
        assertThat(thirdLocale is LocaleRow).isTrue()
        assertThat((thirdLocale as LocaleRow).label).isEqualTo("Russian")
        assertThat(thirdLocale.localeCode).isEqualTo("ru")
        assertThat(thirdLocale.localizedLabel).isEqualTo("Русский")
    }

    @Test
    fun `onSearchQueryChanged produces filtered results based on the query`() = test {
        viewModel.start()

        viewModel.onSearchQueryChanged("Ita")
        val filteredItalianLocaleState = uiStates.last()

        assertThat(filteredItalianLocaleState.listData?.size).isEqualTo(1)

        val italianLocale = filteredItalianLocaleState.listData?.get(0)
        assertThat(italianLocale is LocaleRow).isTrue()
        assertThat((italianLocale as LocaleRow).label).isEqualTo("Italian")
        assertThat(italianLocale.localeCode).isEqualTo("it")
        assertThat(italianLocale.localizedLabel).isEqualTo("Italiano")

        // searching localized label
        viewModel.onSearchQueryChanged("Русс")

        val filteredRussianLocaleState = uiStates.last()

        val russianLocale = filteredRussianLocaleState.listData?.get(0)
        assertThat(russianLocale is LocaleRow).isTrue()
        assertThat((russianLocale as LocaleRow).label).isEqualTo("Russian")
        assertThat(russianLocale.localeCode).isEqualTo("ru")
    }

    @Test
    fun `onSearchQueryChanged shows empty view if no results are found`() = test {
        viewModel.start()

        viewModel.onSearchQueryChanged("Span")
        val lastState = uiStates.last()

        assertThat(lastState.listData?.size).isEqualTo(0)
        assertThat(lastState.isEmptyViewVisible).isTrue()
    }

    @Test
    fun `onClearSearchFieldButtonClicked hides keyboard and clears search field`() = test {
        viewModel.start()
        viewModel.onClearSearchFieldButtonClicked()

        verify(hideKeyboardObserver).onChanged(null)
        verify(clearSearchFieldObserver).onChanged(null)
    }

    @Test
    fun `when no results found onClearSearchFieldButtonClicked hides empty view and displays original list`() = test {
        viewModel.start()

        viewModel.onSearchQueryChanged("Span")
        val stateAfterSearch = uiStates.last()

        assertThat(stateAfterSearch.listData?.size).isEqualTo(0)
        assertThat(stateAfterSearch.isEmptyViewVisible).isTrue()

        viewModel.onClearSearchFieldButtonClicked()

        val stateAfterSearchCancelled = uiStates.last()

        assertThat(stateAfterSearchCancelled.listData?.size).isEqualTo(3)
        assertThat(stateAfterSearchCancelled.isEmptyViewVisible).isFalse()
    }

    @Test
    fun `onSearchFieldFocused expands bottom sheet`() = test {
        viewModel.start()
        viewModel.onSearchFieldFocused()

        verify(expandBottomSheetObserver).onChanged(null)
    }

    @Test
    fun `onCurrentLocaleSelected selects current locale and dismisses the bottom sheet`() = test {
        viewModel.start()

        viewModel.onCurrentLocaleSelected()

        verify(selectedLocaleObserver).onChanged("en_US")
        verify(dismissBottomSheetObserver).onChanged(null)
    }

    @Test
    fun `clicking on a locale selects current locale and dismisses the bottom sheet`() = test {
        viewModel.start()

        val lastState = uiStates.last()
        val italianLocale = lastState.listData?.get(1)
        (italianLocale as LocaleRow).clickAction.onClick()

        verify(selectedLocaleObserver).onChanged("it")
        verify(dismissBottomSheetObserver).onChanged(null)
    }

    @Test
    fun `onListScrolled hides a keyboard`() = test {
        viewModel.start()
        viewModel.onListScrolled()

        verify(hideKeyboardObserver).onChanged(null)
    }
}
