package org.wordpress.android.ui.posts.sharemessage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppThemeM2Editor
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.posts.sharemessage.EditJetpackSocialShareMessageViewModel.ActionEvent
import org.wordpress.android.ui.posts.sharemessage.EditJetpackSocialShareMessageViewModel.UiState
import org.wordpress.android.util.extensions.setContent

@AndroidEntryPoint
class EditJetpackSocialShareMessageActivity : AppCompatActivity() {
    private val viewModel: EditJetpackSocialShareMessageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.start(
            initialShareMessage = requireNotNull(intent.getStringExtra(EXTRA_SOCIAL_SHARE_MESSAGE))
        )
        observeActionEvents()
        setContent {
            AppThemeM2Editor {
                val uiState by viewModel.uiState.collectAsState()
                when (val state = uiState) {
                    is UiState.Loaded -> {
                        Loaded(state)
                    }

                    is UiState.Initial -> {
                        // no-op
                    }
                }
            }
        }
    }

    @Composable
    private fun Loaded(state: UiState.Loaded) {
        Scaffold(
            topBar = {
                MainTopAppBar(
                    title = state.appBarLabel,
                    navigationIcon = NavigationIcons.BackIcon,
                    onNavigationIconClick = state.onBackClick,
                )
            }
        ) { contentPadding ->
            Box(
                modifier = Modifier.padding(contentPadding)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(Margin.ExtraLarge.value)
                ) {
                    var shareTextFieldValue by remember {
                        val cursorIndex = state.currentShareMessage.length
                        mutableStateOf(
                            TextFieldValue(
                                state.currentShareMessage,
                                selection = TextRange(cursorIndex),
                            )
                        )
                    }
                    val focusRequester = remember { FocusRequester() }
                    OutlinedTextField(
                        modifier = Modifier
                            .padding(vertical = Margin.ExtraLarge.value)
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        value = shareTextFieldValue,
                        onValueChange = {
                            val maxLength = state.shareMessageMaxLength
                            val newValue = if (it.text.length > maxLength) {
                                TextFieldValue(
                                    text = it.text.take(state.shareMessageMaxLength),
                                    selection = TextRange(
                                        start = it.selection.start.coerceIn(0, maxLength),
                                        end = it.selection.end.coerceIn(0, maxLength),
                                    ),
                                    composition = it.composition?.let { comp ->
                                        TextRange(
                                            start = comp.start.coerceIn(0, maxLength),
                                            end = comp.start.coerceIn(0, maxLength)
                                        )
                                    }
                                )
                            } else {
                                it
                            }

                            shareTextFieldValue = newValue
                            viewModel.updateShareMessage(newValue.text)
                        },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = MaterialTheme.colors.onSurface,
                            disabledTextColor = MaterialTheme.colors.onSurface
                        ),
                        trailingIcon = {
                            IconButton(
                                modifier = Modifier.padding(end = Margin.Small.value),
                                onClick = {
                                    val newValue = TextFieldValue()
                                    shareTextFieldValue = newValue
                                    viewModel.updateShareMessage(newValue.text)
                                }
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = stringResource(
                                        R.string.post_settings_jetpack_social_share_message_clear
                                    ),
                                )
                            }
                        }
                    )
                    Text(
                        text = state.customizeMessageDescription,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface,
                    )
                    LaunchedEffect(Unit) {
                        // Without a delay the soft keyboard is not shown
                        delay(DELAY_SOFT_KEYBOARD_IN_MS)
                        focusRequester.requestFocus()
                    }
                }
            }
        }
        BackHandler(
            onBack = state.onBackClick,
        )
    }

    private fun observeActionEvents() {
        viewModel.actionEvents.onEach {
            when (it) {
                is ActionEvent.FinishActivity -> {
                    val data = Intent().apply {
                        putExtra(RESULT_UPDATED_SHARE_MESSAGE, it.updatedShareMessage)
                    }
                    setResult(RESULT_OK, data)
                    finish()
                }
            }
        }.launchIn(lifecycleScope)
    }

    companion object {
        @JvmStatic
        fun createIntent(
            context: Context,
            socialShareMessage: String,
        ) =
            Intent(context, EditJetpackSocialShareMessageActivity::class.java).apply {
                putExtra(EXTRA_SOCIAL_SHARE_MESSAGE, socialShareMessage)
            }

        const val EXTRA_SOCIAL_SHARE_MESSAGE = "EXTRA_SOCIAL_SHARE_MESSAGE"
        const val RESULT_UPDATED_SHARE_MESSAGE = "RESULT_UPDATED_SHARE_MESSAGE"
    }
}

private const val DELAY_SOFT_KEYBOARD_IN_MS = 100L
