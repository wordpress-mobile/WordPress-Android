package org.wordpress.android.ui.navmenus.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.navmenus.LocationUiModel
import org.wordpress.android.ui.navmenus.MenuDetailUiState

@Composable
fun MenuDetailScreen(
    state: MenuDetailUiState?,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAutoAddChange: (Boolean) -> Unit,
    onLocationToggle: (String) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentState = state ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MenuBasicInfoCard(
            name = currentState.name,
            description = currentState.description,
            autoAdd = currentState.autoAdd,
            onNameChange = onNameChange,
            onDescriptionChange = onDescriptionChange,
            onAutoAddChange = onAutoAddChange
        )

        if (currentState.availableLocations.isNotEmpty()) {
            LocationsCard(
                availableLocations = currentState.availableLocations,
                selectedLocations = currentState.selectedLocations,
                onLocationToggle = onLocationToggle
            )
        }

        SaveButton(
            isSaving = currentState.isSaving,
            isDeleting = currentState.isDeleting,
            onClick = onSaveClick
        )
    }
}

@Composable
private fun MenuBasicInfoCard(
    name: String,
    description: String,
    autoAdd: Boolean,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAutoAddChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.menu_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.menu_description_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.menu_auto_add_pages))
                Switch(
                    checked = autoAdd,
                    onCheckedChange = onAutoAddChange
                )
            }
        }
    }
}

@Composable
private fun LocationsCard(
    availableLocations: List<LocationUiModel>,
    selectedLocations: List<String>,
    onLocationToggle: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.menu_display_locations),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            availableLocations.forEach { location ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLocationToggle(location.name) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = location.name in selectedLocations,
                        onCheckedChange = { onLocationToggle(location.name) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = location.name)
                        Text(
                            text = location.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveButton(
    isSaving: Boolean,
    isDeleting: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isSaving && !isDeleting
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(stringResource(R.string.save))
        }
    }
}

// region Previews

private val sampleLocations = listOf(
    LocationUiModel(name = "primary", description = "Primary Menu", menuId = 0L),
    LocationUiModel(name = "footer", description = "Footer Menu", menuId = 0L),
    LocationUiModel(name = "social", description = "Social Links Menu", menuId = 0L)
)

@Preview(name = "New Menu Light", showBackground = true)
@Preview(name = "New Menu Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MenuDetailScreenNewPreview() {
    AppThemeM3 {
        MenuDetailScreen(
            state = MenuDetailUiState(
                isNew = true,
                availableLocations = sampleLocations
            ),
            onNameChange = {},
            onDescriptionChange = {},
            onAutoAddChange = {},
            onLocationToggle = {},
            onSaveClick = {}
        )
    }
}

@Preview(name = "Edit Menu Light", showBackground = true)
@Preview(name = "Edit Menu Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MenuDetailScreenEditPreview() {
    AppThemeM3 {
        MenuDetailScreen(
            state = MenuDetailUiState(
                menuId = 1L,
                name = "Main Menu",
                description = "Primary navigation for the site",
                autoAdd = true,
                selectedLocations = listOf("primary"),
                availableLocations = sampleLocations,
                isNew = false
            ),
            onNameChange = {},
            onDescriptionChange = {},
            onAutoAddChange = {},
            onLocationToggle = {},
            onSaveClick = {}
        )
    }
}

@Preview(name = "Saving Light", showBackground = true)
@Preview(name = "Saving Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MenuDetailScreenSavingPreview() {
    AppThemeM3 {
        MenuDetailScreen(
            state = MenuDetailUiState(
                name = "Main Menu",
                isSaving = true
            ),
            onNameChange = {},
            onDescriptionChange = {},
            onAutoAddChange = {},
            onLocationToggle = {},
            onSaveClick = {}
        )
    }
}

// endregion
