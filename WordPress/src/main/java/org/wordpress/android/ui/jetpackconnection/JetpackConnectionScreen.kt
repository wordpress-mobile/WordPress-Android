package org.wordpress.android.ui.jetpackconnection

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3

@Composable
fun JetpackConnectionScreen(
    currentStep: State<JetpackConnectionViewModel.ConnectionStep?>,
    stepStatuses: State<Map<JetpackConnectionViewModel.ConnectionStep, JetpackConnectionViewModel.ConnectionStatus>>,
    onCloseClick: () -> Unit = {}
) {
    Screen(
        onCloseClick = onCloseClick,
        content = {
            JetpackConnectionSteps(
                currentStep = currentStep.value,
                stepStatuses = stepStatuses.value
            )
        }
    )
}

@Composable
private fun JetpackConnectionSteps(
    currentStep: JetpackConnectionViewModel.ConnectionStep?,
    stepStatuses: Map<JetpackConnectionViewModel.ConnectionStep, JetpackConnectionViewModel.ConnectionStatus>
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
            step = JetpackConnectionViewModel.ConnectionStep.LoginWpCom,
            title = stringResource(R.string.jetpack_connection_step_login_wpcom),
            icon = Icons.Default.AccountCircle,
            status = stepStatuses[JetpackConnectionViewModel.ConnectionStep.LoginWpCom]
                ?: JetpackConnectionViewModel.ConnectionStatus.NotStarted,
            isCurrentStep = currentStep == JetpackConnectionViewModel.ConnectionStep.LoginWpCom
        )

        ConnectionStepItem(
            step = JetpackConnectionViewModel.ConnectionStep.InstallJetpack,
            title = stringResource(R.string.jetpack_connection_step_install_jetpack),
            icon = Icons.Default.Add,
            status = stepStatuses[JetpackConnectionViewModel.ConnectionStep.InstallJetpack]
                ?: JetpackConnectionViewModel.ConnectionStatus.NotStarted,
            isCurrentStep = currentStep == JetpackConnectionViewModel.ConnectionStep.InstallJetpack
        )

        ConnectionStepItem(
            step = JetpackConnectionViewModel.ConnectionStep.ConnectSite,
            title = stringResource(R.string.jetpack_connection_step_connect_site),
            icon = Icons.Default.Home,
            status = stepStatuses[JetpackConnectionViewModel.ConnectionStep.ConnectSite]
                ?: JetpackConnectionViewModel.ConnectionStatus.NotStarted,
            isCurrentStep = currentStep == JetpackConnectionViewModel.ConnectionStep.ConnectSite
        )

        ConnectionStepItem(
            step = JetpackConnectionViewModel.ConnectionStep.ConnectWpCom,
            title = stringResource(R.string.jetpack_connection_step_connect_wpcom),
            icon = Icons.Default.Settings,
            status = stepStatuses[JetpackConnectionViewModel.ConnectionStep.ConnectWpCom]
                ?: JetpackConnectionViewModel.ConnectionStatus.NotStarted,
            isCurrentStep = currentStep == JetpackConnectionViewModel.ConnectionStep.ConnectWpCom
        )

        ConnectionStepItem(
            step = JetpackConnectionViewModel.ConnectionStep.Finalize,
            title = stringResource(R.string.jetpack_connection_step_finalize),
            icon = Icons.Default.Done,
            status = stepStatuses[JetpackConnectionViewModel.ConnectionStep.Finalize]
                ?: JetpackConnectionViewModel.ConnectionStatus.NotStarted,
            isCurrentStep = currentStep == JetpackConnectionViewModel.ConnectionStep.Finalize
        )
    }
}

@Composable
private fun ConnectionStepItem(
    step: JetpackConnectionViewModel.ConnectionStep,
    title: String,
    icon: ImageVector,
    status: JetpackConnectionViewModel.ConnectionStatus,
    isCurrentStep: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentStep) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
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
                        JetpackConnectionViewModel.ConnectionStatus.NotStarted ->
                            stringResource(R.string.jetpack_connection_status_not_started)
                        JetpackConnectionViewModel.ConnectionStatus.InProgress ->
                            stringResource(R.string.jetpack_connection_status_in_progress)
                        JetpackConnectionViewModel.ConnectionStatus.Completed ->
                            stringResource(R.string.jetpack_connection_status_completed)
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
                JetpackConnectionViewModel.ConnectionStatus.InProgress -> {
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
                JetpackConnectionViewModel.ConnectionStatus.Completed -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.jetpack_connection_status_completed),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                JetpackConnectionViewModel.ConnectionStatus.NotStarted -> {
                    // No indicator for not started
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen(
    content: @Composable () -> Unit,
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
                    .verticalScroll(rememberScrollState())
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
        JetpackConnectionViewModel.ConnectionStep.LoginWpCom to JetpackConnectionViewModel.ConnectionStatus.Completed,
        JetpackConnectionViewModel.ConnectionStep.InstallJetpack to JetpackConnectionViewModel.ConnectionStatus.Completed,
        JetpackConnectionViewModel.ConnectionStep.ConnectSite to JetpackConnectionViewModel.ConnectionStatus.InProgress,
        JetpackConnectionViewModel.ConnectionStep.ConnectWpCom to JetpackConnectionViewModel.ConnectionStatus.NotStarted,
        JetpackConnectionViewModel.ConnectionStep.Finalize to JetpackConnectionViewModel.ConnectionStatus.NotStarted
    )

    Screen(
        onCloseClick = {},
        content = {
            JetpackConnectionSteps(
                currentStep = JetpackConnectionViewModel.ConnectionStep.ConnectSite,
                stepStatuses = mockStatuses
            )
        }
    )
}
