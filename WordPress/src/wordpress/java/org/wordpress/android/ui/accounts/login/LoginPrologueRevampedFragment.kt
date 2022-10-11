package org.wordpress.android.ui.accounts.login

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection.Rtl
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import org.wordpress.android.R.color
import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
import org.wordpress.android.ui.accounts.login.compose.components.PrimaryButton
import org.wordpress.android.ui.accounts.login.compose.components.SecondaryButton
import org.wordpress.android.ui.accounts.login.compose.components.Tagline
import org.wordpress.android.ui.compose.theme.AppTheme

class LoginPrologueRevampedFragment : Fragment() {
    private lateinit var loginPrologueListener: LoginPrologueListener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setContent {
            AppTheme {
                LoginScreenRevamped(
                        onWpComLoginClicked = loginPrologueListener::showEmailLoginScreen,
                        onSiteAddressLoginClicked = loginPrologueListener::loginViaSiteAddress,
                )
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        check(context is LoginPrologueListener) { "$context must implement LoginPrologueListener" }
        loginPrologueListener = context
    }

    override fun onResume() {
        super.onResume()
        requireActivity().window.addFlags(FLAG_LAYOUT_NO_LIMITS)
    }

    override fun onPause() {
        super.onPause()
        requireActivity().window.clearFlags(FLAG_LAYOUT_NO_LIMITS)
    }

    companion object {
        const val TAG = "login_prologue_revamped_fragment_tag"
    }
}

@Composable
fun LoginScreenRevamped(
    onWpComLoginClicked: () -> Unit,
    onSiteAddressLoginClicked: () -> Unit,
) {
    val brushStrokePainter = painterResource(id = drawable.brush_stroke)
    // Flip the background image for RTL locales
    val scaleX = if (LocalLayoutDirection.current == Rtl) -1f else 1f

    val offsetX = with(LocalDensity.current) { 10.dp.toPx() }
    val offsetY = with(LocalDensity.current) { 75.dp.toPx() }

    Box(modifier = Modifier
            .background(color = colorResource(id = color.login_prologue_revamped_background))
            .drawBehind {
                scale(scaleX = scaleX, scaleY = 1f) {
                    translate(left = size.width - brushStrokePainter.intrinsicSize.width - offsetX, top = -offsetY) {
                        with(brushStrokePainter) {
                            draw(intrinsicSize)
                        }
                    }
                }
            }
    ) {
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 45.dp)
        ) {
            Tagline(text = stringResource(string.login_prologue_revamped_tagline))
            PrimaryButton(
                    text = stringResource(string.continue_with_wpcom),
                    onClick = onWpComLoginClicked,
            )
            SecondaryButton(
                    text = stringResource(string.enter_your_site_address),
                    onClick = onSiteAddressLoginClicked,
            )
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_3A)
@Preview(showBackground = true, device = Devices.PIXEL_3A, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PreviewLoginScreenRevamped() {
    AppTheme {
        LoginScreenRevamped(onWpComLoginClicked = {}, onSiteAddressLoginClicked = {})
    }
}
