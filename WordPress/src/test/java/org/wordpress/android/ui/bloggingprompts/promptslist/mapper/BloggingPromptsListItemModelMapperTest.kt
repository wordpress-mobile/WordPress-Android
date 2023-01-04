package org.wordpress.android.ui.bloggingprompts.promptslist.mapper

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.ui.bloggingprompts.promptslist.model.BloggingPromptsListItemModel
import org.wordpress.android.util.LocaleManagerWrapper
import java.util.Date
import java.util.Locale

@ExperimentalCoroutinesApi
class BloggingPromptsListItemModelMapperTest : BaseUnitTest() {
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    lateinit var mapper: BloggingPromptsListItemModelMapper

    @Before
    fun setUp() {
        whenever(localeManagerWrapper.getLocale()).thenReturn(Locale.US)
        mapper = BloggingPromptsListItemModelMapper(localeManagerWrapper)
    }

    @Test
    fun `when toUiModel is called, then maps to UI model correctly`() {
        val result = mapper.toUiModel(DOMAIN_MODEL)

        assertThat(result).isEqualTo(UI_MODEL)
    }

    companion object {
        private val DOMAIN_MODEL = BloggingPromptModel(
                id = 123,
                text = "Text",
                title = "Title",
                content = "Content",
                date = Date(1671678000000), // December 22, 2022
                isAnswered = true,
                attribution = "Attribution",
                respondentsCount = 321,
                respondentsAvatarUrls = emptyList(),
        )

        private val UI_MODEL = BloggingPromptsListItemModel(
                id = 123,
                text = "Text",
                date = Date(1671678000000), // December 22, 2022
                formattedDate = "Dec 22",
                isAnswered = true,
                answersCount = 321,
        )
    }
}
