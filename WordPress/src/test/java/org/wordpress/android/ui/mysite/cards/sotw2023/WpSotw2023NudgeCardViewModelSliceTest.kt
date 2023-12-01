package org.wordpress.android.ui.mysite.cards.sotw2023

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenExternalUrl
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.config.WpSotw2023NudgeFeatureConfig
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class WpSotw2023NudgeCardViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var featureConfig: WpSotw2023NudgeFeatureConfig

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper

    @Mock
    lateinit var localeManagerWrapper: LocaleManagerWrapper

    private lateinit var viewModelSlice: WpSotw2023NudgeCardViewModelSlice

    @Before
    fun setUp() {
        viewModelSlice = WpSotw2023NudgeCardViewModelSlice(
            featureConfig,
            appPrefsWrapper,
            dateTimeUtilsWrapper,
            localeManagerWrapper
        )
        viewModelSlice.initialize(testScope())
    }

    @Test
    fun `WHEN feature is disabled THEN buildCard returns null `() {
        mockCardRequisites(isFeatureEnabled = false)

        val card = viewModelSlice.buildCard()

        assertThat(card).isNull()
    }

    @Test
    fun `WHEN card is hidden in app prefs THEN buildCard returns null`() {
        mockCardRequisites(isCardHidden = true)

        val card = viewModelSlice.buildCard()

        assertThat(card).isNull()
    }

    @Test
    fun `WHEN date is before event THEN buildCard returns null`() {
        mockCardRequisites(isDateAfterEvent = false)

        val card = viewModelSlice.buildCard()

        assertThat(card).isNull()
    }

    @Test
    fun `WHEN language is not english THEN buildCard returns null`() {
        mockCardRequisites(isLanguageEnglish = false)

        val card = viewModelSlice.buildCard()

        assertThat(card).isNull()
    }

    @Test
    fun `WHEN requisites are met THEN buildCard returns card `() {
        mockCardRequisites()

        val card = viewModelSlice.buildCard()

        assertThat(card).isNotNull
    }

    @Test
    fun `WHEN card onCtaClick is clicked THEN navigate to URL`() {
        mockCardRequisites()

        val card = viewModelSlice.buildCard()!!

        card.onCtaClick.click()
        assertThat(viewModelSlice.onNavigation.value?.peekContent()).isEqualTo(OpenExternalUrl(EXPECTED_URL))
    }

    @Test
    fun `WHEN card onHideMenuItemClick is clicked THEN hide card in app prefs and refresh`() {
        mockCardRequisites()

        val card = viewModelSlice.buildCard()!!

        card.onHideMenuItemClick.click()
        verify(appPrefsWrapper).setShouldHideSotw2023NudgeCard(true)
        assertThat(viewModelSlice.refresh.value?.peekContent()).isTrue
    }

    // region Analytics
    @Ignore("TODO thomashortadev")
    @Test
    fun `WHEN card onCtaClick is clicked THEN analytics is tracked`() {
        // TODO thomashortadev implement when done
    }

    @Ignore("TODO thomashortadev")
    @Test
    fun `WHEN card onHideMenuItemClick is clicked THEN analytics is tracked`() {
        // TODO thomashortadev implement when done
    }
    // endregion Analytics

    private fun mockCardRequisites(
        isFeatureEnabled: Boolean = true,
        isCardHidden: Boolean = false,
        isDateAfterEvent: Boolean = true,
        isLanguageEnglish: Boolean = true
    ) {
        with (Mockito.lenient()) {
            whenever(featureConfig.isEnabled()).thenReturn(isFeatureEnabled)
            whenever(appPrefsWrapper.getShouldHideSotw2023NudgeCard()).thenReturn(isCardHidden)
            val date = if (isDateAfterEvent) "2023-12-12T00:00:00Z" else "2021-12-11T00:00:00Z"
            whenever(dateTimeUtilsWrapper.getInstantNow()).thenReturn(Instant.parse(date))
            val language = if (isLanguageEnglish) "en_US" else "fr_FR"
            whenever(localeManagerWrapper.getLanguage()).thenReturn(language)
        }
    }

    companion object {
        private const val EXPECTED_URL = "https://wordpress.org/state-of-the-word/" +
                "?utm_source=mobile&utm_medium=appnudge&utm_campaign=sotw2023"
    }
}
