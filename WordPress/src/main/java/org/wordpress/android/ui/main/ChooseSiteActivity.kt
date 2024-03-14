package org.wordpress.android.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.databinding.SitePickerActivityBinding
import org.wordpress.android.ui.LocaleAwareActivity

@AndroidEntryPoint
class ChooseSiteActivity : LocaleAwareActivity() {
    private val viewModel: SiteViewModel by viewModels()
    private val adapter = ChooseSiteAdapter()
    private lateinit var binding: SitePickerActivityBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SitePickerActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarMain)

        binding.progress.isVisible = true
        setupRecycleView()

        viewModel.sites.observe(this) {
            binding.progress.isVisible = false
            binding.recyclerView.isVisible = it.isNotEmpty()
            binding.actionableEmptyView.isVisible = it.isEmpty()
            adapter.setSites(it)
        }
        viewModel.loadSites()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.choose_site, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_edit) {
            val currentMode = adapter.mode
            if (currentMode == ActionMode.Pin) {
                item.title = "Edit"
                adapter.setActionMode(ActionMode.None)
            } else {
                item.title = "Done"
                adapter.setActionMode(ActionMode.Pin)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupRecycleView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter.apply { onReload = { viewModel.loadSites() } }
        binding.recyclerView.scrollBarStyle = View.SCROLLBARS_OUTSIDE_OVERLAY
//        binding.recyclerView.itemAnimator =
//            if (mSitePickerMode != null && mSitePickerMode.isReblogMode()) DefaultItemAnimator() else null
//        binding.recyclerView.adapter = getAdapter()
        binding.actionableEmptyView.updateLayoutForSearch(true, 0)
        binding.recyclerView.setEmptyView(binding.actionableEmptyView)
    }

    companion object {
        fun start(context: Context) {
            Intent(context, ChooseSiteActivity::class.java)
                .let { context.startActivity(it) }
        }
    }
}
