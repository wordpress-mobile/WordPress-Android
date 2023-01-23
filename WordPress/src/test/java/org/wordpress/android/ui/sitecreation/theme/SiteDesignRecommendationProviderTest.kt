package org.wordpress.android.ui.sitecreation.theme

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.network.rest.wpcom.theme.StarterDesign
import org.wordpress.android.fluxc.network.rest.wpcom.theme.StarterDesignCategory
import org.wordpress.android.ui.layoutpicker.LayoutCategoryModel
import org.wordpress.android.ui.layoutpicker.LayoutModel
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SiteDesignRecommendationProviderTest : BaseUnitTest() {
    @Mock
    lateinit var resourceProvider: ResourceProvider

    private lateinit var recommendationProvider: SiteDesignRecommendationProvider

    private val mockedVerticalSlug = "mockedVerticalSlug"
    private val mockedVerticalTitle = "mockedVerticalTitle"
    private val recommendedDesignSlug = "recommendedDesignSlug"
    private val blogCategory = StarterDesignCategory(
        slug = "blog",
        title = "Blog",
        description = "Blogging designs",
        emoji = ""
    )
    private val anotherCategory = StarterDesignCategory(
        slug = "another",
        title = "Another",
        description = "Random designs",
        emoji = ""
    )
    private val blogDesign = StarterDesign(
        "blog",
        "title",
        1L,
        listOf(blogCategory),
        "url",
        "theme",
        listOf("any"),
        "desktopThumbnail",
        "tabletThumbnail",
        "mobileThumbnail"
    )
    private val anotherDesign = StarterDesign(
        recommendedDesignSlug,
        "title",
        1L,
        listOf(anotherCategory),
        "url",
        "theme",
        listOf("any", mockedVerticalSlug),
        "desktopThumbnail",
        "tabletThumbnail",
        "mobileThumbnail"
    )
    private val allDesigns = listOf(blogDesign, anotherDesign)
    private val allCategories = listOf(blogCategory, anotherCategory)

    private val slugsArray = arrayOf("art", "automotive", "beauty", mockedVerticalSlug)
    private val verticalArray = arrayOf("Art", "Automotive", "Beauty", mockedVerticalTitle)

    private class ResponseHandler {
        var layouts: List<LayoutModel>? = null
        var categories: List<LayoutCategoryModel>? = null
        fun handle(layouts: List<LayoutModel>, categories: List<LayoutCategoryModel>) {
            this.layouts = layouts
            this.categories = categories
        }
    }

    @Before
    fun setUp() {
        recommendationProvider = SiteDesignRecommendationProvider(resourceProvider)
        whenever(resourceProvider.getString(any())).thenReturn("Blogging")
        whenever(resourceProvider.getString(any(), any())).thenReturn("Best for Blogging")
        whenever(resourceProvider.getStringArray(R.array.site_creation_intents_slugs)).thenReturn(slugsArray)
        whenever(resourceProvider.getStringArray(R.array.site_creation_intents_strings)).thenReturn(verticalArray)
    }

    @Test
    fun `All non recommended categories are randomised`() {
        val handler = ResponseHandler()
        recommendationProvider.handleResponse("", allDesigns, allCategories, handler::handle)
        assertThat(requireNotNull(handler.categories?.filter { !it.isRecommended })).allMatch { it.randomizeOrder }
    }

    @Test
    fun `when no vertical is selected the blog category is recommended`() {
        val handler = ResponseHandler()
        recommendationProvider.handleResponse("", allDesigns, allCategories, handler::handle)
        assertThat(requireNotNull(handler.categories?.filter { it.isRecommended }?.size)).isEqualTo(1)
        assertThat(requireNotNull(handler.categories?.first()?.isRecommended)).isEqualTo(true)
        assertThat(requireNotNull(handler.categories?.first()?.randomizeOrder)).isEqualTo(true)
        assertThat(requireNotNull(handler.categories?.first()?.slug)).isEqualTo(blogCategory.slug)
    }

    @Test
    fun `when a vertical is selected and there are no recommendations, the blog category is recommended`() {
        val handler = ResponseHandler()
        recommendationProvider.handleResponse("art", allDesigns, allCategories, handler::handle)
        assertThat(requireNotNull(handler.categories?.filter { it.isRecommended }?.size)).isEqualTo(1)
        assertThat(requireNotNull(handler.categories?.first()?.isRecommended)).isEqualTo(true)
        assertThat(requireNotNull(handler.categories?.first()?.randomizeOrder)).isEqualTo(true)
        assertThat(requireNotNull(handler.categories?.first()?.slug)).isEqualTo(blogCategory.slug)
    }

    @Test
    fun `when a vertical is selected and there are recommendations, a recommended category is created`() {
        val handler = ResponseHandler()
        recommendationProvider.handleResponse(mockedVerticalTitle, allDesigns, allCategories, handler::handle)
        assertThat(requireNotNull(handler.categories?.filter { it.isRecommended }?.size)).isEqualTo(1)
        assertThat(requireNotNull(handler.categories?.first()?.isRecommended)).isEqualTo(true)
        assertThat(requireNotNull(handler.categories?.first()?.randomizeOrder)).isEqualTo(false)
        assertThat(requireNotNull(handler.categories?.first()?.slug)).isEqualTo("recommended_$mockedVerticalSlug")
    }

    @Test
    fun `when a vertical is selected the associated design is recommended`() {
        val handler = ResponseHandler()
        recommendationProvider.handleResponse(mockedVerticalTitle, allDesigns, allCategories, handler::handle)
        assertThat(requireNotNull(handler.layouts?.filter { it.slug == recommendedDesignSlug }?.size)).isEqualTo(1)
    }

    @Test
    fun `when no vertical is selected and there is no blog category skip the recommendation`() {
        val handler = ResponseHandler()
        val noBlogDesigns = listOf(anotherDesign)
        val noBlogCategories = listOf(anotherCategory)
        recommendationProvider.handleResponse("", noBlogDesigns, noBlogCategories, handler::handle)
        assertThat(requireNotNull(handler.categories?.filter { it.isRecommended }?.size)).isEqualTo(0)
    }

    @Test
    fun `when a vertical is selected and there are no recommendations or blog category skip the recommendation`() {
        val handler = ResponseHandler()
        val noBlogDesigns = listOf(anotherDesign)
        val noBlogCategories = listOf(anotherCategory)
        recommendationProvider.handleResponse("art", noBlogDesigns, noBlogCategories, handler::handle)
        assertThat(requireNotNull(handler.categories?.filter { it.isRecommended }?.size)).isEqualTo(0)
    }
}
