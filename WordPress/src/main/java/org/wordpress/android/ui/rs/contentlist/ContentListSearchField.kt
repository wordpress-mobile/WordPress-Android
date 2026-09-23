package org.wordpress.android.ui.rs.contentlist

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import org.wordpress.android.R

/**
 * The query field an rs content list puts in its top bar while searching.
 *
 * Only the field: each screen builds its own top bar around it, and each owns its focus request,
 * because when to raise the keyboard differs - the comments list must not raise it again when its
 * selection bar hands back to search. Pass a `focusRequester` in through [modifier].
 */
@Composable
fun ContentListSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    @StringRes placeholderResId: Int,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(placeholderResId)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/** Empties a non-blank query; draws nothing while the query is already empty. */
@Composable
fun ContentListSearchClearButton(query: String, onClear: () -> Unit) {
    if (query.isEmpty()) return
    IconButton(onClick = onClear) {
        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
    }
}
