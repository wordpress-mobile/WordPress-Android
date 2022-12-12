package org.wordpress.android.ui.jetpack.scan

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.ScanActivityBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredBottomSheetFragment
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.util.JetpackBrandingUtils.Screen.SCAN
import javax.inject.Inject

@AndroidEntryPoint
class ScanActivity : AppCompatActivity(), ScrollableViewInitializedListener {
    @Inject lateinit var jetpackBrandingUtils: JetpackBrandingUtils
    private var binding: ScanActivityBinding? = null

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        (supportFragmentManager.findFragmentById(R.id.fragment_container_view) as? ScanFragment)?.let {
            it.onNewIntent(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(ScanActivityBinding.inflate(layoutInflater)) {
            setContentView(root)
            binding = this
            setSupportActionBar(toolbarMain)
        }
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onScrollableViewInitialized(containerId: Int) {
        initJetpackBanner(containerId)
    }

    private fun initJetpackBanner(scrollableContainerId: Int) {
        if (jetpackBrandingUtils.shouldShowJetpackBrandingForPhaseOne()) {
            binding?.root?.post {
                val jetpackBannerView = binding?.jetpackBanner?.root ?: return@post
                val scrollableView = binding?.root?.findViewById<View>(scrollableContainerId) as? RecyclerView
                        ?: return@post

                jetpackBrandingUtils.showJetpackBannerIfScrolledToTop(jetpackBannerView, scrollableView)
                jetpackBrandingUtils.initJetpackBannerAnimation(jetpackBannerView, scrollableView)

                if (jetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()) {
                    binding?.jetpackBanner?.root?.setOnClickListener {
                        jetpackBrandingUtils.trackBannerTapped(SCAN)
                        JetpackPoweredBottomSheetFragment
                                .newInstance()
                                .show(supportFragmentManager, JetpackPoweredBottomSheetFragment.TAG)
                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        } else if (item.itemId == R.id.menu_scan_history) {
            // todo malinjir is it worth introducing a vm?
            ActivityLauncher.viewScanHistory(this, intent.getSerializableExtra(WordPress.SITE) as SiteModel)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.scan_menu, menu)
        return true
    }

    companion object {
        const val REQUEST_SCAN_STATE = "request_scan_state"
        const val REQUEST_FIX_STATE = "request_fix_state"
    }
}
