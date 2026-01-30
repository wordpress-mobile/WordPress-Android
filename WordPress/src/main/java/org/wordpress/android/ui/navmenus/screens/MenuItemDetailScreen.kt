package org.wordpress.android.ui.navmenus.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.navmenu.NavMenuItemModel
import org.wordpress.android.ui.navmenus.MenuItemDetailUiState
import org.wordpress.android.ui.navmenus.ParentItemOption

@Composable
fun MenuItemDetailScreen(
    state: MenuItemDetailUiState?,
    onTitleChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onParentChange: (Long) -> Unit,
    onTargetChange: (String) -> Unit,
    onCssClassesChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAttrTitleChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
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
        ItemFieldsCard(
            state = currentState,
            onTitleChange = onTitleChange,
            onUrlChange = onUrlChange,
            onParentChange = onParentChange,
            onTargetChange = onTargetChange,
            onCssClassesChange = onCssClassesChange,
            onDescriptionChange = onDescriptionChange,
            onAttrTitleChange = onAttrTitleChange
        )

        SaveButton(
            isSaving = currentState.isSaving,
            isDeleting = currentState.isDeleting,
            onClick = onSaveClick
        )

        if (!currentState.isNew) {
            DeleteButton(
                isSaving = currentState.isSaving,
                isDeleting = currentState.isDeleting,
                onClick = onDeleteClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemFieldsCard(
    state: MenuItemDetailUiState,
    onTitleChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onParentChange: (Long) -> Unit,
    onTargetChange: (String) -> Unit,
    onCssClassesChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAttrTitleChange: (String) -> Unit
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
                value = state.title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.menu_item_title_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (state.type == NavMenuItemModel.TYPE_CUSTOM) {
                OutlinedTextField(
                    value = state.url,
                    onValueChange = onUrlChange,
                    label = { Text(stringResource(R.string.menu_item_url_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            if (state.availableParents.isNotEmpty()) {
                ParentDropdown(
                    selectedParentId = state.parentId,
                    availableParents = state.availableParents,
                    onParentSelected = onParentChange
                )
            }

            OutlinedTextField(
                value = state.target,
                onValueChange = onTargetChange,
                label = { Text(stringResource(R.string.menu_item_target_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("_blank") }
            )

            OutlinedTextField(
                value = state.cssClasses,
                onValueChange = onCssClassesChange,
                label = { Text(stringResource(R.string.menu_item_css_classes_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.menu_item_description_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            OutlinedTextField(
                value = state.attrTitle,
                onValueChange = onAttrTitleChange,
                label = { Text(stringResource(R.string.menu_item_attr_title_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParentDropdown(
    selectedParentId: Long,
    availableParents: List<ParentItemOption>,
    onParentSelected: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = availableParents.find { it.id == selectedParentId }?.title
        ?: stringResource(R.string.menu_item_no_parent)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.menu_item_parent_label),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                textStyle = MaterialTheme.typography.bodyMedium
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_item_no_parent)) },
                    onClick = {
                        onParentSelected(0L)
                        expanded = false
                    }
                )
                availableParents.forEach { parent ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "  ".repeat(parent.indentLevel) + parent.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        onClick = {
                            onParentSelected(parent.id)
                            expanded = false
                        }
                    )
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

@Composable
private fun DeleteButton(
    isSaving: Boolean,
    isDeleting: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.error
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        enabled = !isSaving && !isDeleting
    ) {
        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        if (isDeleting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.error
            )
        } else {
            Text(stringResource(R.string.delete))
        }
    }
}
