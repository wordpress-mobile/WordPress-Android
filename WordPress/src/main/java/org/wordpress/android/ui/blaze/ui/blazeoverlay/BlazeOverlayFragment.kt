package org.wordpress.android.ui.blaze.ui.blazeoverlay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.FontSize
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.compose.utils.LocaleAwareComposable
import org.wordpress.android.ui.main.jetpack.migration.compose.components.Drawable
import org.wordpress.android.ui.main.jetpack.migration.compose.components.ImageButton
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.LocaleManager


@AndroidEntryPoint
class BlazeOverlayFragment : Fragment() {
    companion object {
        fun newInstance() = BlazeOverlayFragment()
    }

    private val viewModel: BlazeActivityViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppTheme {
                val userLanguage by viewModel.refreshAppLanguage.observeAsState("")

                LocaleAwareComposable(
                    locale = LocaleManager.languageLocale(userLanguage),
                    onLocaleChange = viewModel::setAppLanguage
                ) {
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
    fun BlazeOverlayContent() {
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
