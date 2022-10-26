package org.wordpress.android.ui.main.jetpack

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.FontSize
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.main.jetpack.JetpackWelcomeUiState.Content
import org.wordpress.android.ui.main.jetpack.JetpackWelcomeUiState.SiteListItem

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
            is Content -> ContentState(state)
            else -> Unit // TODO handle other states
        }
    }
}

@Composable
fun ContentState(uiState: Content) {
    Column(Modifier.padding(horizontal = 30.dp)) {
        Image(
                painter = painterResource(R.drawable.ic_placeholder_blavatar_grey_lighten_20_40dp),
                contentDescription = stringResource(R.string.jp_welcome_avatar_content_description),
                modifier = Modifier
                        .padding(top = Margin.MediumLarge.value)
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
                fontSize = 17.sp,
                style = TextStyle(letterSpacing = (-0.01).sp),
                modifier = Modifier
                        .padding(top = 20.dp)
        )
        if (uiState is Content.SiteList) {
            val items by remember { mutableStateOf(uiState.sites) }

            LazyColumn(
                    state = rememberLazyListState(),
                    modifier = Modifier.padding(top = 30.dp)
            ) {
                items(
                        items = items,
                        key = { it.id },
                ) { site ->
                    Row(
                            verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val painter = painterResource(R.drawable.ic_placeholder_blavatar_grey_lighten_20_40dp)
                        Image(
                                painter = painter,
                                contentDescription = null,
                                modifier = Modifier
                                        .padding(vertical = 15.dp)
                                        .padding(end = 20.dp)
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(3.dp))
                        )
                        Column {
                            Text(
                                    text = site.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp,
                            )
                            Text(
                                    text = site.url,
                                    fontSize = FontSize.Large.value,
                                    color = colorResource(R.color.gray_40)
                            )
                        }
                    }
                    Divider(color = colorResource(R.color.gray_10))
                }
            }
        }
    }
}

fun previewSiteList(): List<SiteListItem> {
    val list = mutableListOf<SiteListItem>()
    repeat(10) {
        list.add(
                SiteListItem(
                        id = it.toLong(),
                        name = "Site $it",
                        url = "site-$it.net",
                        iconUrl = "https://secure.gravatar.com/blavatar/5b6c1b7c7c7c7c7c7c7c7c7c7c7c7c7c?s=96&d=mm&r=g",
                )
        )
    }
    return list
}

@Preview(showBackground = true, widthDp = 414, heightDp = 897)
//@Preview(showBackground = true, widthDp = 414, heightDp = 897, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewContentState() {
    val uiState = Content.SiteList(previewSiteList())
    AppTheme {
        ContentState(uiState)
    }
}
