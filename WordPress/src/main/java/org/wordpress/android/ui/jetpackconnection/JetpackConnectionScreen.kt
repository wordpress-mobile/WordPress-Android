package org.wordpress.android.ui.jetpackconnection

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.buttons.PrimaryButtonM3
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.jetpackconnection.JetpackConnectionViewModel.ButtonType
import org.wordpress.android.ui.jetpackconnection.JetpackConnectionViewModel.ConnectionStatus
import org.wordpress.android.ui.jetpackconnection.JetpackConnectionViewModel.ConnectionStep
import org.wordpress.android.ui.jetpackconnection.JetpackConnectionViewModel.StepState

@Composable
fun JetpackConnectionScreen(
    currentStep: State<ConnectionStep?>,
    stepStates: State<Map<ConnectionStep, StepState>>,
    buttonType: State<ButtonType?>,
    onCloseClick: () -> Unit = {},
    onRetryClick: () -> Unit = {}
) {
    Screen(
        onCloseClick = onCloseClick,
        content = {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                JetpackConnectionSteps(
                    currentStep = currentStep.value,
                    stepStates = stepStates.value
                )
                AnimatedVisibility(
                    visible = buttonType.value != null,
                    enter = fadeIn()
                ) {
                    JetpackConnectionButton(
                        buttonType = buttonType.value,
                        onClick = {
                            when (buttonType.value) {
                                ButtonType.Done -> onCloseClick()
                                ButtonType.Retry -> onRetryClick()
                                null -> {}
                            }
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun JetpackConnectionButton(
    buttonType: ButtonType?,
    onClick: () -> Unit
) {
    val labelRes = when (buttonType) {
        ButtonType.Done -> R.string.label_done_button
        ButtonType.Retry -> R.string.retry
        null -> return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PrimaryButtonM3(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(labelRes),
        )
    }
}

@Composable
private fun JetpackConnectionSteps(
    currentStep: ConnectionStep?,
    stepStates: Map<ConnectionStep, StepState>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.jetpack_connection_setup_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ConnectionStepItem(
            step = ConnectionStep.LoginWpCom,
            title = stringResource(R.string.jetpack_connection_step_login_wpcom),
            icon = Icons.Default.AccountCircle,
            stepState = stepStates[ConnectionStep.LoginWpCom]
                ?: StepState(),
            isCurrentStep = currentStep == ConnectionStep.LoginWpCom
        )

        ConnectionStepItem(
            step = ConnectionStep.InstallJetpack,
            title = stringResource(R.string.jetpack_connection_step_install_jetpack),
            icon = Icons.Default.Add,
            stepState = stepStates[ConnectionStep.InstallJetpack]
                ?: StepState(),
            isCurrentStep = currentStep == ConnectionStep.InstallJetpack
        )

        ConnectionStepItem(
            step = ConnectionStep.ConnectSite,
            title = stringResource(R.string.jetpack_connection_step_connect_site),
            icon = Icons.Default.Home,
            stepState = stepStates[ConnectionStep.ConnectSite]
                ?: StepState(),
            isCurrentStep = currentStep == ConnectionStep.ConnectSite
        )

        ConnectionStepItem(
            step = ConnectionStep.ConnectWpCom,
            title = stringResource(R.string.jetpack_connection_step_connect_wpcom),
            icon = Icons.Default.Settings,
            stepState = stepStates[ConnectionStep.ConnectWpCom]
                ?: StepState(),
            isCurrentStep = currentStep == ConnectionStep.ConnectWpCom
        )

        ConnectionStepItem(
            step = ConnectionStep.Finalize,
            title = stringResource(R.string.jetpack_connection_step_finalize),
            icon = Icons.Default.Done,
            stepState = stepStates[ConnectionStep.Finalize]
                ?: StepState(),
            isCurrentStep = currentStep == ConnectionStep.Finalize
        )
    }
}

@Composable
private fun ConnectionStepItem(
    step: ConnectionStep,
    title: String,
    icon: ImageVector,
    stepState: StepState,
    isCurrentStep: Boolean
) {
    val status = stepState.status
    val style = rememberConnectionStepStyle(status, isCurrentStep)

    Row(
        modifier = style.modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ConnectionStepIcon(
            icon = icon,
            style = style
        )

        Spacer(modifier = Modifier.width(16.dp))

        ConnectionStepContent(
            title = title,
            status = status,
            errorMessage = stepState.errorMessage,
            style = style,
            modifier = Modifier.weight(1f)
        )

        ConnectionStepStatusIndicator(
            status = status,
            style = style
        )
    }
}

@Composable
private fun ConnectionStepIcon(
    icon: ImageVector,
    style: ConnectionStepStyle
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        tint = style.iconColor
    )
}

@Composable
private fun ConnectionStepContent(
    title: String,
    status: ConnectionStatus,
    errorMessage: String?,
    style: ConnectionStepStyle,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = style.titleFontWeight,
            color = style.titleColor
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = getStatusText(status),
            style = MaterialTheme.typography.bodyMedium,
            color = style.statusColor
        )

        if (errorMessage != null && status == ConnectionStatus.Failed) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ConnectionStepStatusIndicator(
    status: ConnectionStatus,
    style: ConnectionStepStyle
) {
    when (status) {
        ConnectionStatus.InProgress -> {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = style.progressColor
            )
        }

        ConnectionStatus.Completed -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.jetpack_connection_status_completed),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        ConnectionStatus.Failed -> {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = stringResource(R.string.jetpack_connection_status_failed),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }

        ConnectionStatus.NotStarted -> {
            // No indicator for not started
        }
    }
}

@Composable
private fun getStatusText(status: ConnectionStatus): String {
    return when (status) {
        ConnectionStatus.NotStarted -> stringResource(R.string.jetpack_connection_status_not_started)
        ConnectionStatus.InProgress -> stringResource(R.string.jetpack_connection_status_in_progress)
        ConnectionStatus.Completed -> stringResource(R.string.jetpack_connection_status_completed)
        ConnectionStatus.Failed -> stringResource(R.string.jetpack_connection_status_failed)
    }
}

@Composable
private fun rememberConnectionStepStyle(
    status: ConnectionStatus,
    isCurrentStep: Boolean
): ConnectionStepStyle {
    val targetAlpha = if (status == ConnectionStatus.Completed) 0.6f else 1f
    val animatedAlpha by animateFloatAsState(targetValue = targetAlpha)

    val targetColor = when {
        status == ConnectionStatus.Completed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        status == ConnectionStatus.InProgress -> Color(IN_PROGRESS_BACKGROUND_COLOR)
        status == ConnectionStatus.Failed -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
        isCurrentStep -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val animatedColor by animateColorAsState(targetValue = targetColor)

    val targetElevation = if (status == ConnectionStatus.NotStarted) 2.dp else 0.dp
    val animatedElevation by animateDpAsState(targetValue = targetElevation)

    val shape = MaterialTheme.shapes.medium

    val iconColor = when {
        status == ConnectionStatus.InProgress -> Color(IN_PROGRESS_FOREGROUND_COLOR)
        isCurrentStep -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val titleColor = when {
        status == ConnectionStatus.InProgress -> Color(IN_PROGRESS_FOREGROUND_COLOR)
        isCurrentStep -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val statusColor = when {
        status == ConnectionStatus.InProgress -> Color(IN_PROGRESS_FOREGROUND_COLOR).copy(alpha = 0.7f)
        isCurrentStep -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    }

    val progressColor = when {
        status == ConnectionStatus.InProgress -> Color(IN_PROGRESS_FOREGROUND_COLOR)
        isCurrentStep -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.primary
    }

    return ConnectionStepStyle(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = animatedElevation,
                shape = shape,
                clip = false
            )
            .clip(shape)
            .background(animatedColor)
            .alpha(animatedAlpha)
            .padding(16.dp),
        iconColor = iconColor,
        titleColor = titleColor,
        statusColor = statusColor,
        progressColor = progressColor,
        titleFontWeight = if (isCurrentStep) FontWeight.Bold else FontWeight.Normal
    )
}

private data class ConnectionStepStyle(
    val modifier: Modifier,
    val iconColor: Color,
    val titleColor: Color,
    val statusColor: Color,
    val progressColor: Color,
    val titleFontWeight: FontWeight
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen(
    content: @Composable (ColumnScope.() -> Unit),
    onCloseClick: () -> Unit
) {
    AppThemeM3 {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.jetpack_connection_title)) },
                    navigationIcon = {
                        IconButton(onClick = onCloseClick) {
                            Icon(Icons.Filled.Close, stringResource(R.string.close))
                        }
                    },
                )
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(contentPadding)
            ) {
                content()
            }
        }
    }
}

@Preview(
    name = "Light Mode",
    showBackground = true
)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun JetpackConnectionScreenPreview() {
    val currentStep = remember { mutableStateOf(ConnectionStep.ConnectSite) }
    val stepStates = remember {
        mutableStateOf(
            mapOf(
                ConnectionStep.LoginWpCom to StepState(ConnectionStatus.Completed),
                ConnectionStep.InstallJetpack to StepState(ConnectionStatus.Completed),
                ConnectionStep.ConnectSite to StepState(ConnectionStatus.InProgress),
                ConnectionStep.ConnectWpCom to StepState(
                    ConnectionStatus.Failed,
                    "Failed to connect to WordPress.com"
                ),
                ConnectionStep.Finalize to StepState(ConnectionStatus.NotStarted)
            )
        )
    }
    val buttonType = remember { mutableStateOf<ButtonType?>(ButtonType.Done) }

    JetpackConnectionScreen(
        currentStep = currentStep,
        stepStates = stepStates,
        buttonType = buttonType,
        onCloseClick = {},
        onRetryClick = {}
    )
}

private const val IN_PROGRESS_BACKGROUND_COLOR = 0xFFFFF9C4 // Light yellow
private const val IN_PROGRESS_FOREGROUND_COLOR = 0xFF5D4037 // Dark brown for readability on the above
