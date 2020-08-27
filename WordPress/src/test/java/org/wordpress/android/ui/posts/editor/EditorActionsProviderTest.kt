package org.wordpress.android.ui.posts.editor

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.post.PostStatus

@RunWith(MockitoJUnitRunner::class)
class EditorActionsProviderTest {
    private val actionsProvider: EditorActionsProvider = EditorActionsProvider()

    @Test
    fun `All secondary actions but NONE have isVisible set to TRUE`() {
        for (action in SecondaryEditorAction.values()) {
            assertThat(action.isVisible).isEqualTo(action != SecondaryEditorAction.NONE)
        }
    }

    @Test
    fun `Verify actions of a DRAFT`() {
        // Arrange & Act
        val (primaryAction, secondaryAction) =
                getPrimaryAndSecondaryActions(PostStatus.DRAFT, userCanPublish = true)
        // Assert
        assertThat(primaryAction).isEqualTo(PrimaryEditorAction.PUBLISH_NOW)
        assertThat(secondaryAction).isEqualTo(SecondaryEditorAction.SAVE)
    }

    @Test
    fun `Verify actions of a DRAFT when the user doesn't have publishing rights`() {
        // Arrange & Act
        val (primaryAction, secondaryAction) =
                getPrimaryAndSecondaryActions(PostStatus.DRAFT, userCanPublish = false)
        // Assert
        assertThat(primaryAction).isEqualTo(PrimaryEditorAction.SUBMIT_FOR_REVIEW)
        assertThat(secondaryAction).isEqualTo(SecondaryEditorAction.NONE)
    }

    @Test
    fun `Verify actions of a PUBLISHED post`() {
        // Arrange & Act
        val (primaryAction, secondaryAction) =
                getPrimaryAndSecondaryActions(PostStatus.PUBLISHED, userCanPublish = true)
        // Assert
        assertThat(primaryAction).isEqualTo(PrimaryEditorAction.UPDATE)
        assertThat(secondaryAction).isEqualTo(SecondaryEditorAction.NONE)
    }

    @Test
    fun `Verify actions of a SCHEDULED post`() {
        // Arrange & Act
        val (primaryAction, secondaryAction) =
                getPrimaryAndSecondaryActions(PostStatus.SCHEDULED, userCanPublish = true)
        // Assert
        assertThat(primaryAction).isEqualTo(PrimaryEditorAction.SCHEDULE)
        assertThat(secondaryAction).isEqualTo(SecondaryEditorAction.PUBLISH_NOW)
    }

    @Test
    fun `Verify actions of a TRASHED post`() {
        // Arrange & Act
        val (primaryAction, secondaryAction) =
                getPrimaryAndSecondaryActions(PostStatus.TRASHED, userCanPublish = true)
        // Assert
        assertThat(primaryAction).isEqualTo(PrimaryEditorAction.SAVE)
        assertThat(secondaryAction).isEqualTo(SecondaryEditorAction.SAVE_AS_DRAFT)
    }

    @Test
    fun `Verify actions of a PENDING post`() {
        // Arrange & Act
        val (primaryAction, secondaryAction) =
                getPrimaryAndSecondaryActions(PostStatus.PENDING, userCanPublish = true)
        // Assert
        assertThat(primaryAction).isEqualTo(PrimaryEditorAction.SAVE)
        assertThat(secondaryAction).isEqualTo(SecondaryEditorAction.PUBLISH_NOW)
    }

    @Test
    fun `Verify actions of a PENDING post when the user doesn't have publishing rights`() {
        // Arrange & Act
        val (primaryAction, secondaryAction) =
                getPrimaryAndSecondaryActions(PostStatus.PENDING, userCanPublish = false)
        // Assert
        assertThat(primaryAction).isEqualTo(PrimaryEditorAction.SUBMIT_FOR_REVIEW)
        // TODO Would it make sense to have "SAVE/SAVE_AS_DRAFT" action here?
        assertThat(secondaryAction).isEqualTo(SecondaryEditorAction.NONE)
    }

    @Test
    fun `Verify actions of a PRIVATE post`() {
        // Arrange & Act
        val (primaryAction, secondaryAction) =
                getPrimaryAndSecondaryActions(PostStatus.PRIVATE, userCanPublish = true)
        // Assert
        assertThat(primaryAction).isEqualTo(PrimaryEditorAction.UPDATE)
        assertThat(secondaryAction).isEqualTo(SecondaryEditorAction.NONE)
    }

    @Test
    fun `Verify actions of an UNKNOWN post`() {
        // Arrange & Act
        val (primaryAction, secondaryAction) =
                getPrimaryAndSecondaryActions(PostStatus.UNKNOWN, userCanPublish = true)
        // Assert
        assertThat(primaryAction).isEqualTo(PrimaryEditorAction.UPDATE)
        assertThat(secondaryAction).isEqualTo(SecondaryEditorAction.SAVE_AS_DRAFT)
    }

    @Test
    fun `Verify actions of an UNKNOWN post when the user doesn't have the publishing rights`() {
        // Arrange & Act
        val (primaryAction, secondaryAction) =
                getPrimaryAndSecondaryActions(PostStatus.UNKNOWN, userCanPublish = false)
        // Assert
        assertThat(primaryAction).isEqualTo(PrimaryEditorAction.SUBMIT_FOR_REVIEW)
        assertThat(secondaryAction).isEqualTo(SecondaryEditorAction.NONE)
    }

    private fun getPrimaryAndSecondaryActions(
        postStatus: PostStatus,
        userCanPublish: Boolean
    ): Pair<PrimaryEditorAction, SecondaryEditorAction> = Pair(
            actionsProvider.getPrimaryAction(postStatus, userCanPublish),
            actionsProvider.getSecondaryAction(postStatus, userCanPublish)
    )
}
