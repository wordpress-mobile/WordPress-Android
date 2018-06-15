package org.wordpress.android.ui.main

import android.app.Activity
import android.content.Context
import android.graphics.Paint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatButton
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.ui.posts.BasicFragmentDialog
import org.wordpress.android.ui.prefs.AppPrefs.getSelectedSite
import org.wordpress.android.util.LocaleManager
import javax.inject.Inject

class QuickStartActivity : AppCompatActivity(), BasicFragmentDialog.BasicDialogPositiveClickInterface {
    @Inject lateinit var quickStartStore: QuickStartStore

    private val site: Int = getSelectedSite()
    private val skipAllTasksDialogTag = "skip_all_tasks_dialog"

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
        if (areAllTasksCompleted()) {
            setTasksCompleted()
        } else {
            setTasksDone()
        }
    }

    private fun areAllTasksCompleted(): Boolean {
        QuickStartTask.values().forEach {
            if (it != QuickStartTask.CREATE_SITE) { // CREATE_SITE is completed by default, regardless of DB flag
                if (!quickStartStore.hasDoneTask(site.toLong(), it)) {
                    return@areAllTasksCompleted false
                }
            }
        }
        return true
    }

    private fun setTasksClickListeners() {
        findViewById<RelativeLayout>(R.id.layout_view_site).setOnClickListener {
            quickStartStore.setDoneTask(site.toLong(), QuickStartTask.VIEW_SITE, true)
            checkTasksCompleted()
        }

        findViewById<RelativeLayout>(R.id.layout_browse_themes).setOnClickListener {
            quickStartStore.setDoneTask(site.toLong(), QuickStartTask.CHOOSE_THEME, true)
            checkTasksCompleted()
        }

        findViewById<RelativeLayout>(R.id.layout_customize_site).setOnClickListener {
            quickStartStore.setDoneTask(site.toLong(), QuickStartTask.CUSTOMIZE_SITE, true)
            checkTasksCompleted()
        }

        findViewById<RelativeLayout>(R.id.layout_share_site).setOnClickListener {
            quickStartStore.setDoneTask(site.toLong(), QuickStartTask.SHARE_SITE, true)
            checkTasksCompleted()
        }

        findViewById<RelativeLayout>(R.id.layout_publish_post).setOnClickListener {
            quickStartStore.setDoneTask(site.toLong(), QuickStartTask.PUBLISH_POST, true)
            checkTasksCompleted()
        }

        findViewById<RelativeLayout>(R.id.layout_follow_site).setOnClickListener {
            quickStartStore.setDoneTask(site.toLong(), QuickStartTask.FOLLOW_SITE, true)
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
        val titleCreateSite = findViewById<TextView>(R.id.title_create_site)
        titleCreateSite.paintFlags = titleCreateSite.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        findViewById<ImageView>(R.id.done_create_site).visibility = View.VISIBLE

        if (quickStartStore.hasDoneTask(site.toLong(), QuickStartTask.VIEW_SITE)) {
            val titleViewSite = findViewById<TextView>(R.id.title_view_site)
            titleViewSite.paintFlags = titleViewSite.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            findViewById<ImageView>(R.id.done_view_site).visibility = View.VISIBLE
        }

        if (quickStartStore.hasDoneTask(site.toLong(), QuickStartTask.CHOOSE_THEME)) {
            val titleBrowseThemes = findViewById<TextView>(R.id.title_browse_themes)
            titleBrowseThemes.paintFlags = titleBrowseThemes.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            findViewById<ImageView>(R.id.done_browse_themes).visibility = View.VISIBLE
        }

        if (quickStartStore.hasDoneTask(site.toLong(), QuickStartTask.CUSTOMIZE_SITE)) {
            val titleCustomizeSite = findViewById<TextView>(R.id.title_customize_site)
            titleCustomizeSite.paintFlags = titleCustomizeSite.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            findViewById<ImageView>(R.id.done_customize_site).visibility = View.VISIBLE
        }

        if (quickStartStore.hasDoneTask(site.toLong(), QuickStartTask.SHARE_SITE)) {
            val titleAddSocial = findViewById<TextView>(R.id.title_share_site)
            titleAddSocial.paintFlags = titleAddSocial.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            findViewById<ImageView>(R.id.done_share_site).visibility = View.VISIBLE
        }

        if (quickStartStore.hasDoneTask(site.toLong(), QuickStartTask.PUBLISH_POST)) {
            val titlePublishPost = findViewById<TextView>(R.id.title_publish_post)
            titlePublishPost.paintFlags = titlePublishPost.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            findViewById<ImageView>(R.id.done_publish_post).visibility = View.VISIBLE
        }

        if (quickStartStore.hasDoneTask(site.toLong(), QuickStartTask.FOLLOW_SITE)) {
            val titleFollowSite = findViewById<TextView>(R.id.title_follow_site)
            titleFollowSite.paintFlags = titleFollowSite.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            findViewById<ImageView>(R.id.done_follow_site).visibility = View.VISIBLE
        }
    }

    private fun setTasksSkip() {
        QuickStartTask.values().forEach { quickStartStore.setDoneTask(site.toLong(), it, true) }

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

        val layoutListComplete = findViewById<LinearLayout>(R.id.layout_list_complete)
        layoutListComplete.visibility = View.VISIBLE
        findViewById<RelativeLayout>(R.id.layout_skip_all).visibility = View.GONE
    }

    private fun showSkipDialog() {
        val basicFragmentDialog = BasicFragmentDialog()
        basicFragmentDialog.initialize(skipAllTasksDialogTag,
                getString(R.string.quick_start_dialog_skip_title),
                getString(R.string.quick_start_dialog_skip_message),
                getString(R.string.quick_start_button_skip_positive),
                null, getString(R.string.quick_start_button_skip_negative))

        basicFragmentDialog.show(supportFragmentManager, skipAllTasksDialogTag)
    }

    override fun onPositiveClicked(instanceTag: String) {
        findViewById<ScrollView>(R.id.checklist_scrollview).let {
            it.post {
                findViewById<ScrollView>(R.id.checklist_scrollview).smoothScrollTo(0, 0)
                setTasksSkip()
            }
        }
    }
}
