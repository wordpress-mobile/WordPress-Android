package org.wordpress.android.ui.accounts.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.viewmodel.accounts.SelfHostedLoginFragmentViewModel

class SelfHostedLoginFragment: Fragment() {
    companion object {
        const val TAG: String = "self_hosted_login_fragment_tag"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            this.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SelfHostedLoginView()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SelfHostedLoginView(viewModel: SelfHostedLoginFragmentViewModel = viewModel()) {
        val siteUrl by viewModel.siteUrl.collectAsState()

        viewModel.context = LocalContext.current
        // TODO: Have the state reflected in the UI

        Scaffold(topBar = {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.log_in)) },
                    navigationIcon = {
                        IconButton(onClick = { /* do something */ }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Login Start"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* do something */ }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_help_white_24dp),
                                contentDescription = "Back to Login Start"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding)
            ) {
                Text(stringResource(R.string.enter_site_address))
                OutlinedTextField(
                    value = siteUrl, // Start with a space to ensure the label stays on top
                    label = {
                        Text("Site address")
                    },
                    onValueChange = viewModel::setSiteUrl
                )
                Text(stringResource(R.string.login_find_your_site_adress))
                Spacer(Modifier.padding())
                Button(viewModel::didTapContinue) {
                    Text(stringResource(R.string.continue_label))
                }
            }
        }
    }

    @Preview
    @Composable
    fun PreviewSelfHostedLoginView() {
        AppTheme {
            SelfHostedLoginView()
        }
    }
}
