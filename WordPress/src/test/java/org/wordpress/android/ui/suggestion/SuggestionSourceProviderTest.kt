package org.wordpress.android.ui.suggestion

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.suggestion.SuggestionType.Users
import org.wordpress.android.ui.suggestion.SuggestionType.XPosts

@RunWith(MockitoJUnitRunner::class)
class SuggestionSourceProviderTest {
    @Mock lateinit var mockSuggestionSourceSubcomponentFactory: SuggestionSourceSubcomponent.Factory
    @Mock lateinit var mockSuggestionSourceSubcomponent: SuggestionSourceSubcomponent
    @Mock lateinit var mockSite: SiteModel

    @InjectMocks lateinit var provider: SuggestionSourceProvider

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
        assertEquals(expected, actual)
    }

    @Test
    fun `gets user source`() {
        val expected = mock<UserSuggestionSource>()
        whenever(mockSuggestionSourceSubcomponent.userSuggestionSource())
                .thenReturn(expected)
        val actual = provider.get(Users, mockSite)
        assertEquals(expected, actual)
    }
}
