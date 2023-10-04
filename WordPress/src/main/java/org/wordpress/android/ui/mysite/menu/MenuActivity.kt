package org.wordpress.android.ui.mysite.menu

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.ActivityNavigator
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.utils.LocaleAwareComposable
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.LocaleManager
import javax.inject.Inject

@AndroidEntryPoint
class MenuActivity : AppCompatActivity() {
    @Inject
    lateinit var activityNavigator: ActivityNavigator
    private val viewModel: MenuViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initObservers()
        setContent {
            AppTheme {
                val userLanguage by viewModel.refreshAppLanguage.observeAsState("")

                LocaleAwareComposable(
                    locale = LocaleManager.languageLocale(userLanguage),
                    onLocaleChange = viewModel::setAppLanguage
                ) {
                    viewModel.start()
                    MenuScreen()
                }
            }
        }
    }

    private fun initObservers() {
        viewModel.navigation.observe(this) { handleNavigationAction(it.getContentIfNotHandled()) }
    }


    @Suppress("ComplexMethod", "LongMethod")
    private fun handleNavigationAction(action: SiteNavigationAction?) {
        when (action) {
            is SiteNavigationAction.OpenActivityLog -> ActivityLauncher.viewActivityLogList(this, action.site)
            is SiteNavigationAction.OpenBackup -> ActivityLauncher.viewBackupList(this, action.site)
            is SiteNavigationAction.OpenScan -> ActivityLauncher.viewScan(this, action.site)
            is SiteNavigationAction.OpenPlan -> ActivityLauncher.viewBlogPlans(this, action.site)
            is SiteNavigationAction.OpenPosts -> ActivityLauncher.viewCurrentBlogPosts(this, action.site)
            is SiteNavigationAction.OpenPages -> ActivityLauncher.viewCurrentBlogPages(this, action.site)
            is SiteNavigationAction.OpenAdmin -> ActivityLauncher.viewBlogAdmin(this, action.site)
            is SiteNavigationAction.OpenPeople -> ActivityLauncher.viewCurrentBlogPeople(this, action.site)
            is SiteNavigationAction.OpenSharing -> ActivityLauncher.viewBlogSharing(this, action.site)
            is SiteNavigationAction.OpenSiteSettings -> ActivityLauncher.viewBlogSettingsForResult(this, action.site)
            is SiteNavigationAction.OpenThemes -> ActivityLauncher.viewCurrentBlogThemes(this, action.site)
            is SiteNavigationAction.OpenPlugins -> ActivityLauncher.viewPluginBrowser(this, action.site)
            is SiteNavigationAction.OpenMedia -> ActivityLauncher.viewCurrentBlogMedia(this, action.site)
            is SiteNavigationAction.OpenMeScreen -> ActivityLauncher.viewMeActivityForResult(this)
            is SiteNavigationAction.OpenUnifiedComments -> ActivityLauncher.viewUnifiedComments(this, action.site)
            is SiteNavigationAction.OpenStats -> ActivityLauncher.viewBlogStats(this, action.site)
            is SiteNavigationAction.OpenDomains -> ActivityLauncher.viewDomainsDashboardActivity(
                this,
                action.site
            )
            is SiteNavigationAction.OpenCampaignListingPage -> activityNavigator.navigateToCampaignListingPage(
                this,
                action.campaignListingPageSource
            )
            else -> {}
        }
    }

    @Composable
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    fun MenuScreen(modifier: Modifier = Modifier) {
        Scaffold(
            topBar = {
                MainTopAppBar(
                    title = stringResource(id = R.string.my_site_section_screen_title),
                    navigationIcon = NavigationIcons.BackIcon,
                    onNavigationIconClick = onBackPressedDispatcher::onBackPressed,
                )
            },
            content = {
                MenuContent(modifier = modifier)
            }
        )
    }

    @Composable
    fun MenuContent(modifier: Modifier = Modifier) {
        val uiState by viewModel.uiState.collectAsState()

        LazyColumn(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(uiState.items) { viewState ->
                when (viewState) {
                    is MenuItemState.MenuListItem-> MySiteListItem(viewState)
                    is MenuItemState.MenuHeaderItem -> MySiteListItemHeader(viewState)
                    is MenuItemState.MenuEmptyHeaderItem -> MySiteListItemEmptyHeader()
                }
            }
        }
    }
}
@Composable
fun MySiteListItemHeader(headerItem: MenuItemState.MenuHeaderItem) {
    Text(
        text = uiStringText(headerItem.title),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
        modifier = Modifier
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun MySiteListItemEmptyHeader() {
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
fun MySiteListItem(item: MenuItemState.MenuListItem, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize()
    )
    {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize()
                .clickable { item.onClick.click() }
                .padding(start = 12.dp, top = 6.dp, end = 16.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            content = {
            Image(
                painter = painterResource(id = item.primaryIcon),
                contentDescription = null, // Add appropriate content description
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(24.dp)
                    .padding(1.dp),
                colorFilter =
                if (item.disablePrimaryIconTint) null else ColorFilter.tint(MaterialTheme.colors.onSurface)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = uiStringText(item.primaryText),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp),
            )
            Spacer(modifier = Modifier
                .height(4.dp)
                .weight(1f))

            if (item.secondaryText != null) {
                Text(
                    text = uiStringText(item.secondaryText),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp, end = 8.dp),
                )
            }

            if (item.secondaryIcon != null) {
                Image(
                    painter = painterResource(id = item.secondaryIcon),
                    contentDescription = null, // Add appropriate content description
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(1.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colors.onSurface)
                )
            }

            if (item.showFocusPoint) CustomXMLWidgetView()
        })
    }
}
@Composable
fun CustomXMLWidgetView(modifier: Modifier = Modifier) {
    // Load the custom XML widget using AndroidView
    var customView: View? by remember { mutableStateOf(null) }
    val context = LocalContext.current

    DisposableEffect(context) {
        // Perform the side effect (inflate view) when the composable is composed
        customView = FrameLayout(context).apply {
            addView(LayoutInflater.from(context).inflate(R.layout.quick_start_focus_point, this, false))
        }

        onDispose {
            customView = null
        }
    }
    customView?.let { view ->
        AndroidView(
            factory =  { view },
            modifier = modifier.wrapContentSize(Alignment.Center)
        )
    }
}


@Preview
@Composable
fun MySiteListItemPreviewBase() {
    val onClick = remember { {} }
    MySiteListItem(
        MenuItemState.MenuListItem(
            primaryIcon = R.drawable.ic_posts_white_24dp,
            primaryText = UiString.UiStringText("Blog Posts"),
            secondaryIcon = null,
            secondaryText = null,
            showFocusPoint = false,
            onClick = ListItemInteraction.create { onClick() },
            listItemAction = ListItemAction.POSTS)
    )
}

@Preview
@Composable
fun MySiteListItemPreviewWithFocusPoint() {
    val onClick = remember { {} }
    MySiteListItem(
        MenuItemState.MenuListItem(
            primaryIcon = R.drawable.ic_posts_white_24dp,
            primaryText = UiString.UiStringText("Blog Posts"),
            secondaryIcon = null,
            secondaryText = null,
            showFocusPoint = true,
            onClick = ListItemInteraction.create { onClick() },
            listItemAction = ListItemAction.POSTS)
    )
}

@Preview
@Composable
fun MySiteListItemPreviewWithSecondaryText() {
    val onClick = remember { {} }
    MySiteListItem(
        MenuItemState.MenuListItem(
            primaryIcon = R.drawable.ic_posts_white_24dp,
            primaryText = UiString.UiStringText("Plans"),
            secondaryIcon = null,
            secondaryText = UiString.UiStringText("Basic"),
            showFocusPoint = false,
            onClick = ListItemInteraction.create { onClick() },
            listItemAction = ListItemAction.PLAN)
    )
}

@Preview
@Composable
fun MySiteListItemPreviewWithSecondaryImage() {
    val onClick = remember { {} }
    MySiteListItem(
        MenuItemState.MenuListItem(
            primaryIcon = R.drawable.ic_posts_white_24dp,
            primaryText = UiString.UiStringText("Plans"),
            secondaryIcon = R.drawable.ic_story_icon_24dp,
            secondaryText = null,
            showFocusPoint = false,
            onClick = ListItemInteraction.create { onClick() },
            listItemAction = ListItemAction.PLAN)
    )
}
