package org.wordpress.android.ui.mysite.cards.jpfullplugininstall

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.compose.ui.res.stringResource
import org.wordpress.android.R
import org.wordpress.android.databinding.JpInstallFullPluginCardBinding
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.jetpackplugininstall.fullplugin.onboarding.compose.component.PluginDescription
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.JetpackInstallFullPluginCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.extensions.viewBinding

class JetpackInstallFullPluginCardViewHolder(
    parent: ViewGroup,
) : MySiteCardAndItemViewHolder<JpInstallFullPluginCardBinding>(
    parent.viewBinding(JpInstallFullPluginCardBinding::inflate)
) {
    fun bind(card: JetpackInstallFullPluginCard) = with(binding) {
        jpInstallFullPluginCardContentComposable.setContent {
            AppThemeM2 {
                PluginDescription(
                    siteString = stringResource(R.string.jetpack_full_plugin_install_onboarding_description_this_site),
                    pluginNames = card.pluginNames,
                    useConciseText = true,
                )
            }
        }

        jpInstallFullPluginCardMenu.setOnClickListener {
            showMenu(card.onHideMenuItemClick, it)
        }

        jpInstallFullPluginCardLearnMore.setOnClickListener {
            card.onLearnMoreClick.click()
        }
    }

    private fun showMenu(
        onHideClick: ListItemInteraction,
        anchor: View,
    ) {
        val popupMenu = PopupMenu(itemView.context, anchor)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.jp_install_full_plugin_card_menu_item_hide -> {
                    onHideClick.click()
                    return@setOnMenuItemClickListener true
                }
                else -> return@setOnMenuItemClickListener true
            }
        }
        popupMenu.inflate(R.menu.jp_install_full_plugin_card_menu)
        popupMenu.show()
    }
}
