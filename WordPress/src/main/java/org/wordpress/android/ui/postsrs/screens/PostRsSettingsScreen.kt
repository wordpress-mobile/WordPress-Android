package org.wordpress.android.ui.postsrs.screens

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.SingleChoiceAlertDialog
import org.wordpress.android.ui.postsrs.DialogState
import org.wordpress.android.ui.postsrs.FieldState
import org.wordpress.android.ui.postsrs.PostRsSettingsUiState
import org.wordpress.android.ui.postsrs.RetryableField
import org.wordpress.android.ui.postsrs.toLabel
import uniffi.wp_api.PostStatus

@Composable
fun PostRsSettingsScreen(
    uiState: PostRsSettingsUiState,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit = {},
    onRetryField: (RetryableField) -> Unit = {},
    onStatusClicked: () -> Unit = {},
    onStatusSelected: (PostStatus) -> Unit = {},
    onPasswordClicked: () -> Unit = {},
    onPasswordSet: (String) -> Unit = {},
    onStickyToggled: () -> Unit = {},
    onSaveClicked: () -> Unit = {},
    onDismissDialog: () -> Unit = {},
    onDiscardConfirmed: () -> Unit = {},
) {
    BackHandler {
        onNavigateBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading -> {
                NormalAppBarLayout(
                    onNavigateBack = onNavigateBack,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(
                            Alignment.Center
                        )
                    )
                }
            }
            uiState.error != null -> {
                NormalAppBarLayout(
                    onNavigateBack = onNavigateBack,
                ) {
                    ErrorContent(
                        error = uiState.error,
                        onRetry = onRetry
                    )
                }
            }
            else -> {
                HeroSettingsLayout(
                    uiState = uiState,
                    onNavigateBack = onNavigateBack,
                    onRetryField = onRetryField,
                    onStatusClicked = onStatusClicked,
                    onPasswordClicked = onPasswordClicked,
                    onStickyToggled = onStickyToggled,
                    onSaveClicked = onSaveClicked,
                )
            }
        }
    }

    SettingsDialogs(
        uiState = uiState,
        onStatusSelected = onStatusSelected,
        onPasswordSet = onPasswordSet,
        onDismissDialog = onDismissDialog,
        onDiscardConfirmed = onDiscardConfirmed,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NormalAppBarLayout(
    onNavigateBack: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            R.string.post_settings
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(
                                R.string.back
                            )
                        )
                    }
                }
            )
        }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            content()
        }
    }
}

@Composable
private fun HeroSettingsLayout(
    uiState: PostRsSettingsUiState,
    onNavigateBack: () -> Unit,
    onRetryField: (RetryableField) -> Unit,
    onStatusClicked: () -> Unit,
    onPasswordClicked: () -> Unit,
    onStickyToggled: () -> Unit,
    onSaveClicked: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            when (uiState.featuredImage) {
                is FieldState.Loaded ->
                    HeroImage(
                        imageUrl = uiState.featuredImage.value
                    )
                is FieldState.Loading ->
                    HeroImageShimmer()
                is FieldState.Error ->
                    HeroImagePlaceholder(
                        text = stringResource(
                            R.string
                                .post_rs_settings_featured_image_error
                        )
                    )
                is FieldState.Empty ->
                    HeroImagePlaceholder()
            }
            SettingsContent(
                uiState = uiState,
                onRetryField = onRetryField,
                onStatusClicked = onStatusClicked,
                onPasswordClicked = onPasswordClicked,
                onStickyToggled = onStickyToggled,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )
        FloatingBackButton(
            onNavigateBack = onNavigateBack,
        )
        if (uiState.hasChanges) {
            FloatingSaveButton(
                isSaving = uiState.isSaving,
                onSaveClicked = onSaveClicked,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
private fun HeroImage(imageUrl: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    ) {
        ShimmerBox(modifier = Modifier.matchParentSize())
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(
                R.string.featured_image_desc
            ),
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun HeroImageShimmer() {
    ShimmerBox(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    )
}

@Composable
private fun HeroImagePlaceholder(
    text: String = stringResource(
        R.string.post_rs_settings_featured_image_not_set
    ),
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(
                MaterialTheme.colorScheme.surfaceVariant
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme
                .onSurfaceVariant
        )
    }
}

@Composable
private fun FloatingBackButton(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onNavigateBack,
        modifier = modifier
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(
                R.string.back
            ),
            tint = Color.White,
        )
    }
}

@Composable
private fun FloatingSaveButton(
    isSaving: Boolean,
    onSaveClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isSaving) {
        CircularProgressIndicator(
            modifier = modifier
                .padding(12.dp)
                .size(24.dp),
            strokeWidth = 2.dp,
            color = Color.White,
        )
    } else {
        IconButton(
            onClick = onSaveClicked,
            modifier = modifier
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = stringResource(
                    R.string.save
                ),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.error_generic),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.retry))
        }
    }
}

@Composable
private fun SettingsContent(
    uiState: PostRsSettingsUiState,
    onRetryField: (RetryableField) -> Unit,
    onStatusClicked: () -> Unit,
    onPasswordClicked: () -> Unit,
    onStickyToggled: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            stringResource(R.string.post_settings_publish)
        )

        SettingsRow(
            label = stringResource(
                R.string.post_settings_status
            ),
            value = statusDisplayLabel(uiState),
            modifier = Modifier.clickable(
                onClick = onStatusClicked
            )
        )
        HorizontalDivider()

        SettingsRow(
            label = stringResource(
                R.string.post_settings_time_and_date
            ),
            value = uiState.publishDate
        )
        HorizontalDivider()

        val effectivePassword = uiState.effectivePassword
        SettingsRow(
            label = stringResource(R.string.password),
            value = if (effectivePassword.isNullOrEmpty()) {
                stringResource(R.string.none)
            } else {
                stringResource(
                    R.string.post_rs_settings_protected
                )
            },
            dimmed = effectivePassword.isNullOrEmpty(),
            modifier = Modifier.clickable(
                onClick = onPasswordClicked
            )
        )
        HorizontalDivider()

        AsyncSettingsRow(
            label = stringResource(
                R.string.post_settings_author
            ),
            state = uiState.authorName,
            onRetry = {
                onRetryField(RetryableField.AUTHOR)
            }
        )
        HorizontalDivider()

        SectionHeader(
            stringResource(
                R.string.post_settings_categories_and_tags
            )
        )

        ChipSettingsRow(
            label = stringResource(R.string.categories),
            state = uiState.categoryNames,
            onRetry = {
                onRetryField(RetryableField.CATEGORIES)
            }
        )
        HorizontalDivider()

        ChipSettingsRow(
            label = stringResource(
                R.string.post_settings_tags
            ),
            state = uiState.tagNames,
            onRetry = {
                onRetryField(RetryableField.TAGS)
            }
        )

        SectionHeader(
            stringResource(
                R.string.post_settings_more_options
            )
        )

        StickyRow(
            sticky = uiState.effectiveSticky,
            onToggle = onStickyToggled,
        )
        HorizontalDivider()

        SettingsRow(
            label = stringResource(
                R.string.post_settings_post_format
            ),
            value = uiState.formatLabel
        )
        HorizontalDivider()

        SettingsRow(
            label = stringResource(
                R.string.post_settings_slug
            ),
            value = uiState.slug.ifEmpty {
                stringResource(R.string.none)
            },
            dimmed = uiState.slug.isEmpty()
        )
        HorizontalDivider()

        if (uiState.excerpt.isNotEmpty()) {
            ExpandableSettingsRow(
                label = stringResource(
                    R.string.post_settings_excerpt
                ),
                value = uiState.excerpt
            )
        } else {
            SettingsRow(
                label = stringResource(
                    R.string.post_settings_excerpt
                ),
                value = stringResource(R.string.none),
                dimmed = true
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun statusDisplayLabel(
    uiState: PostRsSettingsUiState,
): String {
    val status = uiState.editedStatus ?: uiState.postStatus
    val resId = status.toLabel()
    return if (resId != 0) stringResource(resId) else ""
}

@Composable
private fun StickyRow(
    sticky: Boolean,
    onToggle: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                stringResource(
                    R.string
                        .post_settings_mark_as_sticky_options_header
                )
            )
        },
        trailingContent = {
            Switch(
                checked = sticky,
                onCheckedChange = { onToggle() }
            )
        },
        modifier = Modifier.clickable(onClick = onToggle)
    )
}

// region Dialogs

@Composable
private fun SettingsDialogs(
    uiState: PostRsSettingsUiState,
    onStatusSelected: (PostStatus) -> Unit,
    onPasswordSet: (String) -> Unit,
    onDismissDialog: () -> Unit,
    onDiscardConfirmed: () -> Unit,
) {
    when (uiState.dialogState) {
        is DialogState.StatusDialog -> StatusDialog(
            currentStatus = uiState.editedStatus
                ?: uiState.postStatus,
            onStatusSelected = onStatusSelected,
            onDismiss = onDismissDialog,
        )
        is DialogState.PasswordDialog -> PasswordDialog(
            currentPassword = uiState.effectivePassword
                ?: "",
            onPasswordSet = onPasswordSet,
            onDismiss = onDismissDialog,
        )
        is DialogState.DiscardDialog -> DiscardDialog(
            onDiscard = onDiscardConfirmed,
            onDismiss = onDismissDialog,
        )
        is DialogState.None -> Unit
    }
}

@Composable
private fun StatusDialog(
    currentStatus: PostStatus?,
    onStatusSelected: (PostStatus) -> Unit,
    onDismiss: () -> Unit,
) {
    val statuses = listOf(
        PostStatus.Publish,
        PostStatus.Draft,
        PostStatus.Pending,
        PostStatus.Private,
    )
    val labels = statuses.map { status ->
        val resId = status.toLabel()
        if (resId != 0) stringResource(resId) else ""
    }
    val currentIndex = statuses.indexOfFirst {
        it == currentStatus
    }.coerceAtLeast(0)

    var selectedIndex by remember {
        mutableIntStateOf(currentIndex)
    }

    SingleChoiceAlertDialog(
        title = stringResource(
            R.string.post_rs_settings_status_dialog_title
        ),
        options = labels,
        selectedIndex = selectedIndex,
        onOptionSelected = { selectedIndex = it },
        onConfirm = {
            onStatusSelected(statuses[selectedIndex])
        },
        onDismiss = onDismiss,
        confirmButtonText = stringResource(R.string.ok),
    )
}

@Composable
private fun PasswordDialog(
    currentPassword: String,
    onPasswordSet: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val prev = view.importantForAutofill
        view.importantForAutofill =
            View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        onDispose {
            view.importantForAutofill = prev
        }
    }

    var text by rememberSaveable {
        mutableStateOf(currentPassword)
    }
    var passwordVisible by rememberSaveable {
        mutableStateOf(false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    R.string
                        .post_rs_settings_password_dialog_title
                )
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = {
                        Text(
                            stringResource(
                                R.string
                                    .post_rs_settings_password_hint
                            )
                        )
                    },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                passwordVisible =
                                    !passwordVisible
                            }
                        ) {
                            Icon(
                                imageVector = if (
                                    passwordVisible
                                ) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (currentPassword.isNotEmpty()) {
                    TextButton(
                        onClick = { onPasswordSet("") },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            stringResource(
                                R.string
                                    .post_rs_settings_remove_password
                            ),
                            color = MaterialTheme
                                .colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onPasswordSet(text) },
                enabled = text.isNotBlank()
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun DiscardDialog(
    onDiscard: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    R.string
                        .post_rs_settings_discard_title
                )
            )
        },
        text = {
            Text(
                stringResource(
                    R.string
                        .post_rs_settings_discard_message
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text(
                    stringResource(R.string.button_discard)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

// endregion

@Composable
private fun AsyncFieldRow(
    label: String,
    state: FieldState,
    onRetry: () -> Unit,
    loadedContent: @Composable (String) -> Unit,
) {
    val loadingDesc = stringResource(
        R.string.post_rs_settings_loading, label
    )
    when (state) {
        is FieldState.Empty ->
            SettingsRow(
                label = label,
                value = stringResource(R.string.none),
                dimmed = true
            )
        is FieldState.Loading -> ListItem(
            headlineContent = { Text(label) },
            supportingContent = {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            },
            modifier = Modifier.semantics {
                contentDescription = loadingDesc
            }
        )
        is FieldState.Loaded ->
            loadedContent(state.value)
        is FieldState.Error ->
            ErrorFieldRow(
                label = label,
                message = state.message,
                onRetry = onRetry
            )
    }
}

@Composable
private fun AsyncSettingsRow(
    label: String,
    state: FieldState,
    onRetry: () -> Unit,
) {
    AsyncFieldRow(label, state, onRetry) { value ->
        SettingsRow(label = label, value = value)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipSettingsRow(
    label: String,
    state: FieldState,
    onRetry: () -> Unit,
) {
    AsyncFieldRow(label, state, onRetry) { value ->
        val items = value
            .split(", ")
            .filter { it.isNotBlank() }
        ListItem(
            headlineContent = { Text(label) },
            supportingContent = {
                FlowRow(
                    horizontalArrangement = Arrangement
                        .spacedBy(8.dp),
                ) {
                    items.forEach { name ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(name) },
                            enabled = false
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun ErrorFieldRow(
    label: String,
    message: String,
    onRetry: () -> Unit,
) {
    val tapToRetry = stringResource(
        R.string.post_rs_settings_tap_to_retry
    )
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = {
            Text(
                "$message — $tapToRetry",
                color = MaterialTheme.colorScheme.error
            )
        },
        modifier = Modifier.clickable(
            onClickLabel = tapToRetry,
            onClick = onRetry
        )
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = 24.dp,
            bottom = 8.dp
        )
    )
}

@Composable
private fun SettingsRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = if (value.isNotEmpty()) {
            {
                Text(
                    text = value,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = if (dimmed) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                    }
                )
            }
        } else {
            null
        },
        modifier = modifier,
    )
}

@Composable
private fun ExpandableSettingsRow(
    label: String,
    value: String,
) {
    var expanded by remember { mutableStateOf(false) }
    var hasOverflow by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = {
            Text(
                text = value,
                maxLines = if (expanded) {
                    Int.MAX_VALUE
                } else {
                    3
                },
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme
                    .onSurfaceVariant,
                onTextLayout = { result ->
                    if (!expanded) {
                        hasOverflow = result.hasVisualOverflow
                    }
                }
            )
        },
        modifier = if (hasOverflow || expanded) {
            Modifier.clickable(
                onClickLabel = stringResource(
                    if (expanded) {
                        R.string.show_less
                    } else {
                        R.string.more
                    }
                )
            ) { expanded = !expanded }
        } else {
            Modifier
        }
    )
}

@Composable
private fun ShimmerBox(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(
        label = "shimmer"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    Box(
        modifier = modifier.background(
            MaterialTheme.colorScheme.onSurface
                .copy(alpha = alpha)
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewSettingsLoaded() {
    MaterialTheme {
        PostRsSettingsScreen(
            uiState = PostRsSettingsUiState(
                isLoading = false,
                postTitle = "My First Post",
                publishDate = "Mar 6, 2026, 10:30 AM",
                password = null,
                authorName = FieldState.Loaded("Jane Doe"),
                categoryNames = FieldState.Loaded(
                    "Travel, Photography"
                ),
                tagNames = FieldState.Loaded(
                    "nature, hiking"
                ),
                featuredImage = FieldState.Empty,
                sticky = true,
                formatLabel = "Standard",
                slug = "my-first-post",
                excerpt = "A short excerpt of the post.",
                postStatus = PostStatus.Publish,
            ),
            onNavigateBack = {},
            onRetry = {},
            onRetryField = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSettingsHeroImage() {
    MaterialTheme {
        PostRsSettingsScreen(
            uiState = PostRsSettingsUiState(
                isLoading = false,
                postTitle = "My First Post",
                publishDate = "Mar 6, 2026, 10:30 AM",
                password = null,
                authorName = FieldState.Loaded("Jane Doe"),
                categoryNames = FieldState.Loaded(
                    "Travel, Photography"
                ),
                tagNames = FieldState.Loaded(
                    "nature, hiking"
                ),
                featuredImage = FieldState.Loaded(
                    "https://example.com/hero.jpg"
                ),
                sticky = true,
                formatLabel = "Standard",
                slug = "my-first-post",
                excerpt = "A short excerpt of the post.",
                postStatus = PostStatus.Publish,
            ),
            onNavigateBack = {},
            onRetry = {},
            onRetryField = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSettingsLoading() {
    MaterialTheme {
        PostRsSettingsScreen(
            uiState = PostRsSettingsUiState(isLoading = true),
            onNavigateBack = {},
            onRetry = {},
            onRetryField = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSettingsError() {
    MaterialTheme {
        PostRsSettingsScreen(
            uiState = PostRsSettingsUiState(
                isLoading = false,
                error = "Unable to load post settings"
            ),
            onNavigateBack = {},
            onRetry = {},
            onRetryField = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSettingsFieldErrors() {
    MaterialTheme {
        PostRsSettingsScreen(
            uiState = PostRsSettingsUiState(
                isLoading = false,
                postTitle = "Test Post",
                publishDate = "Mar 6, 2026, 10:30 AM",
                authorName = FieldState.Error(
                    "Couldn't load"
                ),
                categoryNames = FieldState.Error(
                    "Couldn't load"
                ),
                tagNames = FieldState.Loading,
                featuredImage = FieldState.Loading,
                formatLabel = "Standard",
                slug = "test-post",
                excerpt = "",
            ),
            onNavigateBack = {},
            onRetry = {},
            onRetryField = {},
        )
    }
}
