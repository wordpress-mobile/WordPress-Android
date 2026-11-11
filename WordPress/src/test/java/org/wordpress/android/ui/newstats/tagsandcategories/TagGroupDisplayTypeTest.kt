package org.wordpress.android.ui.newstats.tagsandcategories

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TagGroupDisplayTypeTest {
    @Test
    fun `when all tags are categories, then CATEGORY`() {
        val tags = listOf(
            TagUiItem(
                name = "Cat1",
                tagType = "category"
            ),
            TagUiItem(
                name = "Cat2",
                tagType = "category"
            )
        )

        val result = TagGroupDisplayType.fromTags(tags)

        assertThat(result)
            .isEqualTo(TagGroupDisplayType.CATEGORY)
    }

    @Test
    fun `when all tags are tags, then TAG`() {
        val tags = listOf(
            TagUiItem(
                name = "Tag1",
                tagType = "tag"
            ),
            TagUiItem(
                name = "Tag2",
                tagType = "tag"
            )
        )

        val result = TagGroupDisplayType.fromTags(tags)

        assertThat(result)
            .isEqualTo(TagGroupDisplayType.TAG)
    }

    @Test
    fun `when mixed types, then MIXED`() {
        val tags = listOf(
            TagUiItem(
                name = "Tag1",
                tagType = "tag"
            ),
            TagUiItem(
                name = "Cat1",
                tagType = "category"
            )
        )

        val result = TagGroupDisplayType.fromTags(tags)

        assertThat(result)
            .isEqualTo(TagGroupDisplayType.MIXED)
    }

    @Test
    fun `when single category, then CATEGORY`() {
        val tags = listOf(
            TagUiItem(
                name = "Cat1",
                tagType = "category"
            )
        )

        val result = TagGroupDisplayType.fromTags(tags)

        assertThat(result)
            .isEqualTo(TagGroupDisplayType.CATEGORY)
    }

    @Test
    fun `when single tag, then TAG`() {
        val tags = listOf(
            TagUiItem(
                name = "Tag1",
                tagType = "tag"
            )
        )

        val result = TagGroupDisplayType.fromTags(tags)

        assertThat(result)
            .isEqualTo(TagGroupDisplayType.TAG)
    }

    @Test
    fun `when empty list, then CATEGORY`() {
        val tags = emptyList<TagUiItem>()

        val result = TagGroupDisplayType.fromTags(tags)

        assertThat(result)
            .isEqualTo(TagGroupDisplayType.CATEGORY)
    }

    @Test
    fun `when fromTagType with category, then CATEGORY`() {
        val result = TagGroupDisplayType
            .fromTagType("category")

        assertThat(result)
            .isEqualTo(TagGroupDisplayType.CATEGORY)
    }

    @Test
    fun `when fromTagType with tag, then TAG`() {
        val result = TagGroupDisplayType
            .fromTagType("tag")

        assertThat(result)
            .isEqualTo(TagGroupDisplayType.TAG)
    }

    @Test
    fun `when fromTagType with unknown, then TAG`() {
        val result = TagGroupDisplayType
            .fromTagType("unknown")

        assertThat(result)
            .isEqualTo(TagGroupDisplayType.TAG)
    }
}
