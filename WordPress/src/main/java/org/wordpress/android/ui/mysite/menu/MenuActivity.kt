package org.wordpress.android.ui.mysite.menu

import android.annotation.SuppressLint
import android.content.Intent
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
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.compose.utils.LocaleAwareComposable
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.SiteSettingsFragment
import org.wordpress.android.ui.quickstart.QuickStartMySitePrompts
import org.wordpress.android.ui.stats.refresh.utils.StatsLaunchedFrom
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.LocaleManager
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.extensions.getParcelableExtraCompat
import javax.inject.Inject

const val KEY_QUICK_START_EVENT = "key_quick_start_event"
@AndroidEntryPoint
class MenuActivity : AppCompatActivity() {
    @Inject
    lateinit var activityNavigator: ActivityNavigator

    @Inject
    lateinit var snackbarSequencer: SnackbarSequencer

    @Inject
    lateinit var quickStartUtils: QuickStartUtilsWrapper

    private val viewModel: MenuViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initObservers()
        setContent {
            AppThemeM2 {
                val userLanguage by viewModel.refreshAppLanguage.observeAsState("")

                LocaleAwareComposable(
                    locale = LocaleManager.languageLocale(userLanguage),
                    onLocaleChange = viewModel::setAppLanguage
                ) {
                    viewModel.start(intent.getParcelableExtraCompat(KEY_QUICK_START_EVENT))
                    MenuScreen()
                }
            }
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == SiteSettingsFragment.RESULT_BLOG_REMOVED) {
            viewModel.handleSiteRemoved()
        }
    }

    private fun initObservers() {
        viewModel.navigation.observe(this) { handleNavigationAction(it.getContentIfNotHandled()) }
        viewModel.onSnackbarMessage.observe(this) { showSnackbar(it.getContentIfNotHandled()) }
        viewModel.onQuickStartMySitePrompts.observe(this) { handleActiveTutorialPrompt(it.getContentIfNotHandled()) }
        viewModel.onSelectedSiteMissing.observe(this) { finish() }

        // Set the Compose callback for SnackbarSequencer
        snackbarSequencer.setComposeSnackbarCallback { item ->
            item?.let { viewModel.showSnackbarRequest(it) }
        }
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
            is SiteNavigationAction.OpenStats -> ActivityLauncher.viewBlogStats(
                this,
                action.site,
                StatsLaunchedFrom.ROW
            )

            is SiteNavigationAction.OpenDomains -> ActivityLauncher.viewDomainsDashboardActivity(
                this,
                action.site
            )
            is SiteNavigationAction.OpenCampaignListingPage -> activityNavigator.navigateToCampaignListingPage(
                this,
                action.campaignListingPageSource
            )
            is SiteNavigationAction.OpenSiteMonitoring -> activityNavigator.navigateToSiteMonitoring(this, action.site)
            else -> {}
        }
    }

    private fun showSnackbar(holder: SnackbarMessageHolder?) {
        holder?.let {
            snackbarSequencer.enqueue(
                SnackbarItem(
                    info = SnackbarItem.Info(
                        view = window.decorView.findViewById(android.R.id.content),
                        textRes = holder.message,
                        duration = holder.duration,
                        isImportant = holder.isImportant
                    ),
                    action = holder.buttonTitle?.let {
                        SnackbarItem.Action(
                            textRes = holder.buttonTitle,
                            clickListener = { holder.buttonAction() }
                        )
                    },
                    dismissCallback = { _, event -> holder.onDismissAction(event) }
                )
            )
        }
    }

    private fun handleActiveTutorialPrompt(activeTutorialPrompt: QuickStartMySitePrompts?) {
        activeTutorialPrompt?.let {
            val message = quickStartUtils.stylizeQuickStartPrompt(
                this,
                activeTutorialPrompt.shortMessagePrompt,
                activeTutorialPrompt.iconId
            )

            showSnackbar(SnackbarMessageHolder(UiString.UiStringText(message)))
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onStop() {
        snackbarSequencer.clearComposeSnackbarCallback()
        super.onStop()
    }

    @Composable
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    fun MenuScreen(modifier: Modifier = Modifier) {
        val scaffoldState = rememberScaffoldState()

        Scaffold(
            scaffoldState = scaffoldState,
            snackbarHost = { snackbarHostState ->
                SnackbarHost(hostState = snackbarHostState)
            },
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
        LaunchedEffect(viewModel.snackBar) {
            viewModel.snackBar.collect { message ->
                scaffoldState.snackbarHostState.showSnackbar(
                    message.message, message.actionLabel, message.duration)
            }
        }
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
            Spacer(Modifier.width(16.dp))
            Text(
                text = uiStringText(item.primaryText),
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.high),
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
            secondaryIcon = R.drawable.ic_pages_white_24dp,
            secondaryText = null,
            showFocusPoint = false,
            onClick = ListItemInteraction.create { onClick() },
            listItemAction = ListItemAction.PLAN)
    )
}
