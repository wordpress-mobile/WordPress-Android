package org.wordpress.android.ui.main

import android.app.Activity
import android.content.Context
import android.graphics.Paint
import android.os.Bundle
import android.support.v7.app.AlertDialog.Builder
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatButton
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.R.style
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CHOOSE_THEME
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CREATE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CUSTOMIZE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.FOLLOW_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.PUBLISH_POST
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.SHARE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.VIEW_SITE
import org.wordpress.android.ui.prefs.AppPrefs.getSelectedSite
import org.wordpress.android.util.LocaleManager
import javax.inject.Inject

class QuickStartActivity : AppCompatActivity() {
    @Inject lateinit var mQuickStartStore: QuickStartStore

    private val mSite: Int = getSelectedSite()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.setLocale(newBase))
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component()?.inject(this)
        setContentView(R.layout.quick_start_activity)
        checkTasksCompleted()
        setTasksClickListeners()
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            setResult(Activity.RESULT_OK)
            finish()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun checkTasksCompleted() {
        if (mQuickStartStore.hasDoneTask(mSite.toLong(), CREATE_SITE) &&
                mQuickStartStore.hasDoneTask(mSite.toLong(), VIEW_SITE) &&
                mQuickStartStore.hasDoneTask(mSite.toLong(), CHOOSE_THEME) &&
                mQuickStartStore.hasDoneTask(mSite.toLong(), CUSTOMIZE_SITE) &&
                mQuickStartStore.hasDoneTask(mSite.toLong(), SHARE_SITE) &&
                mQuickStartStore.hasDoneTask(mSite.toLong(), PUBLISH_POST) &&
                mQuickStartStore.hasDoneTask(mSite.toLong(), FOLLOW_SITE)) {
            mQuickStartStore.setDoneTask(mSite.toLong(), CREATE_SITE, false)
            setTasksCompleted()
        } else {
            setTasksDone()
        }
    }

    private fun setTasksClickListeners() {
        findViewById<RelativeLayout>(R.id.layout_create_site).setOnClickListener {
            mQuickStartStore.setDoneTask(mSite.toLong(), CREATE_SITE, true)
            checkTasksCompleted()
        }

        findViewById<RelativeLayout>(R.id.layout_view_site).setOnClickListener {
            mQuickStartStore.setDoneTask(mSite.toLong(), VIEW_SITE, true)
            checkTasksCompleted()
        }

        findViewById<RelativeLayout>(R.id.layout_browse_themes).setOnClickListener {
            mQuickStartStore.setDoneTask(mSite.toLong(), CHOOSE_THEME, true)
            checkTasksCompleted()
        }

        findViewById<RelativeLayout>(R.id.layout_customize_site).setOnClickListener {
            mQuickStartStore.setDoneTask(mSite.toLong(), CUSTOMIZE_SITE, true)
            checkTasksCompleted()
        }

        findViewById<RelativeLayout>(R.id.layout_share_site).setOnClickListener {
            mQuickStartStore.setDoneTask(mSite.toLong(), SHARE_SITE, true)
            checkTasksCompleted()
        }

        findViewById<RelativeLayout>(R.id.layout_publish_post).setOnClickListener {
            mQuickStartStore.setDoneTask(mSite.toLong(), PUBLISH_POST, true)
            checkTasksCompleted()
        }

        findViewById<RelativeLayout>(R.id.layout_follow_site).setOnClickListener {
            mQuickStartStore.setDoneTask(mSite.toLong(), FOLLOW_SITE, true)
            checkTasksCompleted()
        }

        findViewById<AppCompatButton>(R.id.button_skip_all).setOnClickListener {
            showSkipDialog()
        }
    }

    private fun setTasksCompleted() {
        val titleCreateSite = findViewById<TextView>(R.id.title_create_site)
        titleCreateSite.paintFlags = titleCreateSite.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        val doneCreateSite = findViewById<ImageView>(R.id.done_create_site)
        doneCreateSite.visibility = View.VISIBLE

        val titleViewSite = findViewById<TextView>(R.id.title_view_site)
        titleViewSite.paintFlags = titleViewSite.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        findViewById<ImageView>(R.id.done_view_site).visibility = View.VISIBLE

        val titleBrowseThemes = findViewById<TextView>(R.id.title_browse_themes)
        titleBrowseThemes.paintFlags = titleBrowseThemes.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        findViewById<ImageView>(R.id.done_browse_themes).visibility = View.VISIBLE

        val titleCustomizeSite = findViewById<TextView>(R.id.title_customize_site)
        titleCustomizeSite.paintFlags = titleCustomizeSite.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        findViewById<ImageView>(R.id.done_customize_site).visibility = View.VISIBLE

        val titleAddSocial = findViewById<TextView>(R.id.title_share_site)
        titleAddSocial.paintFlags = titleAddSocial.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        findViewById<ImageView>(R.id.done_share_site).visibility = View.VISIBLE

        val titlePublishPost = findViewById<TextView>(R.id.title_publish_post)
        titlePublishPost.paintFlags = titlePublishPost.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        findViewById<ImageView>(R.id.done_publish_post).visibility = View.VISIBLE

        val titleFollowSite = findViewById<TextView>(R.id.title_follow_site)
        titleFollowSite.paintFlags = titleFollowSite.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        findViewById<ImageView>(R.id.done_follow_site).visibility = View.VISIBLE

        val layoutListComplete = findViewById<LinearLayout>(R.id.layout_list_complete)
        layoutListComplete.visibility = View.VISIBLE
        findViewById<RelativeLayout>(R.id.layout_skip_all).visibility = View.GONE
    }

    private fun setTasksDone() {
        if (mQuickStartStore.hasDoneTask(mSite.toLong(), CREATE_SITE)) {
            val titleCreateSite = findViewById<TextView>(R.id.title_create_site)
            titleCreateSite.paintFlags = titleCreateSite.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            findViewById<ImageView>(R.id.done_create_site).visibility = View.VISIBLE
        }

        if (mQuickStartStore.hasDoneTask(mSite.toLong(), VIEW_SITE)) {
            val titleViewSite = findViewById<TextView>(R.id.title_view_site)
            titleViewSite.paintFlags = titleViewSite.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            findViewById<ImageView>(R.id.done_view_site).visibility = View.VISIBLE
        }

        if (mQuickStartStore.hasDoneTask(mSite.toLong(), CHOOSE_THEME)) {
            val titleBrowseThemes = findViewById<TextView>(R.id.title_browse_themes)
            titleBrowseThemes.paintFlags = titleBrowseThemes.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            findViewById<ImageView>(R.id.done_browse_themes).visibility = View.VISIBLE
        }

        if (mQuickStartStore.hasDoneTask(mSite.toLong(), CUSTOMIZE_SITE)) {
            val titleCustomizeSite = findViewById<TextView>(R.id.title_customize_site)
            titleCustomizeSite.paintFlags = titleCustomizeSite.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            findViewById<ImageView>(R.id.done_customize_site).visibility = View.VISIBLE
        }

        if (mQuickStartStore.hasDoneTask(mSite.toLong(), SHARE_SITE)) {
            val titleAddSocial = findViewById<TextView>(R.id.title_share_site)
            titleAddSocial.paintFlags = titleAddSocial.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            findViewById<ImageView>(R.id.done_share_site).visibility = View.VISIBLE
        }

        if (mQuickStartStore.hasDoneTask(mSite.toLong(), PUBLISH_POST)) {
            val titlePublishPost = findViewById<TextView>(R.id.title_publish_post)
            titlePublishPost.paintFlags = titlePublishPost.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            findViewById<ImageView>(R.id.done_publish_post).visibility = View.VISIBLE
        }

        if (mQuickStartStore.hasDoneTask(mSite.toLong(), FOLLOW_SITE)) {
            val titleFollowSite = findViewById<TextView>(R.id.title_follow_site)
            titleFollowSite.paintFlags = titleFollowSite.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            findViewById<ImageView>(R.id.done_follow_site).visibility = View.VISIBLE
        }
    }

    private fun setTasksSkip() {
        mQuickStartStore.setDoneTask(mSite.toLong(), CREATE_SITE, false)
        mQuickStartStore.setDoneTask(mSite.toLong(), VIEW_SITE, true)
        mQuickStartStore.setDoneTask(mSite.toLong(), CHOOSE_THEME, true)
        mQuickStartStore.setDoneTask(mSite.toLong(), CUSTOMIZE_SITE, true)
        mQuickStartStore.setDoneTask(mSite.toLong(), SHARE_SITE, true)
        mQuickStartStore.setDoneTask(mSite.toLong(), PUBLISH_POST, true)
        mQuickStartStore.setDoneTask(mSite.toLong(), FOLLOW_SITE, true)

        val titleCreateSite = findViewById<TextView>(R.id.title_create_site)
        titleCreateSite.paintFlags = titleCreateSite.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        findViewById<ImageView>(R.id.done_create_site).visibility = View.VISIBLE

        val titleViewSite = findViewById<TextView>(R.id.title_view_site)
        titleViewSite.paintFlags = titleViewSite.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        findViewById<ImageView>(R.id.done_view_site).visibility = View.VISIBLE

        val titleBrowseThemes = findViewById<TextView>(R.id.title_browse_themes)
        titleBrowseThemes.paintFlags = titleBrowseThemes.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        findViewById<ImageView>(R.id.done_browse_themes).visibility = View.VISIBLE

        val titleCustomizeSite = findViewById<TextView>(R.id.title_customize_site)
        titleCustomizeSite.paintFlags = titleCustomizeSite.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        findViewById<ImageView>(R.id.done_customize_site).visibility = View.VISIBLE

        val titleAddSocial = findViewById<TextView>(R.id.title_share_site)
        titleAddSocial.paintFlags = titleAddSocial.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        findViewById<ImageView>(R.id.done_share_site).visibility = View.VISIBLE

        val titlePublishPost = findViewById<TextView>(R.id.title_publish_post)
        titlePublishPost.paintFlags = titlePublishPost.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        findViewById<ImageView>(R.id.done_publish_post).visibility = View.VISIBLE

        val titleFollowSite = findViewById<TextView>(R.id.title_follow_site)
        titleFollowSite.paintFlags = titleFollowSite.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        findViewById<ImageView>(R.id.done_follow_site).visibility = View.VISIBLE

        findViewById<RelativeLayout>(R.id.layout_skip_all).visibility = View.GONE
    }

    private fun showSkipDialog() {
        Builder(ContextThemeWrapper(this, style.Calypso_Dialog))
                .setTitle(getString(R.string.quick_start_dialog_skip_title))
                .setMessage(getString(R.string.quick_start_dialog_skip_message))
                .setPositiveButton(getString(R.string.quick_start_button_skip_positive)) { _, _ ->
                    setTasksSkip()
                    finish()
                }
                .setNegativeButton(getString(R.string.quick_start_button_skip_negative)) { _, _ ->
                }
                .setCancelable(true)
                .show()
    }
}
