package org.wordpress.android.ui.postsrs.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.wordpress.android.R
import org.wordpress.android.ui.postsrs.FieldState
import org.wordpress.android.ui.postsrs.PostRsSettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostRsSettingsScreen(
    uiState: PostRsSettingsUiState,
    onNavigateBack: () -> Unit,
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
                    Text(
                        text = uiState.error,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                else -> {
                    SettingsContent(uiState = uiState)
                }
            }
        }
    }
}

@Composable
private fun SettingsContent(uiState: PostRsSettingsUiState) {
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

        SettingsRow(
            label = stringResource(R.string.password),
            value = if (uiState.password.isNullOrEmpty()) {
                ""
            } else {
                stringResource(R.string.post_rs_settings_protected)
            }
        )
        HorizontalDivider()

        AsyncSettingsRow(
            label = stringResource(R.string.post_settings_author),
            state = uiState.authorName
        )

        SectionHeader(
            stringResource(
                R.string.post_settings_categories_and_tags
            )
        )

        AsyncSettingsRow(
            label = stringResource(R.string.categories),
            state = uiState.categoryNames
        )
        HorizontalDivider()

        AsyncSettingsRow(
            label = stringResource(R.string.post_settings_tags),
            state = uiState.tagNames
        )

        SectionHeader(
            stringResource(R.string.post_settings_featured_image)
        )

        FeaturedImageField(state = uiState.featuredImage)

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

        SettingsRow(
            label = stringResource(R.string.post_settings_slug),
            value = uiState.slug
        )
        HorizontalDivider()

        if (uiState.excerpt.isNotEmpty()) {
            SettingsRow(
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
    }
}

@Composable
private fun AsyncSettingsRow(label: String, state: FieldState) {
    when (state) {
        is FieldState.Empty ->
            SettingsRow(label = label, value = "")
        is FieldState.Loading -> ListItem(
            headlineContent = { Text(label) },
            supportingContent = {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            }
        )
        is FieldState.Loaded ->
            SettingsRow(label = label, value = state.value)
        is FieldState.Error -> ListItem(
            headlineContent = { Text(label) },
            supportingContent = {
                Text(
                    state.message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        )
    }
}

@Composable
private fun FeaturedImageField(state: FieldState) {
    val label = stringResource(
        R.string.post_settings_featured_image
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
            }
        )
        is FieldState.Loaded ->
            FeaturedImageRow(imageUrl = state.value)
        is FieldState.Error -> ListItem(
            headlineContent = { Text(label) },
            supportingContent = {
                Text(
                    state.message,
                    color = MaterialTheme.colorScheme.error
                )
            }
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
private fun FeaturedImageRow(imageUrl: String) {
    AsyncImage(
        model = imageUrl,
        contentDescription = stringResource(
            R.string.featured_image_desc
        ),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Crop
    )
}
