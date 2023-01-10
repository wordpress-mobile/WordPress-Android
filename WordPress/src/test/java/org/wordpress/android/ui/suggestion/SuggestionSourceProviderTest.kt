package org.wordpress.android.ui.suggestion

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.suggestion.SuggestionType.Users
import org.wordpress.android.ui.suggestion.SuggestionType.XPosts

@RunWith(MockitoJUnitRunner::class)
class SuggestionSourceProviderTest {
    @Mock
    lateinit var mockSuggestionSourceSubcomponentFactory: SuggestionSourceSubcomponent.Factory

    @Mock
    lateinit var mockSuggestionSourceSubcomponent: SuggestionSourceSubcomponent

    @Mock
    lateinit var mockSite: SiteModel

    @InjectMocks
    lateinit var provider: SuggestionSourceProvider

    @Before
    fun setUp() {
        whenever(mockSuggestionSourceSubcomponentFactory.create(mockSite))
            .thenReturn(mockSuggestionSourceSubcomponent)
    }

    @Test
    fun `gets xpost source`() {
        val expected = mock<XPostsSuggestionSource>()
        whenever(mockSuggestionSourceSubcomponent.xPostSuggestionSource())
            .thenReturn(expected)
        val actual = provider.get(XPosts, mockSite)
        assertThat(expected).isEqualTo(actual)
    }

    @Test
    fun `gets user source`() {
        val expected = mock<UserSuggestionSource>()
        whenever(mockSuggestionSourceSubcomponent.userSuggestionSource())
            .thenReturn(expected)
        val actual = provider.get(Users, mockSite)
        assertThat(expected).isEqualTo(actual)
    }
}
