package org.wordpress.android.ui.main.jetpack

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.FontSize
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.main.jetpack.JetpackWelcomeUiState.Error
import org.wordpress.android.ui.main.jetpack.JetpackWelcomeUiState.Initial

@AndroidEntryPoint
class JetpackWelcomeFragment : Fragment() {
    private val viewModel: JetpackWelcomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppTheme {
                JetpackWelcomeScreen()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.start()
    }
}

@Composable
private fun JetpackWelcomeScreen(viewModel: JetpackWelcomeViewModel = viewModel()) {
    Box {
        val uiState by viewModel.uiState.collectAsState()

        @Suppress("UnnecessaryVariable") // See: https://stackoverflow.com/a/69558316/4129245
        when (val state = uiState) {
            is Initial -> SiteList(state)
            is Error -> Unit // TODO handle Error
        }
    }
}

@Composable
fun SiteList(uiState: Initial) {
    Column(Modifier.padding(horizontal = 30.dp)) {
        Image(
                painter = painterResource(R.drawable.ic_placeholder_blavatar_grey_lighten_20_40dp),
                contentDescription = stringResource(R.string.jp_welcome_avatar_content_description),
                modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(1.dp, colorResource(R.color.black_translucent_20), CircleShape)
                        .align(Alignment.End)
        )
        Image(
                painter = painterResource(R.drawable.ic_wordpress_jetpack_logo),
                contentDescription = stringResource(R.string.jp_welcome_icon_logos_content_description),
                modifier = Modifier
                        .width(123.dp)
                        .height(65.dp)
                        .padding(top = 4.dp)
        )
        Text(
                text = uiStringText(uiState.title),
                fontSize = FontSize.ExtraExtraExtraLarge.value,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                        .padding(top = 30.dp)
        )
        Text(
                text = uiStringText(uiState.subtitle),
                fontSize = FontSize.ExtraLarge.value,
                modifier = Modifier
                        .padding(top = 20.dp)
        )
        Text(
                text = uiStringText(uiState.message),
                fontSize = FontSize.Large.value,
                modifier = Modifier
                        .padding(top = 20.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 414, heightDp = 897)
//@Preview(showBackground = true, widthDp = 414, heightDp = 897, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewSiteList() {
    AppTheme {
        SiteList(Initial)
    }
}
