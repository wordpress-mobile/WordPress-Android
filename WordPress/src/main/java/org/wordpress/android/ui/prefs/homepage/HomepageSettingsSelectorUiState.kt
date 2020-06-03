package org.wordpress.android.ui.prefs.homepage

import org.wordpress.android.R
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
        val (title, id, isHighlighted) = Builder.buildUiProperties(selectedItem)
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

    object Builder {
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
            val (title, id, isHighlighted) = buildUiProperties(selectedItem)
            return HomepageSettingsSelectorUiState(
                    data,
                    title,
                    id,
                    isHighlighted,
                    isExpanded = false
            )
        }

        fun buildUiProperties(pageUiModel: PageUiModel?): Triple<UiString, Int, Boolean> {
            return if (pageUiModel != null) {
                Triple(UiStringText(pageUiModel.title), pageUiModel.id, true)
            } else {
                Triple(UiStringRes(R.string.site_settings_select_page), 0, false)
            }
        }
    }
}
