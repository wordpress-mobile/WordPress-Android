package org.wordpress.android.ui.mysite.cards.quicklinksitem

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.databinding.QuickLinkItemBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinksItem.QuickLinkItem
import org.wordpress.android.util.extensions.viewBinding

class QuickLinksItemViewHolder(
    parent: ViewGroup,
    private val binding: QuickLinkItemBinding = parent.viewBinding(QuickLinkItemBinding::inflate)
) : RecyclerView.ViewHolder(binding.root) {
    init {
        ViewCompat.setAccessibilityDelegate(
            binding.quickLinkItemRoot,
            object : androidx.core.view.AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfoCompat
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.className = Button::class.java.name
                }
            }
        )
    }

    fun onBind(item: QuickLinkItem) = with(binding) {
        quickLinkIcon.setImageResource(item.icon)
        if (item.disableTint) {
            ImageViewCompat.setImageTintList(quickLinkIcon, null)
        }
        quickLinkLabel.setText(item.label.stringRes)
        quickLinkBetaBadge.visibility =
            if (item.showBetaBadge) View.VISIBLE else View.GONE
        quickLinkItemRoot.setOnClickListener { item.onClick.click() }
    }
}
