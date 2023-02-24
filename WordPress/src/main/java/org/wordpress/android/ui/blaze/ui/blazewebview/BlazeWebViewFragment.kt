package org.wordpress.android.ui.blaze.ui.blazewebview

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme

@AndroidEntryPoint
class BlazeWebViewFragment: Fragment() {
    companion object {
        fun newInstance() = BlazeWebViewFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setContent {
            AppTheme {
                BlazeWebViewScreen()
            }
        }
    }
}

// todo: annmarie
// (1) Implement a state for the cancel action to manage the visibility
// (2) Should the title be centered? The mocks only show iOS and not Android???
// (3) What happens on recompose when the cancel is not shown?
// (4) withFullContentAlpha wrapped around the text, but this is hard to do if wrapped in another composable?
@Composable
private fun BlazeWebViewScreen() {
    Scaffold(
        topBar = { TopAppBar() },
        content = { BlazeWebViewContent() }
    )
}

@Composable
private fun TopAppBar(isVisible: Boolean = true) {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface,
        elevation = 0.dp,
        title = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                //withFullContentAlpha { can't use this here
                Text(
                    stringResource(id = R.string.blaze_activity_title),
                    color = MaterialTheme.colors.onSurface
                )
                // }
            }
        },
        actions = {
            if (isVisible) {
                TextButton(
                    onClick = { Log.i(javaClass.simpleName, "***=> Action Clicked") }
                ) {
                    Text(
                        stringResource(id = R.string.cancel),
                        color = MaterialTheme.colors.onSurface
                    )
                }
            }
        }
    ) // TopAppBar
}



@Composable
private fun BlazeWebViewContent() {
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TopAppBarPreview() {
    TopAppBar()
}


