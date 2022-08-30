package org.wordpress.android.ui.accounts.login

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import org.wordpress.android.R
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

        requireActivity().window.showFullScreen()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context !is LoginPrologueListener) {
            error("$context must implement LoginPrologueListener")
        }
        loginPrologueListener = context
    }

    companion object {
        const val TAG = "login_prologue_revamped_fragment_tag"
    }
}

private fun Window.showFullScreen() {
    // Set Translucent Status Bar
    @Suppress("DEPRECATION")
    decorView.systemUiVisibility = decorView.systemUiVisibility.let {
        it or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or SYSTEM_UI_FLAG_LAYOUT_STABLE
    }
    // Set Translucent Navigation Bar
    setFlags(FLAG_LAYOUT_NO_LIMITS, FLAG_LAYOUT_NO_LIMITS)
}


@Composable
private fun LoginScreenRevamped(
    onWpComLoginClicked: () -> Unit,
    onSiteAddressLoginClicked: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                    painter = painterResource(R.drawable.bg_jetpack_login_splash),
                    contentDescription = stringResource(R.string.login_prologue_revamped_content_description_bg),
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.matchParentSize(),
            )
            Image(
                    painter = painterResource(R.drawable.bg_jetpack_login_splash_top_gradient),
                    contentDescription = stringResource(R.string.login_prologue_revamped_content_description_top_bg),
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height = 292.dp),
            )
            Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
            ) {
                Image(
                        painter = painterResource(R.drawable.ic_jetpack_logo_green_24dp),
                        contentDescription = stringResource(
                                R.string.login_prologue_revamped_content_description_jetpack_logo
                        ),
                        modifier = Modifier
                                .padding(top = 60.dp)
                                .size(60.dp)
                )
                Spacer(modifier = Modifier.weight(1.0f))
                Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Button(
                            onClick = onWpComLoginClicked,
                            colors = ButtonDefaults.buttonColors(
                                    backgroundColor = MaterialTheme.colors.primary,
                                    contentColor = Color.White,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.continue_with_wpcom_no_signup))
                    }
                    Button(
                            onClick = onSiteAddressLoginClicked,
                            colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color.White,
                                    contentColor = Color.Black,
                            ),
                            modifier = Modifier
                                    .padding(bottom = 60.dp)
                                    .fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.enter_your_site_address))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Preview(showBackground = true, device = Devices.PIXEL_4, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PreviewLoginScreenRevamped() {
    AppTheme {
        LoginScreenRevamped(onWpComLoginClicked = {}, onSiteAddressLoginClicked = {})
    }
}
