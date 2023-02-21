package org.wordpress.android.ui.blaze.ui.blazeoverlay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import coil.compose.rememberImagePainter
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.blaze.BlazeUiState
import org.wordpress.android.ui.blaze.PostUIModel
import org.wordpress.android.ui.compose.theme.AppColor
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

    private val viewModel: BlazeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppTheme {
                val userLanguage by viewModel.refreshAppLanguage.observeAsState("")
                val postModel by viewModel.promoteUiState.observeAsState(BlazeUiState.PromoteScreen.Site)
                LocaleAwareComposable(
                    locale = LocaleManager.languageLocale(userLanguage),
                    onLocaleChange = viewModel::setAppLanguage
                ) {
                    viewModel.trackOverlayDisplayed()
                    BlazeOverlayContent(postModel)
                }
            }
        }
    }

    @Composable
    fun BlazeOverlayContent(
        postModelState: BlazeUiState.PromoteScreen,
        isDarkTheme: Boolean = false
    ) {
        val post = when (postModelState) {
            is BlazeUiState.PromoteScreen.PromotePost -> postModelState.postUIModel
            else -> null
        }
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
                ),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp)
            )
            post?.let {
                PostThumbnailView(
                    it, modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                )
            }

            ImageButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(start = 20.dp, end = 20.dp)
                    .background(
                        Color.Black,
                        shape = RoundedCornerShape(15.dp)
                    ),
                buttonText = UiString.UiStringRes(R.string.blaze_post_promotional_button),
                drawableRight = Drawable(R.drawable.ic_promote_with_blaze),
                onClick = { viewModel.onPromoteWithBlazeClicked() }
            )
        }
    }

    @Preview
    @Composable
    fun PreviewThumbNailView() {
        AppTheme {
            BlazeOverlayContent(
                postModelState = BlazeUiState.PromoteScreen.PromotePost(
                    PostUIModel(
                        postId = 119,
                        title = "Post title check if this is long enough to be truncated",
                        url = "https://www.google.long.ttiiitititititiit.com",
                        imageUrl = 0
                    )
                )
            )
        }
    }

    @Composable
    fun PostThumbnailView(postUIModel: PostUIModel, modifier: Modifier = Modifier) {
        ConstraintLayout(
            modifier = modifier
                .padding(start = 20.dp, end = 20.dp, top = 15.dp, bottom = 15.dp)
        ) {
            val (postContainer, featuredImage, title, url) = createRefs()
            Box(modifier = Modifier
                .constrainAs(postContainer) {
                    top.linkTo(parent.top, 0.dp)
                    bottom.linkTo(parent.bottom, 0.dp)
                    start.linkTo(parent.start, 0.dp)
                    end.linkTo(parent.end, 0.dp)
                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                }
                .background(color = AppColor.Gray60, shape = RoundedCornerShape(15.dp))
            )
            PostFeaturedImage(url = "", modifier = Modifier
                .constrainAs(featuredImage) {
                    top.linkTo(postContainer.top, 15.dp)
                    bottom.linkTo(postContainer.bottom, 15.dp)
                    end.linkTo(postContainer.end, 20.dp)
                }
                .width(80.dp)
                .height(80.dp))
            PostTitle(title = postUIModel.title, modifier = Modifier.constrainAs(title) {
                top.linkTo(postContainer.top, 15.dp)
                start.linkTo(postContainer.start, 20.dp)
                end.linkTo(featuredImage.start, margin = 20.dp)
                width = Dimension.fillToConstraints
                height = Dimension.wrapContent
            })
            PostUrl(url = postUIModel.url, modifier = Modifier.constrainAs(url) {
                top.linkTo(title.bottom, 5.dp)
                start.linkTo(postContainer.start, 20.dp)
                end.linkTo(featuredImage.start, margin = 20.dp)
                bottom.linkTo(postContainer.bottom, 15.dp)
                width = Dimension.fillToConstraints
                height = Dimension.wrapContent
            })
        }
    }

    // todo the logic for showing the featured image is not implemented yet
    @Composable
    private fun PostFeaturedImage(url: String, modifier: Modifier = Modifier) {
        val painter = rememberImagePainter(url) {
            placeholder(R.drawable.bg_rectangle_placeholder_globe_margin_8dp)
            error(R.drawable.bg_rectangle_placeholder_globe_margin_8dp)
            crossfade(true)
        }
        Image(
            painter = painter,
            contentDescription = stringResource(R.string.blavatar_desc),
            modifier = modifier
                .size(dimensionResource(R.dimen.jp_migration_site_icon_size))
                .clip(RoundedCornerShape(3.dp))
        )
    }


    @Composable
    private fun PostTitle(title: String, modifier: Modifier = Modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.body1,
            maxLines = 2,
            modifier = modifier
        )
    }

    @Composable
    private fun PostUrl(url: String, modifier: Modifier = Modifier) {
        Text(
            text = url,
            style = MaterialTheme.typography.body2,
            maxLines = 2,
            modifier = modifier
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
                .padding(start = 4.dp, top = 12.dp)
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
