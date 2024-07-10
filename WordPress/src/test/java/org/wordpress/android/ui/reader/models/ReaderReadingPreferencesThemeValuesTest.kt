package org.wordpress.android.ui.reader.models

import android.content.Context
import android.content.res.Resources
import android.content.res.Resources.Theme
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import com.google.android.material.R as MaterialR

@RunWith(MockitoJUnitRunner::class)
class ReaderReadingPreferencesThemeValuesTest {
    @Mock
    lateinit var context: Context

    @Mock
    lateinit var resources: Resources

    @Mock
    lateinit var theme: Theme

    private lateinit var contextCompatMock: MockedStatic<ContextCompat>
    private lateinit var resourcesCompatMock: MockedStatic<ResourcesCompat>

    @Before
    fun setUp() {
        whenever(context.resources) doReturn resources
        whenever(context.theme) doReturn theme

        whenever(theme.resolveAttribute(any(), any(), any())) doReturn false

        contextCompatMock = Mockito.mockStatic(ContextCompat::class.java).apply {
            `when`<Int> {
                ContextCompat.getColor(context, R.color.reader_theme_sepia_background)
            } doReturn SEPIA_BACKGROUND_COLOR

            `when`<Int> {
                ContextCompat.getColor(context, R.color.reader_theme_sepia_text)
            } doReturn SEPIA_BASE_TEXT_COLOR

            `when`<Int> {
                ContextCompat.getColor(context, R.color.reader_post_body_link)
            } doReturn SEPIA_LINK_COLOR
        }

        resourcesCompatMock = Mockito.mockStatic(ResourcesCompat::class.java).apply {
            `when`<Float> {
                ResourcesCompat.getFloat(resources, MaterialR.dimen.material_emphasis_high_type)
            } doReturn EMPHASIS_HIGH
            `when`<Float> {
                ResourcesCompat.getFloat(resources, MaterialR.dimen.material_emphasis_medium)
            } doReturn EMPHASIS_MEDIUM
            `when`<Float> {
                ResourcesCompat.getFloat(resources, MaterialR.dimen.material_emphasis_disabled)
            } doReturn EMPHASIS_DISABLED
            `when`<Float> {
                ResourcesCompat.getFloat(resources, R.dimen.emphasis_low)
            } doReturn EMPHASIS_LOW
        }
    }

    @After
    fun tearDown() {
        contextCompatMock.close()
        resourcesCompatMock.close()
    }

    @Ignore("This test is useless because the code uses some Color class methods that are not available in the JVM")
    @Test
    fun `ThemeValues#from should hold the correct Theme colors`() {
        // testing just one color should be enough for checking the calculations are correct
        val themeValues = ReaderReadingPreferences.ThemeValues.from(context, ReaderReadingPreferences.Theme.SEPIA)
        with(themeValues) {
            assertThat(cssBackgroundColor).isEqualTo(SEPIA_BACKGROUND_COLOR_CSS)
            assertThat(cssLinkColor).isEqualTo(SEPIA_LINK_COLOR_CSS)
            assertThat(cssTextColor).isEqualTo(SEPIA_TEXT_COLOR_CSS)
            assertThat(cssTextMediumColor).isEqualTo(SEPIA_TEXT_MEDIUM_COLOR_CSS)
            assertThat(cssTextLightColor).isEqualTo(SEPIA_TEXT_LIGHT_COLOR_CSS)
            assertThat(cssTextExtraLightColor).isEqualTo(SEPIA_TEXT_EXTRA_LIGHT_COLOR_CSS)
            assertThat(cssTextDisabledColor).isEqualTo(SEPIA_TEXT_DISABLED_COLOR_CSS)

            assertThat(intBackgroundColor).isEqualTo(SEPIA_BACKGROUND_COLOR)
            assertThat(intBaseTextColor).isEqualTo(SEPIA_BASE_TEXT_COLOR)
            assertThat(intTextColor).isEqualTo(SEPIA_TEXT_COLOR)
            assertThat(intLinkColor).isEqualTo(SEPIA_LINK_COLOR)
        }
    }

    companion object {
        private const val EMPHASIS_HIGH = 0.87f
        private const val EMPHASIS_MEDIUM = 0.6f
        private const val EMPHASIS_DISABLED = 0.38f
        private const val EMPHASIS_LOW = 0.2f

        // expected colors for SEPIA theme
        private const val SEPIA_BACKGROUND_COLOR = 0xFFEAE0CD.toInt()
        private const val SEPIA_BASE_TEXT_COLOR = 0xFF27201B.toInt()
        private const val SEPIA_TEXT_COLOR = 0xDE27201B.toInt()
        private const val SEPIA_LINK_COLOR = 0xFF0675C4.toInt()

        private const val SEPIA_BACKGROUND_COLOR_CSS = "#EAE0CD"
        private const val SEPIA_LINK_COLOR_CSS = "#0675C4"
        private const val SEPIA_TEXT_COLOR_CSS = "rgba(39, 32, 27, 0.87)"
        private const val SEPIA_TEXT_MEDIUM_COLOR_CSS = "rgba(39, 32, 27, 0.6)"
        private const val SEPIA_TEXT_LIGHT_COLOR_CSS = "rgba(39, 32, 27, 0.38)"
        private const val SEPIA_TEXT_EXTRA_LIGHT_COLOR_CSS = "rgba(39, 32, 27, 0.2)"
        private const val SEPIA_TEXT_DISABLED_COLOR_CSS = "rgba(39, 32, 27, 0.38)"
    }
}
