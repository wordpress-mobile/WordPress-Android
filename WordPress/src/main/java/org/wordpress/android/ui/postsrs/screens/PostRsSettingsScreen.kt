package org.wordpress.android.ui.postsrs.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
                            contentDescription = stringResource(R.string.back)
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
        SectionHeader(stringResource(R.string.post_settings_publish))

        SettingsRow(
            label = stringResource(R.string.post_settings_status),
            value = uiState.statusLabel
        )
        HorizontalDivider()

        SettingsRow(
            label = stringResource(R.string.post_settings_time_and_date),
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

        SettingsRow(
            label = stringResource(R.string.post_settings_author),
            value = uiState.authorDisplayName ?: ""
        )

        SectionHeader(
            stringResource(R.string.post_settings_categories_and_tags)
        )

        SettingsRow(
            label = stringResource(R.string.categories),
            value = when {
                uiState.categoryNames.isNotEmpty() ->
                    uiState.categoryNames.joinToString(", ")
                uiState.categoryIds.isNotEmpty() ->
                    stringResource(R.string.loading)
                else -> ""
            }
        )
        HorizontalDivider()

        SettingsRow(
            label = stringResource(R.string.post_settings_tags),
            value = when {
                uiState.tagNames.isNotEmpty() ->
                    uiState.tagNames.joinToString(", ")
                uiState.tagIds.isNotEmpty() ->
                    stringResource(R.string.loading)
                else -> ""
            }
        )

        SectionHeader(
            stringResource(R.string.post_settings_featured_image)
        )

        if (uiState.featuredImageUrl != null) {
            FeaturedImageRow(imageUrl = uiState.featuredImageUrl)
        } else if (uiState.featuredImageId != 0L) {
            SettingsRow(
                label = stringResource(
                    R.string.post_settings_featured_image
                ),
                value = stringResource(R.string.loading)
            )
        } else {
            SettingsRow(
                label = stringResource(
                    R.string.post_settings_featured_image
                ),
                value = ""
            )
        }

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
            label = stringResource(R.string.post_settings_post_format),
            value = uiState.formatLabel
        )
        HorizontalDivider()

        SettingsRow(
            label = stringResource(R.string.post_settings_slug),
            value = uiState.slug
        )
        HorizontalDivider()

        SettingsRow(
            label = stringResource(R.string.post_settings_excerpt),
            value = uiState.excerpt
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
            start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp
        )
    )
}

@Composable
private fun SettingsRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = if (value.isNotEmpty()) {
            {
                Text(
                    text = value,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
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
    ListItem(
        headlineContent = {
            Text(stringResource(R.string.post_settings_featured_image))
        },
        trailingContent = {
            AsyncImage(
                model = imageUrl,
                contentDescription = stringResource(
                    R.string.featured_image_desc
                ),
                modifier = Modifier
                    .size(FEATURED_IMAGE_SIZE)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
        }
    )
}

private val FEATURED_IMAGE_SIZE = 64.dp
