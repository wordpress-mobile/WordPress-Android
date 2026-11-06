package org.wordpress.android.fluxc.network.rest.wpapi.post

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.DateTimeUtils
import uniffi.wp_api.AnyPostWithEditContext
import uniffi.wp_api.PostContentWithEditContext
import uniffi.wp_api.PostFormat
import uniffi.wp_api.PostGuidWithEditContext
import uniffi.wp_api.PostStatus
import uniffi.wp_api.PostTitleWithEditContext
import uniffi.wp_api.SparsePostExcerpt
import uniffi.wp_api.WpDateString
import java.util.Date

class RsPostMapperTest {
    private val mapper = RsPostMapper()
    private val site = SiteModel().apply {
        id = 7
        siteId = 99L
    }

    /**
     * The regression guard for CMM-2392: `rendered` is the summary WordPress generates from the
     * content when the post has no excerpt, so mapping it would hand the editor an excerpt the
     * author never wrote - which the next Update would then save to the server.
     */
    @Test
    fun `blank raw excerpt does not fall back to the auto-generated rendered excerpt`() {
        val mapped = map(rsPost(excerpt = SparsePostExcerpt("", AUTO_GENERATED, false)))

        assertThat(mapped.excerpt).isEmpty()
    }

    @Test
    fun `author-written excerpt is mapped as-is`() {
        val mapped = map(rsPost(excerpt = SparsePostExcerpt("Written by hand", AUTO_GENERATED, false)))

        assertThat(mapped.excerpt).isEqualTo("Written by hand")
    }

    @Test
    fun `missing excerpt maps to an empty excerpt`() {
        assertThat(map(rsPost(excerpt = null)).excerpt).isEmpty()
    }

    @Test
    fun `null raw excerpt does not fall back to the rendered excerpt`() {
        assertThat(map(rsPost(excerpt = SparsePostExcerpt(null, AUTO_GENERATED, false))).excerpt).isEmpty()
    }

    @Test
    fun `raw title and content win over the rendered ones`() {
        val mapped = map(rsPost())

        assertThat(mapped.title).isEqualTo("Raw title")
        assertThat(mapped.content).isEqualTo("Raw content")
    }

    @Test
    fun `a blank raw title falls back to the rendered title`() {
        val mapped = map(rsPost(title = PostTitleWithEditContext("", "Rendered title")))

        assertThat(mapped.title).isEqualTo("Rendered title")
    }

    @Test
    fun `identity and dates come from the server copy`() {
        val mapped = map(rsPost())

        assertThat(mapped.remotePostId).isEqualTo(42L)
        assertThat(mapped.localSiteId).isEqualTo(7)
        assertThat(mapped.remoteSiteId).isEqualTo(99L)
        assertThat(DateTimeUtils.dateUTCFromIso8601(mapped.dateCreated)).isEqualTo(DATE_CREATED)
        assertThat(DateTimeUtils.dateUTCFromIso8601(mapped.lastModified)).isEqualTo(DATE_MODIFIED)
        assertThat(DateTimeUtils.dateUTCFromIso8601(mapped.remoteLastModified)).isEqualTo(DATE_MODIFIED)
    }

    @Test
    fun `taxonomies are taken from the response, tag names from the caller`() {
        val mapped = mapper.applyTo(PostModel(), rsPost(), site, listOf("alpha", "beta"), isPage = false)

        assertThat(mapped.categoryIdList).containsExactly(5L)
        assertThat(mapped.tagNameList).containsExactly("alpha", "beta")
    }

    @Test
    fun `a post with no categories maps to an empty category list`() {
        assertThat(map(rsPost(categories = null)).categoryIdList).isEmpty()
    }

    @Test
    fun `a page carries its parent, a post does not`() {
        val page = mapper.applyTo(PostModel(), rsPost(parent = 3L), site, emptyList(), isPage = true)
        val post = mapper.applyTo(PostModel(), rsPost(parent = 3L), site, emptyList(), isPage = false)

        assertThat(page.isPage).isTrue()
        assertThat(page.parentId).isEqualTo(3L)
        assertThat(post.isPage).isFalse()
        assertThat(post.parentId).isEqualTo(0L)
    }

    @Test
    fun `every known status round-trips through its slug`() {
        val statuses = listOf(
            PostStatus.Publish to "publish",
            PostStatus.Draft to "draft",
            PostStatus.Pending to "pending",
            PostStatus.Private to "private",
            PostStatus.Future to "future",
            PostStatus.Trash to "trash",
        )
        statuses.forEach { (status, slug) ->
            assertThat(mapper.statusToSlug(status)).isEqualTo(slug)
            assertThat(mapper.statusFromSlug(slug)).isEqualTo(status)
        }
    }

    /**
     * A site can register its own statuses, and PostModel stores whichever string the site uses, so
     * an unrecognised slug has to survive the trip instead of being flattened into a draft.
     */
    @Test
    fun `a plugin-registered status is forwarded verbatim`() {
        assertThat(mapper.statusFromSlug("in-review")).isEqualTo(PostStatus.Custom("in-review"))
        assertThat(mapper.statusToSlug(PostStatus.Custom("in-review"))).isEqualTo("in-review")
    }

    @Test
    fun `an absent status reads as a draft`() {
        assertThat(mapper.statusFromSlug(null)).isEqualTo(PostStatus.Draft)
        assertThat(mapper.statusFromSlug("")).isEqualTo(PostStatus.Draft)
        assertThat(mapper.statusToSlug(null)).isEqualTo("draft")
    }

    @Test
    fun `every known post format round-trips through its slug`() {
        val formats = listOf(
            PostFormat.Standard to "standard",
            PostFormat.Aside to "aside",
            PostFormat.Audio to "audio",
            PostFormat.Chat to "chat",
            PostFormat.Gallery to "gallery",
            PostFormat.Image to "image",
            PostFormat.Link to "link",
            PostFormat.Quote to "quote",
            PostFormat.Status to "status",
            PostFormat.Video to "video",
        )
        formats.forEach { (format, slug) ->
            assertThat(mapper.formatToSlug(format)).isEqualTo(slug)
            assertThat(mapper.formatFromSlug(slug)).isEqualTo(format)
        }
    }

    @Test
    fun `a custom post format is forwarded verbatim and an absent one sends nothing`() {
        assertThat(mapper.formatFromSlug("portfolio")).isEqualTo(PostFormat.Custom("portfolio"))
        assertThat(mapper.formatToSlug(PostFormat.Custom("portfolio"))).isEqualTo("portfolio")
        assertThat(mapper.formatFromSlug(null)).isNull()
        assertThat(mapper.formatFromSlug("")).isNull()
    }

    private fun map(post: AnyPostWithEditContext): PostModel =
        mapper.applyTo(PostModel(), post, site, emptyList(), isPage = false)

    @Suppress("LongParameterList")
    private fun rsPost(
        status: PostStatus = PostStatus.Draft,
        title: PostTitleWithEditContext? = PostTitleWithEditContext("Raw title", "Rendered title"),
        excerpt: SparsePostExcerpt? = SparsePostExcerpt("", AUTO_GENERATED, false),
        format: PostFormat? = PostFormat.Standard,
        categories: List<Long>? = listOf(5L),
        tags: List<Long>? = listOf(9L),
        parent: Long? = null,
    ) = AnyPostWithEditContext(
        id = 42L,
        date = WpDateString("2026-10-01T10:00:00"),
        dateGmt = DATE_CREATED,
        guid = PostGuidWithEditContext(null, "https://example.com/?p=42"),
        link = "https://example.com/raw-title",
        modified = WpDateString("2026-10-02T11:00:00"),
        modifiedGmt = DATE_MODIFIED,
        slug = "raw-title",
        status = status,
        postType = "post",
        password = null,
        permalinkTemplate = null,
        generatedSlug = null,
        title = title,
        content = PostContentWithEditContext("Raw content", "<p>Rendered content</p>", false, null),
        author = 3L,
        excerpt = excerpt,
        featuredMedia = 11L,
        commentStatus = null,
        pingStatus = null,
        format = format,
        meta = null,
        sticky = false,
        template = "",
        categories = categories,
        tags = tags,
        parent = parent,
        menuOrder = null,
        additionalFields = null,
    )

    companion object {
        private const val AUTO_GENERATED = "<p>Raw content&hellip;</p>"
        private val DATE_CREATED = Date(1_760_000_000_000L)
        private val DATE_MODIFIED = Date(1_760_003_600_000L)
    }
}
