package org.wordpress.android.ui.mlp

data class GutenbergPageLayouts(
    val layouts: List<GutenbergLayout>,
    val categories: List<GutenbergLayoutCategory>
) {
    fun layouts(categorySlug: String) =
            layouts.filter { l -> l.categories.any { c -> c.slug == categorySlug } }
}

data class GutenbergLayout(
    val slug: String,
    val title: String,
    val preview: String,
    val categories: List<GutenbergLayoutCategory>
)

data class GutenbergLayoutCategory(
    val slug: String,
    val title: String,
    val description: String,
    val emoji: String
)

/**
 * Static page layout data factory
 * NOTE: This implementation will be deprecated and the layouts for self hosted sites will be provided by a new
 * public API endpoint (/common-block-layouts)
 */
object GutenbergPageLayoutFactory {
    /**
     * Creates a a default set of Page Layout templates to be used on for creating starter pages layouts.
     * @return A default [GutenbergPageLayouts] object that contains a default set of layouts and categories
     */
    fun makeDefaultPageLayouts() =
            GutenbergPageLayouts(
                    makeDefaultLayouts(),
                    makeDefaultCategories()
            )

    // Categories

    private val aboutCategory = GutenbergLayoutCategory(
            slug = "about",
            title = "About",
            description = "About pages",
            emoji = "👋"
    )

    private val blogCategory = GutenbergLayoutCategory(
            slug = "blog",
            title = "Blog",
            description = "Blog pages",
            emoji = "📰"
    )

    private val contactCategory = GutenbergLayoutCategory(
            slug = "contact",
            title = "Contact",
            description = "Contact pages",
            emoji = "📫"
    )

    private val portfolioCategory = GutenbergLayoutCategory(
            slug = "portfolio",
            title = "Portfolio",
            description = "Portfolio pages",
            emoji = "🎨"
    )

    private val servicesCategory = GutenbergLayoutCategory(
            slug = "services",
            title = "Services",
            description = "Services pages",
            emoji = "🔧"
    )

    private val teamCategory = GutenbergLayoutCategory(
            slug = "team",
            title = "Team",
            description = "Team pages",
            emoji = "👥"
    )

    // Layouts

    private val aboutLayout = GutenbergLayout(
            slug = "about",
            title = "About",
            preview = "https://headstartdata.files.wordpress.com/2020/01/about-2.png",
            categories = listOf(aboutCategory)
    )

    private val blogLayout = GutenbergLayout(
            slug = "blog",
            title = "Blog",
            preview = "https://headstartdata.files.wordpress.com/2019/06/blog-4.png",
            categories = listOf(blogCategory)
    )

    private val contactLayout = GutenbergLayout(
            slug = "contact",
            title = "Contact",
            preview = "https://headstartdata.files.wordpress.com/2019/06/contact-2.png",
            categories = listOf(contactCategory)
    )

    private val portfolioLayout = GutenbergLayout(
            slug = "portfolio",
            title = "Portfolio",
            preview = "https://headstartdata.files.wordpress.com/2019/06/portfolio-2.png",
            categories = listOf(portfolioCategory)
    )

    private val servicesLayout = GutenbergLayout(
            slug = "services",
            title = "Services",
            preview = "https://headstartdata.files.wordpress.com/2019/06/services-2.png",
            categories = listOf(servicesCategory)
    )

    private val teamLayout = GutenbergLayout(
            slug = "team",
            title = "Team",
            preview = "https://headstartdata.files.wordpress.com/2020/03/team.png",
            categories = listOf(teamCategory)
    )

    /**
     * Creates a a default set of Categories templates to be used on for creating starter pages layouts.
     */
    private fun makeDefaultCategories(): List<GutenbergLayoutCategory> = listOf(
            aboutCategory,
            blogCategory,
            contactCategory,
            portfolioCategory,
            servicesCategory,
            teamCategory
    )

    /**
     * DEMO Code: TO BE REMOVED
     */
    private const val demoModes = true
    private const val times = 30

    private fun GutenbergLayout.repeat(times: Int): List<GutenbergLayout> = ArrayList<GutenbergLayout>().apply {
        for (i in 1..times) {
            add(GutenbergLayout("$slug-$i", title, preview, categories))
        }
    }

    /**
     * Creates a a default set of Layout meta data to be used on for creating starter pages layouts.
     */
    private fun makeDefaultLayouts(): List<GutenbergLayout> = if (demoModes) ArrayList<GutenbergLayout>().apply {
        addAll(aboutLayout.repeat(times))
        addAll(blogLayout.repeat(times))
        addAll(contactLayout.repeat(times))
        addAll(portfolioLayout.repeat(times))
        addAll(servicesLayout.repeat(times))
        addAll(teamLayout.repeat(times))
    } else listOf(aboutLayout, blogLayout, contactLayout, portfolioLayout, servicesLayout, teamLayout)
}
