package org.wordpress.android.ui.jetpackconnection

import android.content.res.Configuration
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.jetpackconnection.JetpackConnectionViewModel.ConnectionStatus
import org.wordpress.android.ui.jetpackconnection.JetpackConnectionViewModel.ConnectionStep

@Composable
fun JetpackConnectionScreen(
    currentStep: State<ConnectionStep?>,
    stepStatuses: State<Map<ConnectionStep, ConnectionStatus>>,
    onCloseClick: () -> Unit = {},
    showDoneButton: State<Boolean>
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
                    stepStatuses = stepStatuses.value
                )
            }
            AnimatedVisibility(
                visible = showDoneButton.value,
                enter = fadeIn()
            ) {
                JetpackConnectionDoneButton(onClick = onCloseClick)
            }
        }
    )
}

@Composable
private fun JetpackConnectionDoneButton(
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.label_done_button))
        }
    }
}

@Composable
private fun JetpackConnectionSteps(
    currentStep: ConnectionStep?,
    stepStatuses: Map<ConnectionStep, ConnectionStatus>
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
            status = stepStatuses[ConnectionStep.LoginWpCom]
                ?: ConnectionStatus.NotStarted,
            isCurrentStep = currentStep == ConnectionStep.LoginWpCom
        )

        ConnectionStepItem(
            step = ConnectionStep.InstallJetpack,
            title = stringResource(R.string.jetpack_connection_step_install_jetpack),
            icon = Icons.Default.Add,
            status = stepStatuses[ConnectionStep.InstallJetpack]
                ?: ConnectionStatus.NotStarted,
            isCurrentStep = currentStep == ConnectionStep.InstallJetpack
        )

        ConnectionStepItem(
            step = ConnectionStep.ConnectSite,
            title = stringResource(R.string.jetpack_connection_step_connect_site),
            icon = Icons.Default.Home,
            status = stepStatuses[ConnectionStep.ConnectSite]
                ?: ConnectionStatus.NotStarted,
            isCurrentStep = currentStep == ConnectionStep.ConnectSite
        )

        ConnectionStepItem(
            step = ConnectionStep.ConnectWpCom,
            title = stringResource(R.string.jetpack_connection_step_connect_wpcom),
            icon = Icons.Default.Settings,
            status = stepStatuses[ConnectionStep.ConnectWpCom]
                ?: ConnectionStatus.NotStarted,
            isCurrentStep = currentStep == ConnectionStep.ConnectWpCom
        )

        ConnectionStepItem(
            step = ConnectionStep.Finalize,
            title = stringResource(R.string.jetpack_connection_step_finalize),
            icon = Icons.Default.Done,
            status = stepStatuses[ConnectionStep.Finalize]
                ?: ConnectionStatus.NotStarted,
            isCurrentStep = currentStep == ConnectionStep.Finalize
        )
    }
}

@Composable
private fun ConnectionStepItem(
    step: ConnectionStep,
    title: String,
    icon: ImageVector,
    status: ConnectionStatus,
    isCurrentStep: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (status == ConnectionStatus.Completed) 0.6f else 1f),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCurrentStep -> MaterialTheme.colorScheme.primaryContainer
                status == ConnectionStatus.Failed -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isCurrentStep) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isCurrentStep) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrentStep) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when (status) {
                        ConnectionStatus.NotStarted ->
                            stringResource(R.string.jetpack_connection_status_not_started)
                        ConnectionStatus.InProgress ->
                            stringResource(R.string.jetpack_connection_status_in_progress)
                        ConnectionStatus.Completed ->
                            stringResource(R.string.jetpack_connection_status_completed)
                        ConnectionStatus.Failed ->
                            stringResource(R.string.jetpack_connection_status_failed)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrentStep) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    }
                )
            }

            when (status) {
                ConnectionStatus.InProgress -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = if (isCurrentStep) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
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
    }
}

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
    val mockStatuses = mapOf(
        ConnectionStep.LoginWpCom to ConnectionStatus.Completed,
        ConnectionStep.InstallJetpack to ConnectionStatus.Completed,
        ConnectionStep.ConnectSite to ConnectionStatus.InProgress,
        ConnectionStep.ConnectWpCom to ConnectionStatus.Failed,
        ConnectionStep.Finalize to ConnectionStatus.NotStarted
    )

    Screen(
        onCloseClick = {},
        content = {
            JetpackConnectionSteps(
                currentStep = ConnectionStep.ConnectSite,
                stepStatuses = mockStatuses
            )
        }
    )
}
