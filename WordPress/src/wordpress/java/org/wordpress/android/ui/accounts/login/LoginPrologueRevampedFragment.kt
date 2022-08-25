package org.wordpress.android.ui.accounts.login

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale.Companion
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import org.wordpress.android.R.color
import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
import org.wordpress.android.ui.accounts.login.compose.components.Tagline
import org.wordpress.android.ui.accounts.login.compose.components.PrimaryButton
import org.wordpress.android.ui.accounts.login.compose.components.SecondaryButton
import org.wordpress.android.ui.compose.theme.AppTheme

class LoginPrologueRevampedFragment: Fragment() {
    private lateinit var loginPrologueListener: LoginPrologueListener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppTheme {
                LoginScreenRevamped(
                        onLoginClicked = { loginPrologueListener.showEmailLoginScreen() }
                )
            }
        }
    }
    companion object {
        const val TAG = "login_prologue_revamped_fragment_tag"
    }

    @Suppress("TooGenericExceptionThrown")
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context !is LoginPrologueListener) {
            throw RuntimeException("$context must implement LoginPrologueListener")
        }
        loginPrologueListener = context
    }
}

@Composable
fun LoginScreenRevamped(onLoginClicked: () -> Unit) {
    Box(modifier = Modifier.background(color = colorResource(id = color.login_prologue_revamped_background))
    ) {
        Image(
                painter = painterResource(drawable.brush_stroke),
                contentDescription = stringResource(string.login_prologue_revamped_content_description_wordpress_icon),
                modifier = Modifier.align(Alignment.CenterEnd),
                contentScale = Companion.Crop
        )
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 45.dp)
        ) {
            Tagline(text = stringResource(string.login_prologue_revamped_tagline))
            PrimaryButton(
                    text = stringResource(string.login_prologue_revamped_connect),
                    onClick = onLoginClicked,
            )
            SecondaryButton(
                    text = stringResource(string.login_prologue_revamped_create),
                    onClick = { /*TODO*/ },
            )
        }
    }
}


@Preview(showBackground = true, widthDp = 400, heightDp = 700)
@Preview(showBackground = true, widthDp = 400, heightDp = 700, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PreviewLoginScreenRevamped() {
    AppTheme {
        LoginScreenRevamped(onLoginClicked = {})
    }
}
