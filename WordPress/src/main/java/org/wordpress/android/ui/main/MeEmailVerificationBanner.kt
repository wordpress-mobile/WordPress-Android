package org.wordpress.android.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun MeEmailVerificationBanner(
    verificationState: MeViewModel.EmailVerificationState
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        // First row with an icon and text
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = "Home Icon",
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(text = "Welcome to Our App!", style = MaterialTheme.typography.bodyMedium)
        }

        // Spacer to add space between the rows
        Spacer(modifier = Modifier.height(8.dp))

        // Second row of text
        Text(text = "Explore features and more!", style = MaterialTheme.typography.bodyMedium)

        // Spacer to add space between the rows
        Spacer(modifier = Modifier.height(8.dp))

        // Third row of text
        Text(text = "Get started by browsing the app.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview
@Composable
fun MeGravatarQuickEditorPreview() {
    MeEmailVerificationBanner(MeViewModel.EmailVerificationState.VERIFIED)
}
