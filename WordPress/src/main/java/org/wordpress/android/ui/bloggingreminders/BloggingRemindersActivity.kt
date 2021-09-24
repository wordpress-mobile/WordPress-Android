package org.wordpress.android.ui.bloggingreminders

import android.os.Bundle
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.ActivityBloggingRemindersBinding
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.bloggingreminders.BloggingReminderUtils.observeBottomSheet
import javax.inject.Inject

class BloggingRemindersActivity : LocaleAwareActivity() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: BloggingRemindersViewModel
    private lateinit var binding: ActivityBloggingRemindersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)

        binding = ActivityBloggingRemindersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this, viewModelFactory).get(BloggingRemindersViewModel::class.java)

        observeBottomSheet(
                viewModel.isBottomSheetShowing,
                this,
                this,
                BLOGGING_REMINDERS_BOTTOM_SHEET_TAG, { this.supportFragmentManager })

        intent?.let {
            val siteId = intent.getLongExtra(EXTRA_SITE_ID, -1)
            if (siteId > -1) {
                viewModel.onSettingsItemClicked(siteId.toInt())
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            onBackPressed()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }


    companion object {
        private const val BLOGGING_REMINDERS_BOTTOM_SHEET_TAG = "BLOGGING_REMINDERS_BOTTOM_SHEET_TAG"
        const val EXTRA_SITE_ID = "site_id"
    }
}
