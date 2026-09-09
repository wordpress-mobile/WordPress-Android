package org.wordpress.android.ui.compose.utils

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.wordpress.android.BuildConfig

private const val RS_DEBUG_SUFFIX = "(RS)"

/**
 * Title for a wordpress-rs screen. Debug builds append an "(RS)" suffix so these screens can be
 * told apart at a glance from their FluxC-backed counterparts, which are reachable from other
 * entry points on the same site. Release builds get the unmodified title.
 */
@Composable
fun rsDebugTitle(@StringRes titleResId: Int): String {
    val title = stringResource(titleResId)
    return if (BuildConfig.DEBUG) "$title $RS_DEBUG_SUFFIX" else title
}
