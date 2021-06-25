package org.wordpress.android.ui.bloggingreminders

import org.wordpress.android.R
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.HighEmphasisText
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Illustration
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Title
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.UiState.PrimaryButton
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import javax.inject.Inject

class PrologueBuilder
@Inject constructor() {
    fun buildUiItems(): List<BloggingRemindersItem> {
        return listOf(Illustration(R.drawable.img_illustration_celebration_150dp),
                Title(UiStringRes(R.string.set_your_blogging_goals_title)),
                HighEmphasisText(UiStringRes(R.string.set_your_blogging_goals_message))
        )
    }

    fun buildUiItemsForSettings(): List<BloggingRemindersItem> {
        return listOf(
                Illustration(R.drawable.img_illustration_celebration_150dp),
                Title(UiStringRes(R.string.set_your_blogging_goals_title))
        )
    }

    fun buildPrimaryButton(
        onContinue: () -> Unit
    ): PrimaryButton {
        return PrimaryButton(
                UiStringRes(R.string.set_your_blogging_goals_button),
                enabled = true,
                ListItemInteraction.create(onContinue)
        )
    }
}
