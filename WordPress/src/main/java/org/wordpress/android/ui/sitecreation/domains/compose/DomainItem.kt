package org.wordpress.android.ui.sitecreation.domains.compose

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New.DomainUiState

private val CostColorDefault @Composable get() = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)

@Composable
fun DomainItem(
    uiState: DomainUiState,
) = with(uiState) {
    Column {
        Row(
            modifier = Modifier
                .clickable { onClick.invoke() }
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(domainName)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = cost,
                color = CostColorDefault,
            )
        }
        Divider(thickness = 0.5.dp)
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DomainItemPreview() {
    val uiState = DomainUiState(
        domainName = "sub.domain.com",
        cost = "Free",
        onClick = {}
    )
    AppTheme {
        Column {
            DomainItem(uiState.copy(cost = "$6/yr"))
            DomainItem(uiState)
            DomainItem(uiState.copy(cost = "$24/yr"))
        }
    }
}

