package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import org.wordpress.android.databinding.StatsBlockListActionCardBinding
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemActionCard
import org.wordpress.android.util.extensions.viewBinding

class ActionCardViewHolder(
    val parent: ViewGroup,
    val binding: StatsBlockListActionCardBinding = parent.viewBinding(StatsBlockListActionCardBinding::inflate)
) : BlockListItemViewHolder(binding.root) {
    fun bind(
        item: ListItemActionCard
    ) = with(binding) {
        actionTitle.text = parent.context.getString(item.titleResource)
        actionMessage.text = HtmlCompat.fromHtml(parent.context.getString(item.text), HtmlCompat.FROM_HTML_MODE_LEGACY)
        buttonPositive.setText(item.positiveButtonText)
        buttonPositive.setOnClickListener { item.positiveAction.click() }
        buttonNegative.setText(item.negativeButtonText)
        buttonNegative.setOnClickListener { item.negativeAction.click() }
    }
}
