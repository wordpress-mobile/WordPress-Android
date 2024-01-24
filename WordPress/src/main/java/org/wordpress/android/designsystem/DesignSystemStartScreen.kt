package org.wordpress.android.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R

@Composable
fun DesignSystemStartScreen(
    onNextButtonClicked: () -> Unit,
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
                DesignSystemDataSource.startScreenButtonOptions.forEach { item ->
                    SelectOptionButton(
                        labelResourceId = item,
                        onClick = { onNextButtonClicked() }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun StartDesignSystemStartScreenPreview(){
    DesignSystemStartScreen(
        onNextButtonClicked = {},
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.button_container_shadow_height))
    )
}

