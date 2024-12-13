package org.wordpress.android.ui.mysite.cards.sotw2023

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.mysite.MySiteCardAndItem
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

    @Mock
    lateinit var tracker: WpSotw2023NudgeCardAnalyticsTracker

    private lateinit var viewModelSlice: WpSotw2023NudgeCardViewModelSlice

    private val uiModels = mutableListOf<MySiteCardAndItem.Card.WpSotw2023NudgeCardModel?>()

    @Before
    fun setUp() {
        viewModelSlice = WpSotw2023NudgeCardViewModelSlice(
            featureConfig,
            appPrefsWrapper,
            dateTimeUtilsWrapper,
            localeManagerWrapper,
            tracker,
        )
        viewModelSlice.initialize(testScope())

        viewModelSlice.uiModel.observeForever { uiModel ->
            uiModels.add(uiModel)
        }
    }

    @Test
    fun `WHEN feature is disabled THEN buildCard returns null `() = test {
        mockCardRequisites(isFeatureEnabled = false)

        viewModelSlice.buildCard()

        assertThat(uiModels.last()).isNull()
    }

    @Test
    fun `WHEN card is hidden in app prefs THEN buildCard returns null`() = test {
        mockCardRequisites(isCardHidden = true)

        viewModelSlice.buildCard()

        assertThat(uiModels.last()).isNull()
    }

    @Test
    fun `WHEN date is before event THEN buildCard returns null`() = test{
        mockCardRequisites(isDateAfterEvent = false)

        viewModelSlice.buildCard()

        assertThat(uiModels.last()).isNull()
    }

    @Test
    fun `WHEN language is not english THEN buildCard returns null`() = test{
        mockCardRequisites(isLanguageEnglish = false)

        viewModelSlice.buildCard()

        assertThat(uiModels.last()).isNull()
    }

    @Test
    fun `WHEN requisites are met THEN buildCard returns card `() = test{
        mockCardRequisites()

        viewModelSlice.buildCard()

        assertThat(uiModels.last()).isNotNull
    }

    @Test
    fun `WHEN card onCtaClick is clicked THEN navigate to URL`() = test{
        mockCardRequisites()

        viewModelSlice.buildCard()
        uiModels.last()?.onCtaClick?.click()

        assertThat(viewModelSlice.onNavigation.value?.peekContent()).isEqualTo(OpenExternalUrl(EXPECTED_URL))
    }

    @Test
    fun `WHEN card onHideMenuItemClick is clicked THEN hide card in app prefs and refresh`() = test{
        mockCardRequisites()

        viewModelSlice.buildCard()

        val uiModel = uiModels.last() as MySiteCardAndItem.Card.WpSotw2023NudgeCardModel
        uiModel.onHideMenuItemClick.click()

        verify(appPrefsWrapper).setShouldHideSotw2023NudgeCard(true)
        assertThat(uiModels.last()).isNull()
    }

    // region Analytics
    @Test
    fun `WHEN card is shown THEN analytics is tracked`() {
        mockCardRequisites()

        viewModelSlice.trackShown()

        verify(tracker).trackShown()
    }

    @Test
    fun `WHEN card onCtaClick is clicked THEN analytics is tracked`() = test{
        mockCardRequisites()

        viewModelSlice.buildCard()
        uiModels.last()?.onCtaClick?.click()

        verify(tracker).trackCtaTapped()
    }

    @Test
    fun `WHEN card onHideMenuItemClick is clicked THEN analytics is tracked`() = test{
        mockCardRequisites()

        viewModelSlice.buildCard()
        uiModels.last()?.onHideMenuItemClick?.click()

        verify(tracker).trackHideTapped()
    }
    // endregion Analytics

    private fun mockCardRequisites(
        isFeatureEnabled: Boolean = true,
        isCardHidden: Boolean = false,
        isDateAfterEvent: Boolean = true,
        isLanguageEnglish: Boolean = true
    ) {
        with(Mockito.lenient()) {
            whenever(featureConfig.isEnabled()).thenReturn(isFeatureEnabled)
            whenever(appPrefsWrapper.getShouldHideSotw2023NudgeCard()).thenReturn(isCardHidden)
            val date = if (isDateAfterEvent) "2023-12-12T00:00:01Z" else "2021-12-11T00:00:00Z"
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
