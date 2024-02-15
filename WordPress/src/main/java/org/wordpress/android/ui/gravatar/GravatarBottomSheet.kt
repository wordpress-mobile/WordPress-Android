package org.wordpress.android.ui.gravatar

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppTheme

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GravatarBottomSheet(
    openBottomSheet: Boolean,
    onBottomSheetDismiss: () -> Unit,
    onContinueClicked: () -> Unit,
    onLearnMoreClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    scope.launch {
        if (openBottomSheet) {
            bottomSheetState.expand()
        } else {
            bottomSheetState.hide()
        }
    }

    if (!openBottomSheet) return
    ModalBottomSheet(
        sheetState = bottomSheetState,
        onDismissRequest = onBottomSheetDismiss,
        containerColor = AppColor.White,
        modifier = modifier,
    ) {
        Box(Modifier.navigationBarsPadding()) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                GravatarBottomSheetContent(
                    onContinueClicked,
                    Modifier.padding(start = 24.dp, end = 24.dp),
                )
                LearnMoreAboutGravatarProfiles(
                    onLearnMoreClicked,
                    Modifier
                        .padding(top = 16.dp)
                        .background(AppColor.Gray10.copy(alpha = 0.2f))
                )
            }
        }
    }
}

@Composable
private fun GravatarCharacteristicRow(
    iconPainter: Painter,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        Column {
            Image(
                painter = iconPainter,
                contentDescription = stringResource(R.string.icon_desc),
                modifier = Modifier.clearAndSetSemantics {})
        }
        Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(text = description, fontSize = 13.sp)
        }
    }
}

@Composable
@Preview(showBackground = true)
fun GravatarCharacteristicRowPreview() {
    GravatarCharacteristicRow(
        iconPainter = painterResource(R.drawable.gravatar_update_once_icon),
        title = stringResource(R.string.gravatar_bottom_sheet_update_once_title),
        description = stringResource(R.string.gravatar_bottom_sheet_update_once_description),
    )
}

@Composable
private fun GravatarBottomSheetContent(onContinueClicked: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.gravatar_logo),
                contentDescription = "",
                modifier = Modifier.clearAndSetSemantics {})
            Text("Gravatar", fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp))
        }
        Text(
            stringResource(R.string.gravatar_bottom_sheet_security_info),
            modifier = Modifier.padding(top = 24.dp),
        )
        GravatarCharacteristicRow(
            iconPainter = painterResource(R.drawable.gravatar_update_once_icon),
            title = stringResource(R.string.gravatar_bottom_sheet_update_once_title),
            description = stringResource(R.string.gravatar_bottom_sheet_update_once_description),
            modifier = Modifier.padding(top = 24.dp),
        )
        GravatarCharacteristicRow(
            iconPainter = painterResource(R.drawable.gravatar_digital_identity_icon),
            title = stringResource(R.string.gravatar_bottom_sheet_digital_identity_title),
            description = stringResource(R.string.gravatar_bottom_sheet_digital_identity_description),
            modifier = Modifier.padding(top = 24.dp),
        )
        Button(
            onClick = onContinueClicked,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.simplenote_blue_60),
                contentColor = AppColor.White,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
        ) {
            Text(stringResource(R.string.gravatar_continue_label))
        }
    }
}

@Composable
@Preview(showBackground = true)
fun GravatarBottomSheetContentPreview() {
    AppTheme {
        GravatarBottomSheetContent({ })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LearnMoreAboutGravatarProfiles(onLearnMoreClicked: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        // This is a workaround to avoid the minimum touch target size enforcement
        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.gravatar_logo),
                    contentDescription = "",
                    modifier = Modifier
                        .size(20.dp)
                        .clearAndSetSemantics {},
                )
                Text(
                    stringResource(R.string.gravatar_profiles_info_label),
                    fontSize = 13.sp,
                    color = Color.Black.copy(alpha = 0.8f),
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f, fill = false)
                )
                Button(
                    onLearnMoreClicked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = colorResource(R.color.simplenote_blue_60),
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 1.dp)
                ) {
                    Text(
                        stringResource(R.string.gravatar_learn_more),
                        fontSize = 13.sp,
                    )
                    Icon(
                        painterResource(R.drawable.ic_external_v2),
                        stringResource(R.string.icon_desc),
                        tint = colorResource(R.color.simplenote_blue_60),
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(12.dp)
                            .clearAndSetSemantics {},
                    )
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun LearnMoreAboutGravatarProfilesPreview() {
    LearnMoreAboutGravatarProfiles({})
}
