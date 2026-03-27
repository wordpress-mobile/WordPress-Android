package org.wordpress.android.ui.postsrs

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uniffi.wp_api.PostFormat
import uniffi.wp_api.PostStatus
import java.util.Date

class PostRsSettingsUiStateTest {
    // region effectivePassword

    @Test
    fun `effectivePassword returns edited when set`() {
        val state = PostRsSettingsUiState(
            password = "original",
            editedPassword = "edited"
        )
        assertThat(state.effectivePassword)
            .isEqualTo("edited")
    }

    @Test
    fun `effectivePassword returns original when no edit`() {
        val state = PostRsSettingsUiState(
            password = "original"
        )
        assertThat(state.effectivePassword)
            .isEqualTo("original")
    }

    // endregion

    // region effectiveSticky

    @Test
    fun `effectiveSticky returns edited when set`() {
        val state = PostRsSettingsUiState(
            sticky = false,
            editedSticky = true
        )
        assertThat(state.effectiveSticky).isTrue()
    }

    @Test
    fun `effectiveSticky returns true original when no edit`() {
        val state = PostRsSettingsUiState(sticky = true)
        assertThat(state.effectiveSticky).isTrue()
    }

    @Test
    fun `effectiveSticky returns false original when no edit`() {
        val state = PostRsSettingsUiState(sticky = false)
        assertThat(state.effectiveSticky).isFalse()
    }

    // endregion

    // region effectiveSlug

    @Test
    fun `effectiveSlug returns edited when set`() {
        val state = PostRsSettingsUiState(
            slug = "original",
            editedSlug = "edited"
        )
        assertThat(state.effectiveSlug)
            .isEqualTo("edited")
    }

    @Test
    fun `effectiveSlug returns original when no edit`() {
        val state = PostRsSettingsUiState(
            slug = "original"
        )
        assertThat(state.effectiveSlug)
            .isEqualTo("original")
    }

    // endregion

    // region effectiveExcerpt

    @Test
    fun `effectiveExcerpt returns edited when set`() {
        val state = PostRsSettingsUiState(
            excerpt = "original",
            editedExcerpt = "edited"
        )
        assertThat(state.effectiveExcerpt)
            .isEqualTo("edited")
    }

    @Test
    fun `effectiveExcerpt returns original when no edit`() {
        val state = PostRsSettingsUiState(
            excerpt = "original"
        )
        assertThat(state.effectiveExcerpt)
            .isEqualTo("original")
    }

    // endregion

    // region effectiveDate

    @Test
    fun `effectiveDate returns edited when set`() {
        val original = Date(1000L)
        val edited = Date(2000L)
        val state = PostRsSettingsUiState(
            originalDate = original,
            editedDate = edited
        )
        assertThat(state.effectiveDate).isEqualTo(edited)
    }

    @Test
    fun `effectiveDate returns original when no edit`() {
        val original = Date(1000L)
        val state = PostRsSettingsUiState(
            originalDate = original
        )
        assertThat(state.effectiveDate).isEqualTo(original)
    }

    @Test
    fun `effectiveDate returns null when both null`() {
        val state = PostRsSettingsUiState()
        assertThat(state.effectiveDate).isNull()
    }

    // endregion

    // region effectiveAuthorId

    @Test
    fun `effectiveAuthorId returns edited when set`() {
        val state = PostRsSettingsUiState(
            authorId = 1L,
            editedAuthor = 2L
        )
        assertThat(state.effectiveAuthorId).isEqualTo(2L)
    }

    @Test
    fun `effectiveAuthorId returns original when no edit`() {
        val state = PostRsSettingsUiState(authorId = 1L)
        assertThat(state.effectiveAuthorId).isEqualTo(1L)
    }

    // endregion

    // region effectiveFeaturedImageId

    @Test
    fun `effectiveFeaturedImageId returns edited when set`() {
        val state = PostRsSettingsUiState(
            featuredImageId = 1L,
            editedFeaturedImageId = 2L
        )
        assertThat(state.effectiveFeaturedImageId)
            .isEqualTo(2L)
    }

    @Test
    fun `effectiveFeaturedImageId returns original when no edit`() {
        val state = PostRsSettingsUiState(
            featuredImageId = 1L
        )
        assertThat(state.effectiveFeaturedImageId)
            .isEqualTo(1L)
    }

    // endregion

    // region effectiveCategoryIds

    @Test
    fun `effectiveCategoryIds returns edited when set`() {
        val state = PostRsSettingsUiState(
            categoryIds = listOf(1L),
            editedCategoryIds = listOf(2L, 3L)
        )
        assertThat(state.effectiveCategoryIds)
            .containsExactly(2L, 3L)
    }

    @Test
    fun `effectiveCategoryIds returns original when no edit`() {
        val state = PostRsSettingsUiState(
            categoryIds = listOf(1L)
        )
        assertThat(state.effectiveCategoryIds)
            .containsExactly(1L)
    }

    // endregion

    // region effectiveTagIds

    @Test
    fun `effectiveTagIds returns edited when set`() {
        val state = PostRsSettingsUiState(
            tagIds = listOf(1L),
            editedTagIds = listOf(2L, 3L)
        )
        assertThat(state.effectiveTagIds)
            .containsExactly(2L, 3L)
    }

    @Test
    fun `effectiveTagIds returns original when no edit`() {
        val state = PostRsSettingsUiState(
            tagIds = listOf(1L)
        )
        assertThat(state.effectiveTagIds)
            .containsExactly(1L)
    }

    // endregion

    // region hasChanges

    @Test
    fun `hasChanges is false with no edits`() {
        val state = PostRsSettingsUiState()
        assertThat(state.hasChanges).isFalse()
    }

    @Test
    fun `hasChanges is true with editedStatus`() {
        val state = PostRsSettingsUiState(
            editedStatus = PostStatus.Draft
        )
        assertThat(state.hasChanges).isTrue()
    }

    @Test
    fun `hasChanges is true with editedPassword`() {
        val state = PostRsSettingsUiState(
            editedPassword = "pw"
        )
        assertThat(state.hasChanges).isTrue()
    }

    @Test
    fun `hasChanges is true with editedSticky`() {
        val state = PostRsSettingsUiState(
            editedSticky = true
        )
        assertThat(state.hasChanges).isTrue()
    }

    @Test
    fun `hasChanges is true with editedSlug`() {
        val state = PostRsSettingsUiState(
            editedSlug = "slug"
        )
        assertThat(state.hasChanges).isTrue()
    }

    @Test
    fun `hasChanges is true with editedExcerpt`() {
        val state = PostRsSettingsUiState(
            editedExcerpt = "excerpt"
        )
        assertThat(state.hasChanges).isTrue()
    }

    @Test
    fun `hasChanges is true with editedFormat`() {
        val state = PostRsSettingsUiState(
            editedFormat = PostFormat.Gallery
        )
        assertThat(state.hasChanges).isTrue()
    }

    @Test
    fun `hasChanges is true with editedDate`() {
        val state = PostRsSettingsUiState(
            editedDate = Date()
        )
        assertThat(state.hasChanges).isTrue()
    }

    @Test
    fun `hasChanges is true with editedAuthor`() {
        val state = PostRsSettingsUiState(
            editedAuthor = 1L
        )
        assertThat(state.hasChanges).isTrue()
    }

    @Test
    fun `hasChanges is true with editedFeaturedImageId`() {
        val state = PostRsSettingsUiState(
            editedFeaturedImageId = 1L
        )
        assertThat(state.hasChanges).isTrue()
    }

    @Test
    fun `hasChanges is true with editedCategoryIds`() {
        val state = PostRsSettingsUiState(
            editedCategoryIds = listOf(1L)
        )
        assertThat(state.hasChanges).isTrue()
    }

    @Test
    fun `hasChanges is true with editedTagIds`() {
        val state = PostRsSettingsUiState(
            editedTagIds = listOf(1L)
        )
        assertThat(state.hasChanges).isTrue()
    }

    // endregion
}
