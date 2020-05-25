package org.wordpress.android.ui.prefs.homepage

import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText

data class HomepageSettingsSelectorUiState(
    val data: List<PageUiModel>,
    val selectedItem: UiString,
    val selectedItemId: Int,
    val isHighlighted: Boolean = true,
    val isExpanded: Boolean = false
) {
    fun selectItem(updatedItemId: Int): HomepageSettingsSelectorUiState {
        val selectedItem = data.find { it.id == updatedItemId }
        val (title, id, isHighlighted) = selectedItem.buildUiProperties()
        return this.copy(
                selectedItem = title,
                selectedItemId = id,
                isHighlighted = isHighlighted,
                isExpanded = false
        )
    }

    fun getSelectedItemRemoteId(): Long? {
        return data.find { it.id == selectedItemId }?.remoteId
    }

    data class PageUiModel(val id: Int, val remoteId: Long, val title: String)

    companion object {
        fun build(
            pages: List<PageModel>,
            remoteId: Long?
        ): HomepageSettingsSelectorUiState {
            val data = pages.map {
                PageUiModel(
                        it.pageId,
                        it.remoteId,
                        it.title
                )
            }
            val selectedItem = data
                    .find { it.remoteId == remoteId }
            val (title, id, isHighlighted) = selectedItem.buildUiProperties()
            return HomepageSettingsSelectorUiState(
                    data,
                    title,
                    id,
                    isHighlighted,
                    isExpanded = false
            )
        }

        fun PageUiModel?.buildUiProperties(): Triple<UiString, Int, Boolean> {
            return if (this != null) {
                Triple(UiStringText(this.title), this.id, true)
            } else {
                Triple(UiStringRes(string.site_settings_select_page), 0, false)
            }
        }
    }
}
