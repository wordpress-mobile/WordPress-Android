package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.text.Html
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import org.wordpress.android.databinding.StatsBlockListGuideCardBinding
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemGuideCard
import org.wordpress.android.util.extensions.viewBinding

class GuideCardViewHolder(
    val parent: ViewGroup,
    val binding: StatsBlockListGuideCardBinding = parent.viewBinding(StatsBlockListGuideCardBinding::inflate)
) : BlockListItemViewHolder(binding.root) {
    fun bind(
        item: ListItemGuideCard
    ) = with(binding) {
        guideMessage.text = Html.fromHtml(item.text, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
}
