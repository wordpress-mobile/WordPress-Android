package org.wordpress.android.designsystem

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.designsystem.DesignSystemDataSource.buttonOptions

enum class DesignSystemScreen {
    Start
}
@Composable
fun DesignSystemStartScreen(
    modifier: Modifier = Modifier
){
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f, false)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(id = R.dimen.button_container_shadow_height)
                )
            ) {
                buttonOptions.forEach { item ->
                    SelectOptionButton(
                        labelResourceId = item,
                        onClick = {}
                    )
                }
            }
        }
    }
}

@Composable
fun SelectOptionButton(
    @StringRes labelResourceId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
){
    Button(
        onClick = onClick,
        modifier = modifier.widthIn(min = 250.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(stringResource(labelResourceId))
    }
}

@Preview
@Composable
fun StartDesignSystemPreview() {
    DesignSystemStartScreen(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.button_container_shadow_height))
    )
}
@Composable
fun DesignSystem() {
    DesignSystemStartScreen(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.button_container_shadow_height))
    )
}
