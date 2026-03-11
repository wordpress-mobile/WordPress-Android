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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.semantics.Role
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
import kotlinx.coroutines.flow.distinctUntilChanged
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.SingleChoiceAlertDialog
import org.wordpress.android.ui.postsrs.AuthorInfo
import org.wordpress.android.ui.postsrs.DialogState
import org.wordpress.android.ui.postsrs.FieldState
import org.wordpress.android.ui.postsrs.PostRsSettingsUiState
import org.wordpress.android.ui.postsrs.RetryableField
import org.wordpress.android.ui.postsrs.toLabel
import uniffi.wp_api.PostFormat
import uniffi.wp_api.PostStatus
import java.util.Calendar
import java.util.Date
import org.wordpress.android.ui.postsrs.UTC

@Composable
@Suppress("LongParameterList")
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
    onSlugClicked: () -> Unit = {},
    onSlugSet: (String) -> Unit = {},
    onExcerptClicked: () -> Unit = {},
    onExcerptSet: (String) -> Unit = {},
    onFormatClicked: () -> Unit = {},
    onFormatSelected: (PostFormat) -> Unit = {},
    onDateClicked: () -> Unit = {},
    onDateSelected: (Int, Int, Int) -> Unit = { _, _, _ -> },
    onTimeSelected: (Int, Int) -> Unit = { _, _ -> },
    onAuthorClicked: () -> Unit = {},
    onAuthorSelected: (Long) -> Unit = {},
    onFeaturedImageClicked: () -> Unit = {},
    onFeaturedImageRemoved: () -> Unit = {},
    onLoadMoreAuthors: () -> Unit = {},
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
                    onSlugClicked = onSlugClicked,
                    onExcerptClicked = onExcerptClicked,
                    onFormatClicked = onFormatClicked,
                    onDateClicked = onDateClicked,
                    onAuthorClicked = onAuthorClicked,
                    onFeaturedImageClicked =
                        onFeaturedImageClicked,
                    onFeaturedImageRemoved =
                        onFeaturedImageRemoved,
                    onSaveClicked = onSaveClicked,
                )
            }
        }
    }

    SettingsDialogs(
        uiState = uiState,
        onStatusSelected = onStatusSelected,
        onPasswordSet = onPasswordSet,
        onSlugSet = onSlugSet,
        onExcerptSet = onExcerptSet,
        onFormatSelected = onFormatSelected,
        onDateSelected = onDateSelected,
        onTimeSelected = onTimeSelected,
        onAuthorSelected = onAuthorSelected,
        onLoadMoreAuthors = onLoadMoreAuthors,
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

@Suppress("LongParameterList")
@Composable
private fun HeroSettingsLayout(
    uiState: PostRsSettingsUiState,
    onNavigateBack: () -> Unit,
    onRetryField: (RetryableField) -> Unit,
    onStatusClicked: () -> Unit,
    onPasswordClicked: () -> Unit,
    onStickyToggled: () -> Unit,
    onSlugClicked: () -> Unit,
    onExcerptClicked: () -> Unit,
    onFormatClicked: () -> Unit,
    onDateClicked: () -> Unit,
    onAuthorClicked: () -> Unit,
    onFeaturedImageClicked: () -> Unit,
    onFeaturedImageRemoved: () -> Unit,
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
                    HeroImageWithMenu(
                        imageUrl =
                            uiState.featuredImage.value,
                        onChangeClicked =
                            onFeaturedImageClicked,
                        onRemoveClicked =
                            onFeaturedImageRemoved,
                    )
                is FieldState.Loading ->
                    HeroImageShimmer()
                is FieldState.Error ->
                    HeroImagePlaceholder(
                        text = stringResource(
                            R.string
                                .post_rs_settings_featured_image_error
                        ),
                        onClick = onFeaturedImageClicked,
                    )
                is FieldState.Empty ->
                    HeroImagePlaceholder(
                        onClick = onFeaturedImageClicked,
                    )
            }
            SettingsContent(
                uiState = uiState,
                onRetryField = onRetryField,
                onStatusClicked = onStatusClicked,
                onPasswordClicked = onPasswordClicked,
                onStickyToggled = onStickyToggled,
                onSlugClicked = onSlugClicked,
                onExcerptClicked = onExcerptClicked,
                onFormatClicked = onFormatClicked,
                onDateClicked = onDateClicked,
                onAuthorClicked = onAuthorClicked,
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
            modifier = Modifier.padding(4.dp)
        )
        if (uiState.hasChanges) {
            FloatingSaveButton(
                isSaving = uiState.isSaving,
                onSaveClicked = onSaveClicked,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            )
        }
    }
}

@Composable
private fun HeroImageWithMenu(
    imageUrl: String,
    onChangeClicked: () -> Unit,
    onRemoveClicked: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    ) {
        ShimmerBox(modifier = Modifier.matchParentSize())
        AsyncImage(
            model = ImageRequest.Builder(
                LocalContext.current
            )
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(
                R.string.featured_image_desc
            ),
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        FeaturedImageEditButton(
            onChangeClicked = onChangeClicked,
            onRemoveClicked = onRemoveClicked,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
        )
    }
}

@Composable
private fun EditIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .background(
                Color.Black.copy(alpha = 0.5f),
                MaterialTheme.shapes.small
            )
            .size(36.dp)
    ) {
        Icon(
            Icons.Default.Edit,
            contentDescription = stringResource(
                R.string
                    .post_rs_settings_edit_featured_image
            ),
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun FeaturedImageEditButton(
    onChangeClicked: () -> Unit,
    onRemoveClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        EditIconButton(onClick = { expanded = true })
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            R.string
                                .post_rs_settings_change_featured_image
                        )
                    )
                },
                onClick = {
                    expanded = false
                    onChangeClicked()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            R.string
                                .post_rs_settings_remove_featured_image
                        ),
                        color = MaterialTheme
                            .colorScheme.error
                    )
                },
                onClick = {
                    expanded = false
                    onRemoveClicked()
                }
            )
        }
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
    onClick: (() -> Unit)? = null,
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(
                MaterialTheme.colorScheme.surfaceVariant
            )
            .then(clickModifier),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme
                .onSurfaceVariant,
            modifier = Modifier.align(Alignment.Center)
        )
        if (onClick != null) {
            EditIconButton(
                onClick = onClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            )
        }
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
    onSlugClicked: () -> Unit,
    onExcerptClicked: () -> Unit,
    onFormatClicked: () -> Unit,
    onDateClicked: () -> Unit,
    onAuthorClicked: () -> Unit,
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
            value = uiState.publishDate,
            modifier = Modifier.clickable(
                onClick = onDateClicked
            )
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

        AsyncFieldRow(
            label = stringResource(
                R.string.post_settings_author
            ),
            state = uiState.authorName,
            onRetry = {
                onRetryField(RetryableField.AUTHOR)
            }
        ) { value ->
            SettingsRow(
                label = stringResource(
                    R.string.post_settings_author
                ),
                value = value,
                modifier = Modifier.clickable(
                    onClick = onAuthorClicked
                )
            )
        }
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
            value = formatDisplayLabel(uiState),
            modifier = Modifier.clickable(
                onClick = onFormatClicked
            )
        )
        HorizontalDivider()

        val effectiveSlug = uiState.effectiveSlug
        SettingsRow(
            label = stringResource(
                R.string.post_settings_slug
            ),
            value = effectiveSlug.ifEmpty {
                stringResource(R.string.none)
            },
            dimmed = effectiveSlug.isEmpty(),
            modifier = Modifier.clickable(
                onClick = onSlugClicked
            )
        )
        HorizontalDivider()

        val effectiveExcerpt = uiState.effectiveExcerpt
        SettingsRow(
            label = stringResource(
                R.string.post_settings_excerpt
            ),
            value = effectiveExcerpt.ifEmpty {
                stringResource(R.string.none)
            },
            dimmed = effectiveExcerpt.isEmpty(),
            modifier = Modifier.clickable(
                onClick = onExcerptClicked
            )
        )

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
private fun formatDisplayLabel(
    uiState: PostRsSettingsUiState,
): String {
    val format = uiState.editedFormat ?: uiState.postFormat
    return postFormatLabel(format)
}

@Composable
private fun postFormatLabel(format: PostFormat?): String =
    when (format) {
        is PostFormat.Standard ->
            stringResource(R.string.post_format_standard)
        is PostFormat.Aside ->
            stringResource(R.string.post_format_aside)
        is PostFormat.Chat ->
            stringResource(R.string.post_format_chat)
        is PostFormat.Gallery ->
            stringResource(R.string.post_format_gallery)
        is PostFormat.Link ->
            stringResource(R.string.post_format_link)
        is PostFormat.Image ->
            stringResource(R.string.post_format_image)
        is PostFormat.Quote ->
            stringResource(R.string.post_format_quote)
        is PostFormat.Status ->
            stringResource(R.string.post_format_status)
        is PostFormat.Video ->
            stringResource(R.string.post_format_video)
        is PostFormat.Audio ->
            stringResource(R.string.post_format_audio)
        is PostFormat.Custom -> format.v1
        null -> ""
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
    onSlugSet: (String) -> Unit,
    onExcerptSet: (String) -> Unit,
    onFormatSelected: (PostFormat) -> Unit,
    onDateSelected: (Int, Int, Int) -> Unit,
    onTimeSelected: (Int, Int) -> Unit,
    onAuthorSelected: (Long) -> Unit,
    onLoadMoreAuthors: () -> Unit,
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
        is DialogState.SlugDialog -> SlugDialog(
            currentSlug = uiState.effectiveSlug,
            onSlugSet = onSlugSet,
            onDismiss = onDismissDialog,
        )
        is DialogState.ExcerptDialog -> ExcerptDialog(
            currentExcerpt = uiState.effectiveExcerpt,
            onExcerptSet = onExcerptSet,
            onDismiss = onDismissDialog,
        )
        is DialogState.FormatDialog -> FormatDialog(
            currentFormat = uiState.editedFormat
                ?: uiState.postFormat,
            onFormatSelected = onFormatSelected,
            onDismiss = onDismissDialog,
        )
        is DialogState.DateDialog -> DateDialog(
            currentDate = uiState.effectiveDate,
            onDateSelected = onDateSelected,
            onDismiss = onDismissDialog,
        )
        is DialogState.TimeDialog -> TimeDialog(
            currentDate = uiState.effectiveDate,
            onTimeSelected = onTimeSelected,
            onDismiss = onDismissDialog,
        )
        is DialogState.AuthorDialog -> AuthorDialog(
            authors = uiState.siteAuthors,
            currentAuthorId = uiState.effectiveAuthorId,
            isLoadingMore = uiState.isLoadingMoreAuthors,
            canLoadMore = uiState.canLoadMoreAuthors,
            onAuthorSelected = onAuthorSelected,
            onLoadMore = onLoadMoreAuthors,
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

    val selectedIndex = remember {
        mutableIntStateOf(currentIndex)
    }

    SingleChoiceAlertDialog(
        title = stringResource(
            R.string.post_rs_settings_status_dialog_title
        ),
        options = labels,
        selectedIndex = selectedIndex.intValue,
        onOptionSelected = { selectedIndex.intValue = it },
        onConfirm = {
            onStatusSelected(statuses[selectedIndex.intValue])
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
private fun TextFieldDialog(
    title: String,
    hint: String,
    currentValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    singleLine: Boolean = false,
    minLines: Int = 1,
    clearButtonText: String? = null,
    onClear: (() -> Unit)? = null,
) {
    var text by rememberSaveable {
        mutableStateOf(currentValue)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme
                        .colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = singleLine,
                    minLines = minLines,
                    modifier = Modifier.fillMaxWidth()
                )
                if (
                    onClear != null &&
                    clearButtonText != null &&
                    currentValue.isNotEmpty()
                ) {
                    TextButton(
                        onClick = onClear,
                        modifier = Modifier
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            clearButtonText,
                            color = MaterialTheme
                                .colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
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
private fun SlugDialog(
    currentSlug: String,
    onSlugSet: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    TextFieldDialog(
        title = stringResource(
            R.string.post_rs_settings_slug_dialog_title
        ),
        hint = stringResource(
            R.string.post_settings_slug_dialog_hint
        ),
        currentValue = currentSlug,
        onConfirm = onSlugSet,
        onDismiss = onDismiss,
        singleLine = true,
    )
}

@Composable
private fun ExcerptDialog(
    currentExcerpt: String,
    onExcerptSet: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    TextFieldDialog(
        title = stringResource(
            R.string.post_rs_settings_excerpt_dialog_title
        ),
        hint = stringResource(
            R.string.post_settings_excerpt_dialog_hint
        ),
        currentValue = currentExcerpt,
        onConfirm = onExcerptSet,
        onDismiss = onDismiss,
        minLines = 5,
        clearButtonText = stringResource(
            R.string.post_rs_settings_clear_excerpt
        ),
        onClear = { onExcerptSet("") },
    )
}

@Suppress("ForbiddenComment")
@Composable
private fun FormatDialog(
    currentFormat: PostFormat?,
    onFormatSelected: (PostFormat) -> Unit,
    onDismiss: () -> Unit,
) {
    // TODO: Replace hardcoded formats with site-supported
    //  formats once wordpress-rs exposes that API
    val formats = listOf(
        PostFormat.Standard,
        PostFormat.Aside,
        PostFormat.Audio,
        PostFormat.Chat,
        PostFormat.Gallery,
        PostFormat.Image,
        PostFormat.Link,
        PostFormat.Quote,
        PostFormat.Status,
        PostFormat.Video,
    )
    val labels = formats.map { postFormatLabel(it) }
    val currentIndex = formats.indexOfFirst {
        it == currentFormat
    }.coerceAtLeast(0)

    val selectedIndex = remember {
        mutableIntStateOf(currentIndex)
    }

    SingleChoiceAlertDialog(
        title = stringResource(
            R.string.post_rs_settings_format_dialog_title
        ),
        options = labels,
        selectedIndex = selectedIndex.intValue,
        onOptionSelected = { selectedIndex.intValue = it },
        onConfirm = {
            onFormatSelected(formats[selectedIndex.intValue])
        },
        onDismiss = onDismiss,
        confirmButtonText = stringResource(R.string.ok),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(
    currentDate: Date?,
    onDateSelected: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    // Normalize to UTC midnight for DatePickerState
    val initialMillis = currentDate?.let { date ->
        val cal = Calendar.getInstance(UTC)
        cal.time = date
        cal[Calendar.HOUR_OF_DAY] = 0
        cal[Calendar.MINUTE] = 0
        cal[Calendar.SECOND] = 0
        cal[Calendar.MILLISECOND] = 0
        cal.timeInMillis
    } ?: System.currentTimeMillis()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState
                        .selectedDateMillis ?: return@TextButton
                    val cal = Calendar.getInstance(UTC)
                    cal.timeInMillis = millis
                    onDateSelected(
                        cal[Calendar.YEAR],
                        cal[Calendar.MONTH],
                        cal[Calendar.DAY_OF_MONTH]
                    )
                }
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(
    currentDate: Date?,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val cal = remember(currentDate) {
        Calendar.getInstance(UTC).apply {
            time = currentDate ?: Date()
        }
    }
    val timePickerState = rememberTimePickerState(
        initialHour = cal[Calendar.HOUR_OF_DAY],
        initialMinute = cal[Calendar.MINUTE],
    )

    TimePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(
                        timePickerState.hour,
                        timePickerState.minute
                    )
                }
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = {
            Text(
                stringResource(
                    R.string.post_rs_settings_select_time
                )
            )
        }
    ) {
        TimePicker(state = timePickerState)
    }
}

@Composable
private fun AuthorDialog(
    authors: List<AuthorInfo>,
    currentAuthorId: Long,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    onAuthorSelected: (Long) -> Unit,
    onLoadMore: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (authors.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    stringResource(
                        R.string
                            .post_rs_settings_author_dialog_title
                    )
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
        return
    }

    val currentIndex = authors.indexOfFirst {
        it.id == currentAuthorId
    }.coerceAtLeast(0)

    val selectedIndex = remember {
        mutableIntStateOf(currentIndex)
    }

    val listState = rememberLazyListState()

    LaunchedEffect(canLoadMore) {
        if (!canLoadMore) return@LaunchedEffect
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible =
                info.visibleItemsInfo.lastOrNull()?.index
                    ?: 0
            lastVisible >= info.totalItemsCount -
                AUTHOR_LOAD_THRESHOLD
        }.distinctUntilChanged().collect { shouldLoad ->
            if (shouldLoad) onLoadMore()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    R.string
                        .post_rs_settings_author_dialog_title
                )
            )
        },
        text = {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .selectableGroup()
                    .heightIn(max = 400.dp)
            ) {
                itemsIndexed(
                    items = authors,
                    key = { _, author -> author.id }
                ) { index, author ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = index ==
                                    selectedIndex.intValue,
                                onClick = {
                                    selectedIndex.intValue =
                                        index
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = index ==
                                selectedIndex.intValue,
                            onClick = null
                        )
                        Text(
                            text = author.name,
                            style = MaterialTheme
                                .typography.bodyLarge,
                            modifier = Modifier
                                .padding(start = 8.dp)
                        )
                    }
                }
                if (isLoadingMore) {
                    item(key = "loading_more_authors") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment =
                                Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAuthorSelected(
                        authors[selectedIndex.intValue].id
                    )
                }
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

private const val AUTHOR_LOAD_THRESHOLD = 3

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
                slug = "test-post",
                excerpt = "",
            ),
            onNavigateBack = {},
            onRetry = {},
            onRetryField = {},
        )
    }
}
