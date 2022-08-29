package org.wordpress.android.ui.accounts.login

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.Fragment
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin

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
    Column(
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = Margin.ExtraExtraMediumLarge.value)
    ) {
        Button(
                onClick = onLoginClicked,
        ) {
            Text(stringResource(R.string.continue_with_wpcom))
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
