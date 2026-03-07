package org.wordpress.android.ui.accounts

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.util.EinkDeviceDetector
import org.wordpress.android.util.extensions.setContent

@AndroidEntryPoint
class DeviceInfoActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppThemeM3 {
                DeviceInfoScreen(
                    onNavigateBack = onBackPressedDispatcher::onBackPressed
                )
            }
        }
    }
}

private data class DeviceInfoSection(
    val title: String,
    val entries: List<Pair<String, String>>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceInfoScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val sections = buildDeviceInfoSections()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.device_info_title)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.back)
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            sections.forEach { section ->
                SectionHeader(title = section.title)
                section.entries.forEach { (label, value) ->
                    DeviceInfoRow(label = label, value = value)
                }
            }
            Button(
                onClick = {
                    val text = sections.joinToString("\n\n") { s ->
                        s.title + "\n" + s.entries.joinToString(
                            "\n"
                        ) { "${it.first}: ${it.second}" }
                    }
                    val clipboard = context.getSystemService(
                        ClipboardManager::class.java
                    )
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText(
                            context.getString(
                                R.string.device_info_title
                            ),
                            text
                        )
                    )
                    Toast.makeText(
                        context,
                        R.string.device_info_copied,
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.copy_to_clipboard
                    )
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp
        ),
    )
}

@Composable
private fun buildDeviceInfoSections(): List<DeviceInfoSection> {
    val einkValue = if (EinkDeviceDetector.isEinkDevice()) {
        stringResource(R.string.yes)
    } else {
        stringResource(R.string.no)
    }
    return listOf(
        DeviceInfoSection(
            title = stringResource(R.string.device_info_section_application),
            entries = listOf(
                stringResource(R.string.device_info_app_version)
                    to WordPress.versionName,
            ),
        ),
        DeviceInfoSection(
            title = stringResource(R.string.device_info_section_device),
            entries = listOf(
                stringResource(R.string.device_info_manufacturer)
                    to Build.MANUFACTURER,
                stringResource(R.string.device_info_brand)
                    to Build.BRAND,
                stringResource(R.string.device_info_model)
                    to Build.MODEL,
                stringResource(R.string.device_info_device)
                    to Build.DEVICE,
                stringResource(R.string.device_info_product)
                    to Build.PRODUCT,
                stringResource(R.string.device_info_eink_detected)
                    to einkValue,
            ),
        ),
        DeviceInfoSection(
            title = stringResource(R.string.device_info_section_android),
            entries = listOf(
                stringResource(R.string.device_info_android_version)
                    to Build.VERSION.RELEASE,
                stringResource(R.string.device_info_sdk_level)
                    to Build.VERSION.SDK_INT.toString(),
            ),
        ),
    )
}

@Composable
private fun DeviceInfoRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal,
        )
    }
    HorizontalDivider()
}

@Preview(showBackground = true)
@Composable
private fun DeviceInfoRowPreview() {
    AppThemeM3 {
        DeviceInfoRow(
            label = "App version",
            value = "24.5-rc-1"
        )
    }
}
