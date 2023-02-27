package org.wordpress.android.ui.blaze.ui.blazeoverlay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
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
import org.wordpress.android.ui.compose.components.Drawable
import org.wordpress.android.ui.compose.components.ImageButton
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.FontSize
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.utils.UiString

@Stable
private val darkModePrimaryButtonColor = Color(0xFF1C1C1E)

@Stable
private val lightModePostThumbnailBackground = Color(0xD000000)

@Stable
private val bulletedTextColor = Color(0xFF666666)

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
                val postModel by viewModel.promoteUiState.observeAsState(BlazeUiState.PromoteScreen.Site)
                BlazeOverlayScreen(postModel)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    @Composable
    fun BlazeOverlayScreen(
        postModelState: BlazeUiState.PromoteScreen,
        isDarkTheme: Boolean = isSystemInDarkTheme()
    ) {
        val post = when (postModelState) {
            is BlazeUiState.PromoteScreen.PromotePost -> postModelState.postUIModel
            else -> null
        }
        Scaffold(
            topBar = { OverlayTopBar(post) },
        ) {
            BlazeOverlayContent(post, isDarkTheme)
        }
    }

    @Composable
    fun OverlayTopBar(postUIModel: PostUIModel?, modifier: Modifier = Modifier) {
        postUIModel?.also {
            MainTopAppBar(
                title = null,
                navigationIcon = {},
                onNavigationIconClick = {},
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            modifier = modifier
                                .align(Alignment.Top)
                                .padding(end = 8.dp),
                            painter = painterResource(R.drawable.ic_close_white_24dp),
                            contentDescription = stringResource(
                                R.string.jetpack_full_plugin_install_onboarding_dismiss_button_content_description
                            ),
                        )
                    }
                }
            )
        }?: MainTopAppBar(
                title = stringResource(R.string.blaze_activity_title),
                navigationIcon = NavigationIcons.BackIcon,
                onNavigationIconClick = {}
        )
    }

    @Composable
    private fun BlazeOverlayContent(
        post: PostUIModel?,
        isDarkTheme: Boolean
    ) {
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
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Margin.ExtraExtraMediumLarge.value)
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
                    it,
                    isInDarkTheme = isDarkTheme,
                    modifier = Modifier
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
                        getPrimaryButtonColor(isDarkTheme),
                        shape = RoundedCornerShape(15.dp)
                    ),
                buttonText = UiString.UiStringRes(getPrimaryButtonText(post)),
                drawableRight = Drawable(R.drawable.ic_promote_with_blaze),
                onClick = { viewModel.onPromoteWithBlazeClicked() },
            )
        }
    }

    private fun getPrimaryButtonColor(isInDarkTheme: Boolean): Color {
        return if (isInDarkTheme) darkModePrimaryButtonColor
        else AppColor.Black
    }

    private fun getPrimaryButtonText(postUIModel: PostUIModel?): Int {
        return postUIModel?.let { R.string.blaze_overlay_post_promotional_button }
            ?: R.string.blaze_overlay_site_promotional_button
    }

    @Composable
    fun PostThumbnailView(postUIModel: PostUIModel, modifier: Modifier = Modifier, isInDarkTheme: Boolean) {
        ConstraintLayout(
            modifier = modifier
                .padding(horizontal = 20.dp, vertical = 15.dp)
        ) {
            val (postContainer, featuredImage, title) = createRefs()
            Box(modifier = Modifier
                .constrainAs(postContainer) {
                    top.linkTo(parent.top, 0.dp)
                    bottom.linkTo(parent.bottom, 0.dp)
                    start.linkTo(parent.start, 0.dp)
                    end.linkTo(parent.end, 0.dp)
                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                }
                .background(color = getThumbnailBackground(isInDarkTheme), shape = RoundedCornerShape(15.dp))
            )
            PostFeaturedImage(url = postUIModel.featuredImageUrl, modifier = Modifier
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
                postUIModel.featuredImageUrl?.run {
                    end.linkTo(featuredImage.start, margin = 20.dp)
                } ?: run {
                    end.linkTo(postContainer.end, margin = 20.dp)
                }
                width = Dimension.fillToConstraints
                height = Dimension.wrapContent
            })
            val url = createRef()
            PostUrl(url = postUIModel.url, modifier = Modifier.constrainAs(url) {
                top.linkTo(title.bottom, 5.dp)
                start.linkTo(postContainer.start, 20.dp)
                postUIModel.featuredImageUrl?.run {
                    end.linkTo(featuredImage.start, margin = 20.dp)
                } ?: run {
                    end.linkTo(postContainer.end, margin = 20.dp)
                }
                bottom.linkTo(postContainer.bottom, 15.dp)
                width = Dimension.fillToConstraints
                height = Dimension.wrapContent
            })
        }
    }

    private fun getThumbnailBackground(isInDarkTheme: Boolean): Color {
        return if (isInDarkTheme) AppColor.DarkGray
        else lightModePostThumbnailBackground
    }

    @Composable
    private fun PostFeaturedImage(url: String?, modifier: Modifier = Modifier) {
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
            fontSize = FontSize.Large.value,
            fontWeight = FontWeight.Bold,
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
                color = bulletedTextColor
            )
        }
    }

    @Preview
    @Composable
    private fun PreviewBlazeOverlayScreenPostFlow() {
        AppTheme {
            BlazeOverlayScreen(
                postModelState = BlazeUiState.PromoteScreen.PromotePost(
                    PostUIModel(
                        postId = 119,
                        title = "Post title check if this is long enough to be truncated",
                        url = "https://www.google.long.ttiiitititititiit.com",
                        imageUrl = 0,
                        featuredImageUrl = null
                    )
                )
            )
        }
    }

    @Preview
    @Composable
    private fun PreviewBlazeOverlayScreenSiteFlow() {
        AppTheme {
            BlazeOverlayScreen(BlazeUiState.PromoteScreen.Site)
        }
    }
}
