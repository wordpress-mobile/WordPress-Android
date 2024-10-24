package org.wordpress.android.ui.blaze.blazeoverlay

import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.blaze.BlazeUIModel
import org.wordpress.android.ui.blaze.BlazeUiState
import org.wordpress.android.ui.blaze.PageUIModel
import org.wordpress.android.ui.blaze.PostUIModel
import org.wordpress.android.ui.compose.components.buttons.Button
import org.wordpress.android.ui.compose.components.buttons.Drawable
import org.wordpress.android.ui.compose.components.buttons.ImageButton
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.compose.unit.FontSize
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.utils.UiString

@Stable
private val darkModePrimaryButtonColor = Color(0xFF1C1C1E)

@Stable
@SuppressLint("InvalidColorHexValue")
private val lightModePostThumbnailBackground = Color(0xD000000)

@Stable
@SuppressLint("InvalidColorHexValue")
private val darkModePostThumbnailBackground = Color(0xDFFFFFF)

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
            AppThemeM2 {
                val postModel by viewModel.promoteUiState.observeAsState(BlazeUiState.PromoteScreen.Site)
                BlazeOverlayScreen(postModel)
            }
        }
    }

    @Composable
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    fun BlazeOverlayScreen(
        content: BlazeUiState.PromoteScreen,
        isDarkTheme: Boolean = isSystemInDarkTheme()
    ) {
        val blazeUIModel = when (content) {
            is BlazeUiState.PromoteScreen.PromotePost -> content.postUIModel
            is BlazeUiState.PromoteScreen.PromotePage -> content.pagesUIModel
            else -> null
        }
        Scaffold(
            topBar = { OverlayTopBar(blazeUIModel) },
        ) { BlazeOverlayContent(blazeUIModel, isDarkTheme) }
    }

    @Composable
    fun OverlayTopBar(uiModel: BlazeUIModel?, modifier: Modifier = Modifier) {
        uiModel?.also {
            MainTopAppBar(
                title = null,
                navigationIcon = {},
                onNavigationIconClick = {},
                actions = {
                    IconButton(onClick = { viewModel.dismissOverlay() }) {
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
        } ?: MainTopAppBar(
            title = stringResource(R.string.blaze_activity_title),
            navigationIcon = NavigationIcons.BackIcon,
            onNavigationIconClick = { viewModel.dismissOverlay() }
        )
    }

    @Composable
    private fun BlazeOverlayContent(
        uiModel: BlazeUIModel?,
        isDarkTheme: Boolean
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Margin.ExtraLarge.value)
        ) {
            Image(
                painterResource(id = R.drawable.ic_blaze_overlay_image),
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
            uiModel?.let {
                PostThumbnailView(
                    it,
                    isInDarkTheme = isDarkTheme,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                )
            }
            Spacer(
                modifier = Modifier
                    .padding(top = 15.dp)
            )
            ImageButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(start = 20.dp, end = 20.dp)
                    .background(
                        getPrimaryButtonColor(isDarkTheme),
                        shape = RoundedCornerShape(15.dp)
                    ),
                button = Button(
                    text = UiString.UiStringRes(getPrimaryButtonText(uiModel)),
                    color = AppColor.White,
                    fontWeight = FontWeight.Normal,
                ),
                drawableRight = Drawable(R.drawable.ic_promote_with_blaze),
                onClick = { viewModel.onPromoteWithBlazeClicked() },
            )
        }
    }

    private fun getPrimaryButtonColor(isInDarkTheme: Boolean): Color {
        return if (isInDarkTheme) darkModePrimaryButtonColor
        else AppColor.Black
    }

    private fun getPrimaryButtonText(blazeUIModel: BlazeUIModel?): Int {
        return (blazeUIModel?.let { uiModel ->
            return when (uiModel) {
                is PostUIModel -> R.string.blaze_overlay_post_promotional_button
                is PageUIModel -> R.string.blaze_overlay_page_promotional_button
            }
        }?: R.string.blaze_overlay_site_promotional_button)
    }

    @Composable
    fun PostThumbnailView(uiModel: BlazeUIModel, modifier: Modifier = Modifier, isInDarkTheme: Boolean) {
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
            FeaturedImage(url = uiModel.featuredImageUrl, modifier = Modifier
                .constrainAs(featuredImage) {
                    top.linkTo(postContainer.top, 15.dp)
                    bottom.linkTo(postContainer.bottom, 15.dp)
                    end.linkTo(postContainer.end, 15.dp)
                })
            Title(
                title = uiModel.title, modifier = Modifier
                    .constrainAs(title) {
                        top.linkTo(postContainer.top, 15.dp)
                        start.linkTo(postContainer.start, 20.dp)
                        uiModel.featuredImageUrl?.run {
                            end.linkTo(featuredImage.start, margin = 15.dp)
                        } ?: run {
                            end.linkTo(postContainer.end, margin = 20.dp)
                        }
                        width = Dimension.fillToConstraints
                    }
                    .wrapContentHeight()
            )
            val url = createRef()
            Url(url = uiModel.url, modifier = Modifier
                .constrainAs(url) {
                    top.linkTo(title.bottom)
                    start.linkTo(postContainer.start, 20.dp)
                    uiModel.featuredImageUrl?.run {
                        end.linkTo(featuredImage.start, margin = 15.dp)
                    } ?: run {
                        end.linkTo(postContainer.end, margin = 20.dp)
                    }
                    width = Dimension.fillToConstraints
                    height = Dimension.wrapContent
                }
                .padding(bottom = 15.dp))
        }
    }

    private fun getThumbnailBackground(isInDarkTheme: Boolean): Color {
        return if (isInDarkTheme) darkModePostThumbnailBackground
        else lightModePostThumbnailBackground
    }

    @Composable
    private fun FeaturedImage(url: String?, modifier: Modifier = Modifier) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .crossfade(true)
                .build(),
            contentScale = ContentScale.Crop,
            contentDescription = stringResource(R.string.blavatar_desc),
            modifier = modifier
                .size(80.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }


    @Composable
    private fun Title(title: String, modifier: Modifier = Modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.body1,
            fontSize = FontSize.Large.value,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier.wrapContentHeight()
        )
    }

    @Composable
    private fun Url(url: String, modifier: Modifier = Modifier) {
        Text(
            text = url,
            style = MaterialTheme.typography.body2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
    }


    @Composable
    fun Subtitles(list: List<Int>, modifier: Modifier = Modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier.padding(
                top = Margin.ExtraLarge.value,
                bottom = Margin.ExtraLarge.value
            )
        ) {
            list.forEach { BulletedText(it) }
        }
    }

    @Composable
    fun BulletedText(stringResource: Int) {
        Row(horizontalArrangement = Arrangement.Start) {
            Canvas(modifier = Modifier
                .padding(start = 4.dp, top = 12.dp)
                .size(6.dp),
                onDraw = {
                    drawCircle(color = bulletedTextColor)
                })
            Text(
                modifier = Modifier.padding(start = Margin.ExtraLarge.value),
                text = stringResource(id = stringResource),
                fontSize = FontSize.Large.value,
                color = bulletedTextColor
            )
        }
    }

    @Preview
    @Composable
    private fun PreviewBlazeOverlayScreenPostFlow() {
        AppThemeM2 {
            BlazeOverlayScreen(
                content = BlazeUiState.PromoteScreen.PromotePost(
                    PostUIModel(
                        postId = 119,
                        title = "Post title long enough to be truncated and this is not just a test to see " +
                                "how it looks",
                        url = "www.google.long.ttiiitititititiit.com/blog./24/2021/05/12" +
                                "/this-is-a-test-post/trucncation is happeniding",
                        featuredImageId = 357,
                        featuredImageUrl = "https://ajeshrpai.in/wp-content/uploads/2023/02/wp-1677490974228.jpg"
                    )
                )
            )
        }
    }

    @Preview
    @Composable
    private fun PreviewBlazeOverlayScreenSiteFlow() {
        AppThemeM2 {
            BlazeOverlayScreen(BlazeUiState.PromoteScreen.Site)
        }
    }
}
