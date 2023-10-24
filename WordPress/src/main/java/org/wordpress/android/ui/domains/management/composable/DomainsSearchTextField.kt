package org.wordpress.android.ui.domains.management.composable

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.wordpress.android.R

@Composable
fun DomainsSearchTextField(
    value: String,
    enabled: Boolean,
    @StringRes placeholder: Int,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        placeholder = { Text(stringResource(placeholder)) },
        shape = RoundedCornerShape(50),
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_search_white_24dp),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.outline,
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}
