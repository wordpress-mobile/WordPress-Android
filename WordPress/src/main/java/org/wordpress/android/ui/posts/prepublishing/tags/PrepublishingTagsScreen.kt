package org.wordpress.android.ui.posts.prepublishing.tags

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.unit.Margin

private val OUTLINE_SHAPE = RoundedCornerShape(4.dp)
private const val SUGGESTIONS_MAX_HEIGHT_DP = 320
private const val INPUT_MIN_WIDTH_DP = 120
private const val CLOSE_ICON_SIZE_DP = 18

@Composable
fun PrepublishingTagsScreen(
    uiState: PrepublishingTagsUiState,
    onInputChanged: (String) -> Unit,
    onTagAdded: (String) -> Unit,
    onTagRemoved: (String) -> Unit,
    onLastTagRemoved: () -> Unit,
    modifier: Modifier = Modifier
) {
    var input by rememberSaveable { mutableStateOf("") }

    fun commit(rawTag: String) {
        val tag = rawTag.trim()
        if (tag.isNotEmpty()) onTagAdded(tag)
        input = ""
        onInputChanged("")
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Margin.ExtraLarge.value),
        verticalArrangement = Arrangement.spacedBy(Margin.ExtraLarge.value)
    ) {
        Text(
            text = stringResource(R.string.prepublishing_tags_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        TagsChipInputField(
            tags = uiState.selectedTags,
            input = input,
            onInputChange = { newValue ->
                when {
                    newValue.endsWith(",") || newValue.endsWith("\n") -> commit(newValue.dropLast(1))
                    newValue.contains(",") -> {
                        val parts = newValue.split(",")
                        parts.dropLast(1).forEach { part -> part.trim().takeIf { it.isNotEmpty() }?.let(onTagAdded) }
                        input = parts.last()
                        onInputChanged(parts.last())
                    }
                    else -> {
                        input = newValue
                        onInputChanged(newValue)
                    }
                }
            },
            onImeAdd = { commit(input) },
            onChipRemove = onTagRemoved,
            onBackspaceWhenEmpty = { if (input.isEmpty()) onLastTagRemoved() }
        )

        if (uiState.suggestions.isNotEmpty()) {
            TagSuggestionsList(
                suggestions = uiState.suggestions,
                onSuggestionClick = { suggestion -> commit(suggestion) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagsChipInputField(
    tags: List<String>,
    input: String,
    onInputChange: (String) -> Unit,
    onImeAdd: () -> Unit,
    onChipRemove: (String) -> Unit,
    onBackspaceWhenEmpty: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = OUTLINE_SHAPE)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusRequester.requestFocus() }
            .padding(Margin.Medium.value),
        horizontalArrangement = Arrangement.spacedBy(Margin.Medium.value),
        verticalArrangement = Arrangement.spacedBy(Margin.Small.value)
    ) {
        tags.forEach { tag ->
            InputChip(
                selected = false,
                onClick = { onChipRemove(tag) },
                label = { Text(tag) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.prepublishing_tags_remove_tag_desc, tag),
                        modifier = Modifier.size(CLOSE_ICON_SIZE_DP.dp)
                    )
                }
            )
        }

        BasicTextField(
            value = input,
            onValueChange = onInputChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onImeAdd() }),
            modifier = Modifier
                .widthIn(min = INPUT_MIN_WIDTH_DP.dp)
                .align(Alignment.CenterVertically)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.Backspace && event.type == KeyEventType.KeyDown && input.isEmpty()) {
                        onBackspaceWhenEmpty()
                        true
                    } else {
                        false
                    }
                },
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (input.isEmpty() && tags.isEmpty()) {
                        Text(
                            text = stringResource(R.string.prepublishing_tags_add_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}

@Composable
private fun TagSuggestionsList(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = SUGGESTIONS_MAX_HEIGHT_DP.dp)
    ) {
        items(suggestions, key = { it }) { suggestion ->
            Text(
                text = suggestion,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSuggestionClick(suggestion) }
                    .padding(horizontal = Margin.Small.value, vertical = Margin.Large.value)
            )
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun PrepublishingTagsScreenPreview() {
    AppThemeM3 {
        PrepublishingTagsScreen(
            uiState = PrepublishingTagsUiState(
                selectedTags = listOf("test", "nature"),
                suggestions = listOf("travel", "food", "photography")
            ),
            onInputChanged = {},
            onTagAdded = {},
            onTagRemoved = {},
            onLastTagRemoved = {},
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        )
    }
}
