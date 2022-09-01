package org.wordpress.android.ui.accounts.login

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import org.wordpress.android.ui.accounts.login.components.ButtonsColumn
import org.wordpress.android.ui.accounts.login.components.JetpackLogo
import org.wordpress.android.ui.accounts.login.components.PrimaryButton
import org.wordpress.android.ui.accounts.login.components.SecondaryButton
import org.wordpress.android.ui.accounts.login.components.SplashBackgroundBox
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.util.extensions.showFullScreen

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

        requireActivity().window.showInFullScreen()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        check(context is LoginPrologueListener) { "$context must implement LoginPrologueListener" }
        loginPrologueListener = context
    }

    private fun Window.showInFullScreen() {
        // Set Translucent Status Bar
        this.showFullScreen()

        // Set Translucent Navigation Bar
        setFlags(FLAG_LAYOUT_NO_LIMITS, FLAG_LAYOUT_NO_LIMITS)
    }

    companion object {
        const val TAG = "login_prologue_revamped_fragment_tag"
    }
}

/**
 * The approach used here to set the height of blurred area is not recommended in the Compose guidelines.
 * See [Recomposition loop (cyclic phase dependency)](https://developer.android.com/jetpack/compose/phases#recomp-loop)
 * A better approach would be to make use of
 * [Custom layouts](https://developer.android.com/jetpack/compose/layouts/custom),
 * but that requires further investigation.
 */
@Composable
private fun LoginScreenRevamped(
    onWpComLoginClicked: () -> Unit,
    onSiteAddressLoginClicked: () -> Unit,
) {
    val blurredAreaHeight = remember { mutableStateOf(0) }

    SplashBackgroundBox(blurredAreaHeight) {
        JetpackLogo(
                modifier = Modifier
                        .padding(top = 60.dp)
                        .size(60.dp)
        )
        Spacer(Modifier.weight(1.0f))
        ButtonsColumn(Modifier.onSizeChanged { blurredAreaHeight.value = it.height }) {
            PrimaryButton(onClick = onWpComLoginClicked)
            SecondaryButton(onClick = onSiteAddressLoginClicked)
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PreviewLoginScreenRevamped() {
    AppTheme {
        LoginScreenRevamped(
                onWpComLoginClicked = {},
                onSiteAddressLoginClicked = {}
        )
    }
}
