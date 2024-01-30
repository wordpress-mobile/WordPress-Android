package org.wordpress.android.ui.sitemonitor

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.SparseArray
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.util.extensions.getSerializableExtraCompat
import javax.inject.Inject

@AndroidEntryPoint
class SiteMonitorParentActivity: AppCompatActivity() {
    @Inject
    lateinit var siteMonitorUtils: SiteMonitorUtils

    private var savedStateSparseArray = SparseArray<Fragment.SavedState>()
    private var currentSelectItemId = 0

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        siteMonitorUtils.trackActivityLaunched()

        if (savedInstanceState != null) {
            savedStateSparseArray = savedInstanceState.getSparseParcelableArray(
                SAVED_STATE_CONTAINER_KEY
            )
                ?: savedStateSparseArray
            currentSelectItemId = savedInstanceState.getInt(SAVED_STATE_CURRENT_TAB_KEY)
        }
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    SiteMonitorScreen()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSparseParcelableArray(SAVED_STATE_CONTAINER_KEY, savedStateSparseArray)
        outState.putInt(SAVED_STATE_CURRENT_TAB_KEY, currentSelectItemId)
    }

    private fun getSite(): SiteModel {
        return requireNotNull(intent.getSerializableExtraCompat(WordPress.SITE)) as SiteModel
    }

    private fun getInitialTab(): SiteMonitorType {
        return intent?.getSerializableExtraCompat(ARG_SITE_MONITOR_TYPE_KEY) as SiteMonitorType?
            ?: SiteMonitorType.METRICS
    }

    companion object {
        const val ARG_SITE_MONITOR_TYPE_KEY = "ARG_SITE_MONITOR_TYPE_KEY"
        const val SAVED_STATE_CONTAINER_KEY = "ContainerKey"
        const val SAVED_STATE_CURRENT_TAB_KEY = "CurrentTabKey"
    }

    @Composable
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    fun SiteMonitorScreen(modifier: Modifier = Modifier) {
        var selectedTab by rememberSaveable { mutableStateOf(SiteMonitorTabItem.Metrics.route) }
        Scaffold(
            topBar = {
                MainTopAppBar(
                    title = stringResource(id = R.string.site_monitoring),
                    navigationIcon = NavigationIcons.BackIcon,
                    onNavigationIconClick = onBackPressedDispatcher::onBackPressed,
                )
            }
        ) { padding ->
            Column(modifier = modifier.padding(padding)) {
                SiteMonitorTabHeader { clickTab ->
                    selectedTab = clickTab
                }
                SiteMonitorTabNavigation(selectedTab) { selectedTab ->
                    val item = enumValues<SiteMonitorTabItem>().find {
                        it.route == selectedTab
                    } ?: initialItem(getInitialTab())

                    siteMonitorUtils.trackTabLoaded(item.siteMonitorType)

                    SiteMonitorFragmentContainer(
                        modifier = Modifier.fillMaxSize(),
                        commit = getCommitFunction(
                            SiteMonitorTabFragment.newInstance(item.urlTemplate, item.siteMonitorType, getSite()),
                            item.route
                        )
                    )
                }
            }
        }
    }

    private fun initialItem(type: SiteMonitorType): SiteMonitorTabItem {
        return enumValues<SiteMonitorTabItem>().find {
            it.siteMonitorType == type
        } ?: SiteMonitorTabItem.Metrics
    }

    private fun getCommitFunction(
        fragment : Fragment,
        tag: String
    ): FragmentTransaction.(containerId: Int) -> Unit =
        {
            saveAndRetrieveFragment(supportFragmentManager, it, fragment)
            replace(it, fragment, tag)
        }

    private fun saveAndRetrieveFragment(
        supportFragmentManager: FragmentManager,
        tabId: Int,
        fragment: Fragment
    ) {
        val currentFragment = supportFragmentManager.findFragmentById(currentSelectItemId)
        if (currentFragment != null) {
            savedStateSparseArray.put(
                currentSelectItemId,
                supportFragmentManager.saveFragmentInstanceState(currentFragment)
            )
        }
        currentSelectItemId = tabId
        fragment.setInitialSavedState(savedStateSparseArray[currentSelectItemId])
    }
}
