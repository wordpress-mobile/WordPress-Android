package org.wordpress.android.ui.mysite.cards.personalize

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
import org.wordpress.android.util.config.DashboardPersonalizationFeatureConfig

@RunWith(MockitoJUnitRunner::class)
class PersonalizeCardBuilderTest {
    @Mock
    lateinit var featureConfig: DashboardPersonalizationFeatureConfig

    private lateinit var builder: PersonalizeCardBuilder

    @Before
    fun setUp() {
        builder = PersonalizeCardBuilder(featureConfig)
    }

    @Test
    fun `given flag is enabled, when build requested, then model is built`() {
        whenever(featureConfig.isEnabled()).thenReturn(true)

        val result = builder.build(getParams())

        assertThat(result).isNotNull
    }

    @Test
    fun `given flag is disabled, when build requested, then model is not built`() {
        whenever(featureConfig.isEnabled()).thenReturn(false)

        val result = builder.build(getParams())

        assertThat(result).isNull()
    }

    private fun getParams() =
         MySiteCardAndItemBuilderParams.PersonalizeCardBuilderParams(mock())
}
