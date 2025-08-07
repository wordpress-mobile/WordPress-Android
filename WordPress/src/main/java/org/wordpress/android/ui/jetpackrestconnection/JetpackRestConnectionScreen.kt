package org.wordpress.android.ui.jetpackrestconnection

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.ScreenWithTopAppBarM3
import org.wordpress.android.ui.compose.components.buttons.PrimaryButtonM3
import org.wordpress.android.ui.jetpackrestconnection.JetpackRestConnectionViewModel.ButtonType
import org.wordpress.android.ui.jetpackrestconnection.JetpackRestConnectionViewModel.ConnectionStatus
import org.wordpress.android.ui.jetpackrestconnection.JetpackRestConnectionViewModel.ConnectionStep
import org.wordpress.android.ui.jetpackrestconnection.JetpackRestConnectionViewModel.ErrorType
import org.wordpress.android.ui.jetpackrestconnection.JetpackRestConnectionViewModel.StepState

@Composable
fun JetpackRestConnectionScreen(
    currentStep: State<ConnectionStep?>,
    stepStates: State<Map<ConnectionStep, StepState>>,
    buttonType: State<ButtonType?>,
    onStartClick: () -> Unit = {},
    onCloseClick: () -> Unit = {},
    onRetryClick: () -> Unit = {}
) {
    ScreenWithTopAppBarM3(
        titleRes = R.string.jetpack_rest_connection_setup_title,
        onCloseClick = onCloseClick,
        content = {
            Column {
                JetpackConnectionSteps(
                    currentStep = currentStep.value,
                    stepStates = stepStates.value,
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                )

                AnimatedVisibility(
                    visible = buttonType.value != null,
                    enter = fadeIn()
                ) {
                    JetpackConnectionButton(
                        buttonType = buttonType.value,
                        onStartClick = onStartClick,
                        onDoneClick = onCloseClick,
                        onRetryClick = onRetryClick
                    )
                }
            }
        },
    )
}

@Composable
private fun JetpackConnectionButton(
    buttonType: ButtonType?,
    onStartClick: () -> Unit = {},
    onDoneClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    val (labelRes, onClick) = when (buttonType) {
        ButtonType.Done -> R.string.label_done_button to onDoneClick
        ButtonType.Retry -> R.string.retry to onRetryClick
        ButtonType.Start -> R.string.start to onStartClick
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

private data class StepConfig(
    val step: ConnectionStep,
    val titleRes: Int,
    val icon: ImageVector
)

private val stepConfigs = listOf(
    StepConfig(
        step = ConnectionStep.LoginWpCom,
        titleRes = R.string.jetpack_rest_connection_step_login_wpcom,
        icon = Icons.Default.AccountCircle
    ),
    StepConfig(
        step = ConnectionStep.InstallJetpack,
        titleRes = R.string.jetpack_rest_connection_step_install_jetpack,
        icon = Icons.Default.Build
    ),
    StepConfig(
        step = ConnectionStep.ConnectSite,
        titleRes = R.string.jetpack_rest_connection_step_connect_site,
        icon = Icons.Default.Home
    ),
    StepConfig(
        step = ConnectionStep.ConnectWpCom,
        titleRes = R.string.jetpack_rest_connection_step_connect_wpcom,
        icon = Icons.Default.Settings
    ),
    StepConfig(
        step = ConnectionStep.Finalize,
        titleRes = R.string.jetpack_rest_connection_step_finalize,
        icon = Icons.Default.CheckCircle
    )
)

@Composable
private fun JetpackConnectionSteps(
    currentStep: ConnectionStep?,
    stepStates: Map<ConnectionStep, StepState>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        stepConfigs.forEach { config ->
            ConnectionStepItem(
                title = stringResource(config.titleRes),
                icon = config.icon,
                stepState = stepStates[config.step] ?: StepState(),
                isCurrentStep = currentStep == config.step
            )
        }
    }
}

@Composable
private fun ConnectionStepItem(
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
            errorType = stepState.errorType,
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
    errorType: ErrorType?,
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

        if (errorType != null && status == ConnectionStatus.Failed) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = getErrorText(LocalContext.current, errorType),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun getErrorText(context: Context, errorType: ErrorType): String {
    @StringRes val messageRes = when (errorType) {
        is ErrorType.JetpackAlreadyInstalled -> R.string.jetpack_rest_connection_error_jetpack_already_installed
        is ErrorType.Timeout -> R.string.jetpack_rest_connection_error_timeout
        is ErrorType.Offline -> R.string.jetpack_rest_connection_error_offline
        is ErrorType.Unknown -> R.string.jetpack_rest_connection_error_unknown
        is ErrorType.FailedToConnectWpCom -> R.string.jetpack_rest_connection_error_wpcom
    }
    val baseMessage = context.getString(messageRes)
    return errorType.message?.let { "$baseMessage: $it" } ?: baseMessage
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
                contentDescription = stringResource(R.string.jetpack_rest_connection_status_completed),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        ConnectionStatus.Failed -> {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = stringResource(R.string.jetpack_rest_connection_status_failed),
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
private fun getStatusText(status: ConnectionStatus): String = when (status) {
    ConnectionStatus.NotStarted -> stringResource(R.string.jetpack_rest_connection_status_not_started)
    ConnectionStatus.InProgress -> stringResource(R.string.jetpack_rest_connection_status_in_progress)
    ConnectionStatus.Completed -> stringResource(R.string.jetpack_rest_connection_status_completed)
    ConnectionStatus.Failed -> stringResource(R.string.jetpack_rest_connection_status_failed)
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
        status == ConnectionStatus.InProgress -> colorResource(IN_PROGRESS_BACKGROUND_COLOR)
        status == ConnectionStatus.Failed -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
        isCurrentStep -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val animatedColor by animateColorAsState(targetValue = targetColor)

    val elevation = when (status) {
        ConnectionStatus.NotStarted -> 2.dp
        ConnectionStatus.InProgress -> 4.dp
        else -> 0.dp
    }

    val shape = MaterialTheme.shapes.medium

    val iconColor = when {
        status == ConnectionStatus.InProgress -> colorResource(IN_PROGRESS_FOREGROUND_COLOR)
        isCurrentStep -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val titleColor = when {
        status == ConnectionStatus.InProgress -> colorResource(IN_PROGRESS_FOREGROUND_COLOR)
        isCurrentStep -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val statusColor = when {
        status == ConnectionStatus.InProgress -> colorResource(IN_PROGRESS_FOREGROUND_COLOR).copy(alpha = 0.7f)
        isCurrentStep -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    }

    val progressColor = when {
        status == ConnectionStatus.InProgress -> colorResource(IN_PROGRESS_FOREGROUND_COLOR)
        isCurrentStep -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.primary
    }

    return ConnectionStepStyle(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = elevation,
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
private fun JetpackRestConnectionScreenPreview() {
    val currentStep = remember { mutableStateOf(ConnectionStep.ConnectSite) }
    val stepStates = remember {
        mutableStateOf(
            mapOf(
                ConnectionStep.LoginWpCom to StepState(ConnectionStatus.Completed),
                ConnectionStep.InstallJetpack to StepState(ConnectionStatus.Completed),
                ConnectionStep.ConnectSite to StepState(ConnectionStatus.InProgress),
                ConnectionStep.ConnectWpCom to StepState(
                    ConnectionStatus.Failed,
                    ErrorType.FailedToConnectWpCom()
                ),
                ConnectionStep.Finalize to StepState(ConnectionStatus.NotStarted)
            )
        )
    }
    val buttonType = remember { mutableStateOf<ButtonType?>(ButtonType.Done) }

    JetpackRestConnectionScreen(
        currentStep = currentStep,
        stepStates = stepStates,
        buttonType = buttonType,
        onCloseClick = {},
        onRetryClick = {}
    )
}

@ColorRes private val IN_PROGRESS_BACKGROUND_COLOR = R.color.yellow_10 // Light yellow
@ColorRes private val IN_PROGRESS_FOREGROUND_COLOR = R.color.yellow_90 // Dark brown for readability on the above
