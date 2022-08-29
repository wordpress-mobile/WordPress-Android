package org.wordpress.android.ui.accounts.login

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Preview
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
                        onLoginClicked = { loginPrologueListener.showEmailLoginScreen() }
                )
            }
        }
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

@Composable
private fun LoginScreenRevamped(
    onLoginClicked: () -> Unit,
) {
    Column {
        Button(
                onClick = onLoginClicked,
        ) {
            Text(text = "Login")
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
