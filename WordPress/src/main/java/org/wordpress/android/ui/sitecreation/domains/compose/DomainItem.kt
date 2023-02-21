package org.wordpress.android.ui.sitecreation.domains.compose

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New.DomainUiState

@Composable
fun DomainItem(
    uiState: DomainUiState,
) = with(uiState) {
    Row(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 16.dp)
    )
    {
        Text(domainName)
        Spacer(modifier = Modifier.weight(1f))
        Text(cost)
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DomainItemPreview() {
    val uiState = DomainUiState(
        domainName = "sub.domain.com",
        cost = "Free",
    )
    AppTheme {
        DomainItem(uiState)
    }
}

