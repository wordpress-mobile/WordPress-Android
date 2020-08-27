package org.wordpress.android.ui.posts.prepublishing

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Calendar
import java.util.Locale

class PrepublishingPublishSettingsViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: PrepublishingPublishSettingsViewModel
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    @Mock lateinit var resourceProvider: ResourceProvider

    @Before
    fun setup() {
        viewModel = PrepublishingPublishSettingsViewModel(
                resourceProvider,
                mock(),
                localeManagerWrapper,
                mock(),
                mock()
        )
        whenever(localeManagerWrapper.getCurrentCalendar()).thenReturn(Calendar.getInstance(Locale.US))
        whenever(resourceProvider.getString(R.string.immediately)).thenReturn("")
    }

    @Test
    fun `when viewModel is started updateToolbarTitle is called with the publish title`() {
        var title: UiStringRes? = null
        viewModel.updateToolbarTitle.observeForever {
            title = it as UiStringRes
        }

        viewModel.start(mock())

        assertThat(title?.stringRes).isEqualTo(R.string.prepublishing_nudges_toolbar_title_publish)
    }

    @Test
    fun `when onBackClicked is triggered navigateToHomeScreen is called`() {
        var event: Event<Unit>? = null
        viewModel.navigateToHomeScreen.observeForever {
            event = it
        }

        viewModel.onBackButtonClicked()

        assertThat(event).isNotNull
    }
}
