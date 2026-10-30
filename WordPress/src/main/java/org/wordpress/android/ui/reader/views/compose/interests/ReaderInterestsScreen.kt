package org.wordpress.android.ui.reader.views.compose.interests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ContentUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ErrorUiState

/**
 * Lets the user pick the tags they follow, both as the Discover overlay and from Settings
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
@Suppress("LongParameterList")
fun ReaderInterestsScreen(
    uiState: UiState,
    snackbarHostState: SnackbarHostState,
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    onInterestToggled: (index: Int, isChecked: Boolean) -> Unit,
    onDoneClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    val screenDescription = stringResource(R.string.reader_choose_interests_tags_content_description)
    Scaffold(
        modifier = Modifier.semantics { contentDescription = screenDescription },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // hosted in a fragment, so the activity has already handled the insets
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            val doneButton = uiState.doneButtonUiState
            if (doneButton.visible) {
                Surface(tonalElevation = 3.dp, shadowElevation = 3.dp) {
                    Button(
                        onClick = onDoneClick,
                        enabled = doneButton.enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Margin.ExtraLarge.value)
                    ) {
                        Text(stringResource(doneButton.titleRes))
                    }
                }
            }
        }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            when (uiState) {
                is ContentUiState -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Margin.ExtraLarge.value)
                        .padding(top = if (showBackButton) 56.dp else Margin.ExtraLarge.value)
                ) {
                    if (uiState.titleVisible) {
                        Text(
                            text = stringResource(R.string.reader_label_choose_interests),
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = FontFamily.Serif,
                            modifier = Modifier.padding(bottom = Margin.ExtraMediumLarge.value)
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Margin.Medium.value),
                        modifier = Modifier.padding(bottom = Margin.ExtraLarge.value)
                    ) {
                        uiState.interestsUiState.forEachIndexed { index, interest ->
                            FilterChip(
                                selected = interest.isChecked,
                                onClick = { onInterestToggled(index, !interest.isChecked) },
                                label = { Text(interest.title) },
                                // not while the picks are being saved
                                enabled = !uiState.progressBarVisible
                            )
                        }
                    }
                }

                is ErrorUiState -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(Margin.ExtraMediumLarge.value)
                ) {
                    Text(
                        text = stringResource(uiState.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = onRetryClick, modifier = Modifier.padding(top = Margin.ExtraLarge.value)) {
                        Text(stringResource(R.string.retry))
                    }
                }

                else -> Unit
            }
            // while the interests load, and while the picks are saved
            if (uiState.progressBarVisible) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            if (showBackButton) {
                IconButton(onClick = onBackClick, modifier = Modifier.padding(Margin.Small.value)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.reader_label_toolbar_back)
                    )
                }
            }
        }
    }
}
