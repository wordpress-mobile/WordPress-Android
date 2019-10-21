package org.wordpress.android.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.ui.main.SpeedDialAction.SD_ACTION_NEW_PAGE
import org.wordpress.android.ui.main.SpeedDialAction.SD_ACTION_NEW_POST

data class SpeedDialUiState(
    val speedDialState: SpeedDialState
)

enum class SpeedDialState {
    CLOSED,
    HIDDEN
}

enum class SpeedDialActionMenuItem(
    @IdRes val id: Int,
    @DrawableRes val iconId: Int,
    @StringRes val labelId: Int,
    val action: SpeedDialAction
) {
    NEW_POST(R.id.fab_add_new_post,
            R.drawable.ic_posts_white_24dp,
            R.string.my_site_speed_dial_add_post,
            SD_ACTION_NEW_POST
    ),
    NEW_PAGE(R.id.fab_add_new_page,
            R.drawable.ic_pages_white_24dp,
            R.string.my_site_speed_dial_add_page,
            SD_ACTION_NEW_PAGE
    );

    companion object {
        fun fromId(@IdRes id: Int): SpeedDialActionMenuItem = values().firstOrNull { it.id == id }
                    ?: throw IllegalArgumentException("SpeedDialAction wrong id $id")
    }
}
