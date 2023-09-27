package org.wordpress.android.ui.domains.management

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.domains.management.DomainStatus.Active
import org.wordpress.android.ui.domains.management.DomainStatus.Expired
import java.time.LocalDate

@Composable
fun DomainListCard(uiState: DomainCardUiState) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            verticalAlignment = CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = uiState.domain,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = uiState.title,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                StatusRow(
                    status = uiState.status,
                    expiry = uiState.expiry,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right_white_24dp),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
        uiState.notice?.let {
            Divider(thickness = Dp.Hairline)
            Text(
                text = it,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .padding(16.dp),
            )
        }
    }
}


@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DomainListCardPreview() {
    M3Theme {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            DomainListCard(uiState = DomainCardUiState(
                domain = "domain.cool",
                title = "A cool website",
                status = Active,
                expiry = LocalDate.of(2024,8,15),
            ))
            DomainListCard(uiState = DomainCardUiState(
                domain = "domain.cool",
                title = "A cool website",
                status = Expired,
                expiry = LocalDate.of(2024,8,15),
            ))
        }
    }
}
