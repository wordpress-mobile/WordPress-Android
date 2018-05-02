package org.wordpress.android.ui.activitylog

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import kotlinx.android.synthetic.main.toolbar.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel
import javax.inject.Inject

class   ActivityLogListActivity : AppCompatActivity() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ActivityLogViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (application as WordPress).component().inject(this)
        setContentView(R.layout.activity_log_activity)

        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ActivityLogViewModel::class.java)

        viewModel.site = if (savedInstanceState == null) {
            intent.getSerializableExtra(WordPress.SITE) as SiteModel
        } else {
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        }

        viewModel.start()
        showListFragment()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putSerializable(WordPress.SITE, viewModel.site)
        super.onSaveInstanceState(outState)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showListFragment() {
        var listFragment = supportFragmentManager.findFragmentByTag(ActivityLogListFragment.TAG)
        if (listFragment == null) {
            listFragment = ActivityLogListFragment.newInstance(viewModel.site)
        }
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, listFragment, ActivityLogListFragment.TAG)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit()
    }
}
