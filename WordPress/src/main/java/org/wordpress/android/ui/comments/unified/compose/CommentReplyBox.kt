package org.wordpress.android.ui.comments.unified.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.wordpress.android.R
import org.wordpress.android.ui.dataview.compose.RemoteImage
import org.wordpress.android.ui.suggestion.Suggestion

/**
 * The reply box pinned to the bottom of the comment detail: expand-to-full-screen affordance,
 * multi-line reply field with `@`-mention suggestions, and a send button that swaps for a
 * progress indicator while the reply is in flight. Mirrors the legacy reader_include_comment_box.
 */
@Composable
@Suppress("LongParameterList")
fun CommentReplyBox(
    replyText: TextFieldValue,
    onReplyTextChange: (TextFieldValue) -> Unit,
    suggestions: List<Suggestion>,
    hint: String,
    isReplyInProgress: Boolean,
    focusOnLaunch: Boolean,
    onSendClick: () -> Unit,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var isReplyFieldFocused by remember { mutableStateOf(false) }
    val canSend = replyText.text.isNotBlank() && !isReplyInProgress

    Column(modifier = modifier.fillMaxWidth()) {
        // Only suggest mentions while the field is focused, so a restored draft that happens to
        // end in an @-token doesn't pop the panel the moment the screen opens.
        if (isReplyFieldFocused) {
            MentionSuggestionPanel(replyText, suggestions, onReplyTextChange)
        }
        HorizontalDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExpandClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_up_white_24dp),
                    contentDescription = stringResource(R.string.description_expand),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = MEDIUM_EMPHASIS_ALPHA)
                )
            }
            ReplyTextField(
                replyText = replyText,
                onReplyTextChange = onReplyTextChange,
                hint = hint,
                enabled = !isReplyInProgress,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { isReplyFieldFocused = it.isFocused }
            )
            if (isReplyInProgress) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .size(24.dp)
                )
            } else {
                IconButton(onClick = onSendClick, enabled = canSend) {
                    Icon(
                        painter = painterResource(R.drawable.ic_send_white_24dp),
                        contentDescription = stringResource(R.string.send),
                        tint = if (canSend) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = MEDIUM_EMPHASIS_ALPHA)
                        }
                    )
                }
            }
        }
    }

    if (focusOnLaunch) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

/**
 * A full-screen version of the reply field, opened from the reply box's expand affordance.
 * Shares [replyText] with the inline field so text and cursor survive expand/collapse, replacing
 * the legacy CollapseFullScreenDialogFragment + CommentFullScreenDialogFragment pair.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
fun FullScreenReplyDialog(
    replyText: TextFieldValue,
    onReplyTextChange: (TextFieldValue) -> Unit,
    suggestions: List<Suggestion>,
    hint: String,
    isReplyInProgress: Boolean,
    onSendClick: () -> Unit,
    onCollapseClick: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var isReplyFieldFocused by remember { mutableStateOf(false) }
    val canSend = replyText.text.isNotBlank() && !isReplyInProgress
    Dialog(
        onDismissRequest = onCollapseClick,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(stringResource(R.string.comment)) },
                    navigationIcon = {
                        IconButton(onClick = onCollapseClick) {
                            Icon(
                                painter = painterResource(R.drawable.ic_chevron_down_white_24dp),
                                contentDescription = stringResource(R.string.description_collapse)
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = onSendClick, enabled = canSend) {
                            Text(stringResource(R.string.send))
                        }
                    }
                )
                ReplyTextField(
                    replyText = replyText,
                    onReplyTextChange = onReplyTextChange,
                    hint = hint,
                    enabled = !isReplyInProgress,
                    singleLineHeight = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { isReplyFieldFocused = it.isFocused }
                )
                if (isReplyFieldFocused) {
                    MentionSuggestionPanel(replyText, suggestions, onReplyTextChange)
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun ReplyTextField(
    replyText: TextFieldValue,
    onReplyTextChange: (TextFieldValue) -> Unit,
    hint: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    singleLineHeight: Boolean = true
) {
    TextField(
        value = replyText,
        onValueChange = onReplyTextChange,
        enabled = enabled,
        placeholder = { Text(hint) },
        textStyle = MaterialTheme.typography.bodyLarge,
        minLines = if (singleLineHeight) 1 else 2,
        maxLines = if (singleLineHeight) 4 else Int.MAX_VALUE,
        // No IME send action: the legacy field was textMultiLine, so the enter key inserts a
        // newline and sending stays on the dedicated send button.
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        modifier = modifier
    )
}

/**
 * User suggestions for the `@`-mention being typed at the cursor, shown directly above the reply
 * field. Replaces the popup the legacy SuggestionAutoCompleteText offered; matching and filtering
 * follow the legacy SuggestionAdapter/SuggestionTokenizer behaviour.
 */
@Composable
private fun MentionSuggestionPanel(
    replyText: TextFieldValue,
    suggestions: List<Suggestion>,
    onReplyTextChange: (TextFieldValue) -> Unit
) {
    val token = findMentionToken(replyText) ?: return
    val filtered = filterMentionSuggestions(suggestions, token.query)
    if (filtered.isEmpty()) return

    HorizontalDivider()
    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
        items(filtered, key = { it.value }) { suggestion ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onReplyTextChange(applyMentionSuggestion(replyText, token, suggestion))
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                RemoteImage(
                    imageUrl = suggestion.avatarUrl,
                    fallbackImageRes = R.drawable.ic_user_placeholder_primary_24,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = "@${suggestion.value}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = suggestion.displayValue,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = MEDIUM_EMPHASIS_ALPHA)
                    )
                }
            }
        }
    }
}

internal data class MentionToken(val start: Int, val query: String)

/**
 * Finds the `@`-mention token the cursor is currently inside: an `@` at the start of the text or
 * preceded by whitespace, with no whitespace between it and the cursor. Returns null when the
 * cursor isn't in a mention (or a selection is active).
 */
internal fun findMentionToken(value: TextFieldValue): MentionToken? {
    val cursor = value.selection.end
    val text = value.text
    if (!value.selection.collapsed || cursor > text.length) return null
    // The candidate token is whatever sits between the last whitespace and the cursor
    val beforeCursor = text.substring(0, cursor)
    val tokenStart = beforeCursor.indexOfLast { it.isWhitespace() } + 1
    val candidate = beforeCursor.substring(tokenStart)
    return if (candidate.startsWith('@')) {
        MentionToken(start = tokenStart, query = candidate.substring(1))
    } else {
        null
    }
}

/** Same matching as the legacy SuggestionAdapter: user login or display name prefix, per word. */
internal fun filterMentionSuggestions(suggestions: List<Suggestion>, query: String): List<Suggestion> {
    if (query.isEmpty()) return suggestions
    val lowerQuery = query.lowercase()
    return suggestions.filter { suggestion ->
        suggestion.value.lowercase().startsWith(lowerQuery) ||
            suggestion.displayValue.lowercase().startsWith(lowerQuery) ||
            suggestion.displayValue.lowercase().contains(" $lowerQuery")
    }
}

/** Replaces the mention being typed with the picked suggestion and moves the cursor after it. */
internal fun applyMentionSuggestion(
    value: TextFieldValue,
    token: MentionToken,
    suggestion: Suggestion
): TextFieldValue {
    val replacement = "@${suggestion.value} "
    val newText = value.text.replaceRange(token.start, value.selection.end, replacement)
    return value.copy(text = newText, selection = TextRange(token.start + replacement.length))
}
