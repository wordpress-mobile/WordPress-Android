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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.unit.Margin

private val OUTLINE_SHAPE = RoundedCornerShape(4.dp)
private const val SUGGESTIONS_MAX_HEIGHT_DP = 320
private const val INPUT_MIN_WIDTH_DP = 120
private const val CLOSE_ICON_SIZE_DP = 18

// Invisible zero-width space kept as the first character of the input field. Deleting into an empty
// field removes it, which fires onValueChange even on soft keyboards (they emit a delete to the
// InputConnection rather than a Key.Backspace event), letting us detect "backspace on empty".
private const val SENTINEL = "\u200B"

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

    fun addTag(rawTag: String) {
        val tag = rawTag.trim()
        if (tag.isNotEmpty()) onTagAdded(tag)
    }

    fun commit(rawTag: String) {
        addTag(rawTag)
        input = ""
        onInputChanged("")
    }

    // After process death the composable's [input] is restored (rememberSaveable) while the
    // recreated ViewModel starts with an empty filter. Push the restored text back so a dismiss
    // that commits the pending tag (commitPendingTag) does not silently drop it.
    LaunchedEffect(Unit) {
        if (input.isNotEmpty()) onInputChanged(input)
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
                if (newValue.contains(",") || newValue.contains("\n")) {
                    // Split on every comma/newline so multi-tag input like "a,b," becomes separate
                    // tags instead of one tag containing commas. The final segment stays in the
                    // input as the in-progress tag.
                    val parts = newValue.split(",", "\n")
                    parts.dropLast(1).forEach { part -> addTag(part) }
                    val remainder = parts.last()
                    input = remainder
                    onInputChanged(remainder)
                } else {
                    input = newValue
                    onInputChanged(newValue)
                }
            },
            onImeAdd = { commit(input) },
            onChipRemove = onTagRemoved,
            onBackspaceWhenEmpty = onLastTagRemoved
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

    // The field text is always prefixed with the invisible [SENTINEL]. See SENTINEL for why.
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(SENTINEL, selection = TextRange(SENTINEL.length)))
    }

    // Keep the field text in sync with the hoisted [input] (cleared after a commit, restored after
    // process death), always re-adding the sentinel and placing the cursor at the end.
    LaunchedEffect(input) {
        if (fieldValue.text.removePrefix(SENTINEL) != input) {
            val text = SENTINEL + input
            fieldValue = TextFieldValue(text, selection = TextRange(text.length))
        }
    }

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
            value = fieldValue,
            onValueChange = { newValue ->
                if (newValue.text.startsWith(SENTINEL)) {
                    fieldValue = newValue
                    onInputChange(newValue.text.removePrefix(SENTINEL))
                } else {
                    // The sentinel was deleted: a backspace at the very start of the field.
                    if (input.isEmpty()) onBackspaceWhenEmpty() else onInputChange(newValue.text)
                    // Never leave the field without its sentinel.
                    fieldValue = TextFieldValue(SENTINEL, selection = TextRange(SENTINEL.length))
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onImeAdd() }),
            modifier = Modifier
                .widthIn(min = INPUT_MIN_WIDTH_DP.dp)
                .align(Alignment.CenterVertically)
                .focusRequester(focusRequester),
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
