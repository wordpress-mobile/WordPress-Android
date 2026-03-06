package org.wordpress.android.ui.postsrs.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R
import org.wordpress.android.ui.postsrs.FieldState
import org.wordpress.android.ui.postsrs.PostRsSettingsUiState
import org.wordpress.android.ui.postsrs.RetryableField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostRsSettingsScreen(
    uiState: PostRsSettingsUiState,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit = {},
    onRetryField: (RetryableField) -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.post_settings),
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
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    ErrorContent(
                        error = uiState.error,
                        onRetry = onRetry
                    )
                }
                else -> {
                    SettingsContent(
                        uiState = uiState,
                        onRetryField = onRetryField
                    )
                }
            }
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
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SectionHeader(
            stringResource(R.string.post_settings_publish)
        )

        SettingsRow(
            label = stringResource(R.string.post_settings_status),
            value = uiState.statusLabel
        )
        HorizontalDivider()

        SettingsRow(
            label = stringResource(
                R.string.post_settings_time_and_date
            ),
            value = uiState.publishDate
        )
        HorizontalDivider()

        if (uiState.password.isNullOrEmpty()) {
            SettingsRow(
                label = stringResource(R.string.password),
                value = stringResource(R.string.none),
                dimmed = true
            )
        } else {
            SettingsRow(
                label = stringResource(R.string.password),
                value = stringResource(
                    R.string.post_rs_settings_protected
                )
            )
        }
        HorizontalDivider()

        AsyncSettingsRow(
            label = stringResource(R.string.post_settings_author),
            state = uiState.authorName,
            onRetry = {
                onRetryField(RetryableField.AUTHOR)
            }
        )

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
            label = stringResource(R.string.post_settings_tags),
            state = uiState.tagNames,
            onRetry = {
                onRetryField(RetryableField.TAGS)
            }
        )

        SectionHeader(
            stringResource(R.string.post_settings_featured_image)
        )

        FeaturedImageField(
            state = uiState.featuredImage,
            onRetry = {
                onRetryField(RetryableField.FEATURED_IMAGE)
            }
        )

        SectionHeader(
            stringResource(R.string.post_settings_more_options)
        )

        SettingsRow(
            label = stringResource(
                R.string.post_settings_mark_as_sticky_options_header
            ),
            value = if (uiState.sticky) {
                stringResource(R.string.yes)
            } else {
                stringResource(R.string.no)
            }
        )
        HorizontalDivider()

        SettingsRow(
            label = stringResource(
                R.string.post_settings_post_format
            ),
            value = uiState.formatLabel
        )
        HorizontalDivider()

        if (uiState.slug.isNotEmpty()) {
            SettingsRow(
                label = stringResource(R.string.post_settings_slug),
                value = uiState.slug
            )
        } else {
            SettingsRow(
                label = stringResource(R.string.post_settings_slug),
                value = stringResource(R.string.none),
                dimmed = true
            )
        }
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
private fun AsyncSettingsRow(
    label: String,
    state: FieldState,
    onRetry: () -> Unit,
) {
    val noneLabel = stringResource(R.string.none)
    val loadingDesc = stringResource(
        R.string.post_rs_settings_loading, label
    )
    when (state) {
        is FieldState.Empty ->
            SettingsRow(
                label = label,
                value = noneLabel,
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
            SettingsRow(label = label, value = state.value)
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
    val noneLabel = stringResource(R.string.none)
    val loadingDesc = stringResource(
        R.string.post_rs_settings_loading, label
    )
    when (state) {
        is FieldState.Empty ->
            SettingsRow(
                label = label,
                value = noneLabel,
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
        is FieldState.Loaded -> {
            val items = state.value
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
        is FieldState.Error ->
            ErrorFieldRow(
                label = label,
                message = state.message,
                onRetry = onRetry
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
        modifier = Modifier.clickable(onClick = onRetry)
    )
}

@Composable
private fun FeaturedImageField(
    state: FieldState,
    onRetry: () -> Unit,
) {
    val label = stringResource(
        R.string.post_settings_featured_image
    )
    val loadingDesc = stringResource(
        R.string.post_rs_settings_loading, label
    )
    when (state) {
        is FieldState.Empty ->
            SettingsRow(
                label = stringResource(
                    R.string.post_settings_featured_image
                ),
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
            FeaturedImageRow(imageUrl = state.value)
        is FieldState.Error ->
            ErrorFieldRow(
                label = label,
                message = state.message,
                onRetry = onRetry
            )
    }
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
    dimmed: Boolean = false,
    onClick: (() -> Unit)? = null,
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
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        } else {
            null
        },
        modifier = if (onClick != null) {
            Modifier.clickable(onClick = onClick)
        } else {
            Modifier
        }
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
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                onTextLayout = { result ->
                    if (!expanded) {
                        hasOverflow = result.hasVisualOverflow
                    }
                }
            )
        },
        modifier = if (hasOverflow || expanded) {
            Modifier.clickable { expanded = !expanded }
        } else {
            Modifier
        }
    )
}

@Composable
private fun FeaturedImageRow(imageUrl: String) {
    val imageModifier = Modifier
        .padding(horizontal = 16.dp)
        .fillMaxWidth()
        .aspectRatio(16f / 9f)
        .clip(RoundedCornerShape(8.dp))
    Box(modifier = imageModifier) {
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
            MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
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
                statusLabel = "Published",
                publishDate = "Mar 6, 2026, 10:30 AM",
                password = null,
                authorName = FieldState.Loaded("Jane Doe"),
                categoryNames = FieldState.Loaded(
                    "Travel, Photography"
                ),
                tagNames = FieldState.Loaded("nature, hiking"),
                featuredImage = FieldState.Empty,
                sticky = true,
                formatLabel = "Standard",
                slug = "my-first-post",
                excerpt = "A short excerpt of the post.",
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
                statusLabel = "Draft",
                publishDate = "Mar 6, 2026, 10:30 AM",
                authorName = FieldState.Error("Couldn't load"),
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
