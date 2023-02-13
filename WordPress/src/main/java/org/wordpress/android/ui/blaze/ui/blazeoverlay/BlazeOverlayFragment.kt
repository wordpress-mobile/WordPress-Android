package org.wordpress.android.ui.blaze.ui.blazeoverlay

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import coil.compose.rememberImagePainter
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.blaze.BlazeUiState
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.FontSize
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.compose.utils.LocaleAwareComposable
import org.wordpress.android.ui.main.jetpack.migration.compose.components.Drawable
import org.wordpress.android.ui.main.jetpack.migration.compose.components.ImageButton
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.LocaleManager
import org.wordpress.android.util.UrlUtils

//todo: post thumbnail view
//todo: promote blaze button theme
//todo: post data details


@AndroidEntryPoint
class BlazeOverlayFragment : Fragment() {
    companion object {
        fun newInstance() = BlazeOverlayFragment()
    }

    private val viewModel: BlazeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppTheme {
                val userLanguage by viewModel.refreshAppLanguage.observeAsState("")
                viewModel.uiState.observe(viewLifecycleOwner) { uiState ->
//                    when(uiState) {
//                        is BlazeUiState.PromoteScreen.Site -> {
//                            BlazeOverlayContent()
//                        }
//                        is BlazeUiState.PromoteScreen.PromotePost -> {
//                            BlazeOverlayContent(uiState.postModel)
//                        }
//                    }
                }

                LocaleAwareComposable(
                    locale = LocaleManager.languageLocale(userLanguage),
                    onLocaleChange = viewModel::setAppLanguage
                ) {
                    viewModel.trackOverlayDisplayed()
                    BlazeOverlayContent()
                }
            }
        }
    }


    @Preview
    @Composable
    fun BlazeOverlayContentPreview() {
        AppTheme {
            BlazeOverlayContent()
        }
    }

    @Composable
    fun BlazeOverlayContent(postModel: PostModel? = null) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(
                top = Margin.ExtraLarge.value,
                start = Margin.ExtraLarge.value,
                end = Margin.ExtraLarge.value
            )
        ) {
            Image(
                painterResource(id = R.drawable.ic_blaze_promotional_image),
                contentDescription = stringResource(id = R.string.blaze_activity_title),
                modifier = Modifier.size(100.dp)
            )
            Text(
                stringResource(id = R.string.blaze_promotional_text),
                fontSize = FontSize.DoubleExtraLarge.value,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = Margin.ExtraExtraMediumLarge.value),
                textAlign = TextAlign.Center
            )
            Subtitles(
                listOf(
                    R.string.blaze_promotional_subtitle_1,
                    R.string.blaze_promotional_subtitle_2,
                    R.string.blaze_promotional_subtitle_3
                )
            )
            postModel?.autoSavePreviewUrl?.let { PostThumbnailView(url = it) }
            ImageButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        Color.Gray,
                        shape = RoundedCornerShape(6.dp)
                    ),
                buttonText = UiString.UiStringRes(R.string.blaze_post_promotional_button),
                drawableRight = Drawable(R.drawable.ic_promote_with_blaze),
                onClick = { viewModel.onPromoteWithBlazeClicked() }
            )
        }
    }

    @Composable
    fun PostThumbnailView(url: String) {
        AndroidView(factory = {
            WebView(requireContext()).apply {
                webViewClient = WebViewClient()
                loadUrl(UrlUtils.makeHttps(url))
            }
        },
            Modifier
                .height(50.dp)
                .fillMaxWidth())
    }

    @Composable
    private fun PostThumbnail(url: String) {
        val painter = rememberImagePainter(url) {
            placeholder(R.drawable.bg_rectangle_placeholder_globe_margin_8dp)
            error(R.drawable.bg_rectangle_placeholder_globe_margin_8dp)
            crossfade(true)
        }
        Image(
            painter = painter,
            contentDescription = stringResource(R.string.blavatar_desc),
            modifier = Modifier
                .padding(vertical = 15.dp)
                .padding(end = 20.dp)
                .size(dimensionResource(R.dimen.jp_migration_site_icon_size))
                .clip(RoundedCornerShape(3.dp))
        )
    }


    @Composable
    private fun PostTitle(title: String) {
        Text(
            text = title,
            style = MaterialTheme.typography.h6,
            maxLines = 2,
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    @Composable
    private fun PostUrl(url: String) {
        Text(
            text = url,
            style = MaterialTheme.typography.body2,
            maxLines = 1,
            modifier = Modifier.padding(top = 8.dp)
        )
    }


    @Composable
    fun Subtitles(list: List<Int>, modifier: Modifier = Modifier) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier.padding(
                top = Margin.ExtraLarge.value,
                bottom = Margin.ExtraLarge.value
            )
        ) {
            items(list) {
                BulletedText(it)
            }
        }
    }

    @Composable
    fun BulletedText(stringResource: Int) {
        Row(horizontalArrangement = Arrangement.Start) {
            Canvas(modifier = Modifier
                .padding(start = 8.dp, top = 12.dp)
                .size(6.dp),
                onDraw = {
                    drawCircle(Color.LightGray)
                })
            Text(
                modifier = Modifier.padding(start = Margin.ExtraLarge.value),
                text = stringResource(id = stringResource),
                fontSize = FontSize.Large.value,
                fontWeight = FontWeight.Light,
                color = Color.Gray
            )
        }
    }
}
