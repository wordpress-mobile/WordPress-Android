package org.wordpress.android.ui.domains.management.composable

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.themes.M3Theme

@Composable
fun DomainsSearchTextField(
    value: String,
    enabled: Boolean,
    @StringRes placeholder: Int,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        placeholder = { Text(stringResource(placeholder)) },
        shape = RoundedCornerShape(4.dp),
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_search_white_24dp),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.outline,
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cross_in_circle_white_24dp),
                        contentDescription = stringResource(R.string.domain_management_clear_search),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, group = "Empty")
@Composable
fun PreviewDomainsSearchTextFieldEmpty() {
    M3Theme {
        DomainsSearchTextField(
            value = "",
            onValueChange = {},
            enabled = true,
            placeholder = R.string.domain_management_search_your_domains,
        )
    }
}
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, group = "Populated")
@Composable
fun PreviewDomainsSearchTextFieldPopulated() {
    M3Theme {
        DomainsSearchTextField(
            value = "Cool domain",
            onValueChange = {},
            enabled = true,
            placeholder = R.string.domain_management_search_your_domains,
        )
    }
}
