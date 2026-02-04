package org.wordpress.android.ui.navmenus.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.navmenus.LinkableItemOption
import org.wordpress.android.ui.navmenus.LinkableItemsState
import org.wordpress.android.ui.navmenus.MenuItemDetailUiState
import org.wordpress.android.ui.navmenus.MenuItemTypeOption
import org.wordpress.android.ui.navmenus.ParentItemOption

@Composable
fun MenuItemDetailScreen(
    state: MenuItemDetailUiState?,
    onTitleChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onParentChange: (Long) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onTypeChange: (MenuItemTypeOption) -> Unit,
    onLinkableItemChange: (LinkableItemOption) -> Unit,
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
        ItemFieldsCard(
            state = currentState,
            onTitleChange = onTitleChange,
            onUrlChange = onUrlChange,
            onParentChange = onParentChange,
            onDescriptionChange = onDescriptionChange,
            onTypeChange = onTypeChange,
            onLinkableItemChange = onLinkableItemChange
        )

        SaveButton(
            isSaving = currentState.isSaving,
            isDeleting = currentState.isDeleting,
            onClick = onSaveClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemFieldsCard(
    state: MenuItemDetailUiState,
    onTitleChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onParentChange: (Long) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onTypeChange: (MenuItemTypeOption) -> Unit,
    onLinkableItemChange: (LinkableItemOption) -> Unit
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
            // Show type dropdown only for new items
            if (state.isNew) {
                TypeDropdown(
                    selectedType = state.selectedTypeOption,
                    onTypeSelected = onTypeChange
                )
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.menu_item_title_label)) },
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

            if (state.selectedTypeOption == MenuItemTypeOption.CUSTOM_LINK) {
                OutlinedTextField(
                    value = state.url,
                    onValueChange = onUrlChange,
                    label = { Text(stringResource(R.string.menu_item_url_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            } else if (state.isNew) {
                LinkableItemDropdown(
                    linkableItemsState = state.linkableItemsState,
                    selectedItem = state.selectedLinkableItem,
                    selectedType = state.selectedTypeOption,
                    onItemSelected = onLinkableItemChange
                )
            }

            if (state.availableParents.isNotEmpty()) {
                ParentDropdown(
                    selectedParentId = state.parentId,
                    availableParents = state.availableParents,
                    onParentSelected = onParentChange
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeDropdown(
    selectedType: MenuItemTypeOption,
    onTypeSelected: (MenuItemTypeOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.menu_item_type_label),
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
                value = stringResource(selectedType.labelResId),
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
                MenuItemTypeOption.entries.forEach { typeOption ->
                    DropdownMenuItem(
                        text = { Text(stringResource(typeOption.labelResId)) },
                        onClick = {
                            onTypeSelected(typeOption)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkableItemDropdown(
    linkableItemsState: LinkableItemsState,
    selectedItem: LinkableItemOption?,
    selectedType: MenuItemTypeOption,
    onItemSelected: (LinkableItemOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val placeholderText = when (selectedType) {
        MenuItemTypeOption.POST -> stringResource(R.string.menu_item_select_post)
        MenuItemTypeOption.PAGE -> stringResource(R.string.menu_item_select_page)
        MenuItemTypeOption.CATEGORY -> stringResource(R.string.menu_item_select_category)
        MenuItemTypeOption.TAG -> stringResource(R.string.menu_item_select_tag)
        MenuItemTypeOption.CUSTOM_LINK -> stringResource(R.string.menu_item_select_item)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.menu_item_link_to_label),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (!linkableItemsState.isLoading) expanded = it }
        ) {
            OutlinedTextField(
                value = when {
                    linkableItemsState.isLoading -> stringResource(R.string.menu_item_loading_items)
                    selectedItem != null -> selectedItem.title
                    else -> placeholderText
                },
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    if (linkableItemsState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                textStyle = MaterialTheme.typography.bodyMedium,
                enabled = !linkableItemsState.isLoading
            )

            if (!linkableItemsState.isLoading) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    if (linkableItemsState.items.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.menu_item_no_items_found),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        linkableItemsState.items.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = item.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                onClick = {
                                    onItemSelected(item)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
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
                    val parentDescription = stringResource(
                        R.string.menu_item_accessibility_description,
                        parent.indentLevel + 1,
                        parent.title,
                        ""
                    ).trim()
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = parent.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = (parent.indentLevel * 16).dp)
                            )
                        },
                        onClick = {
                            onParentSelected(parent.id)
                            expanded = false
                        },
                        modifier = Modifier.semantics {
                            contentDescription = parentDescription
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
