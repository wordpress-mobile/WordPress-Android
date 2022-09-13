package org.wordpress.android.ui.compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString

/**
 * Loads the text from a UiString.
 *
 * @param uiString the UiString object.
 * @return the string data loaded from the UiString.
 */
@Composable
@ReadOnlyComposable
fun uiStringText(uiString: UiString) = UiHelpers().getTextOfUiString(LocalContext.current, uiString).toString()
