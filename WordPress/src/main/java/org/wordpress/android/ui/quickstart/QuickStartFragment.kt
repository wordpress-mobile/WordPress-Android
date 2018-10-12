package org.wordpress.android.ui.quickstart

import android.app.Activity.RESULT_OK
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AlertDialog.Builder
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import kotlinx.android.synthetic.main.quick_start_fragment.*
import org.wordpress.android.R
import org.wordpress.android.R.style
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CHOOSE_THEME
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CREATE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CUSTOMIZE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.FOLLOW_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.PUBLISH_POST
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.SHARE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.VIEW_SITE
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.util.QuickStartUtils
import org.wordpress.android.viewmodel.quickstart.QuickStartViewModel
import javax.inject.Inject

class QuickStartFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: QuickStartViewModel

    private var skipAllTasksDialog: AlertDialog? = null

    companion object {
        private const val STATE_KEY_IS_SKIP_TASKS_DIALOG_VISIBLE = "is_skip_all_dialog_visible"

        fun newInstance(): QuickStartFragment {
            return QuickStartFragment()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(activity!!, viewModelFactory)
                .get<QuickStartViewModel>(QuickStartViewModel::class.java)

        viewModel.quickStartTaskStateStates.observe(this, Observer { quickStartModel ->
            var allTasksCompleted = true
            quickStartModel?.forEach {
                if (it.isTaskCompleted) {
                    crossOutTask(it.task)
                } else {
                    allTasksCompleted = false
                }
            }

            if (allTasksCompleted) {
                layout_list_complete.visibility = View.VISIBLE
                layout_skip_all.visibility = View.GONE
            }
        })

        viewModel.start(AppPrefs.getSelectedSite().toLong())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity?.application as WordPress).component()?.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.quick_start_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layout_view_site.setOnClickListener {
            startQuickStartTask(VIEW_SITE)
        }

        layout_browse_themes.setOnClickListener {
            startQuickStartTask(CHOOSE_THEME)
        }

        layout_customize_site.setOnClickListener {
            startQuickStartTask(CUSTOMIZE_SITE)
        }

        layout_share_site.setOnClickListener {
            startQuickStartTask(SHARE_SITE)
        }

        layout_publish_post.setOnClickListener {
            startQuickStartTask(PUBLISH_POST)
        }

        layout_follow_site.setOnClickListener {
            startQuickStartTask(FOLLOW_SITE)
        }

        button_skip_all.setOnClickListener {
            showSkipDialog()
        }

        if (savedInstanceState != null &&
                savedInstanceState.getBoolean(STATE_KEY_IS_SKIP_TASKS_DIALOG_VISIBLE, false)) {
            showSkipDialog()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_KEY_IS_SKIP_TASKS_DIALOG_VISIBLE,
                skipAllTasksDialog != null && skipAllTasksDialog!!.isShowing)
    }

    private fun startQuickStartTask(task: QuickStartTask) {
        AnalyticsTracker.track(QuickStartUtils.getQuickStartListTappedTracker(task))
        val intent = Intent()
        intent.putExtra(QuickStartActivity.ARG_QUICK_START_TASK, task)
        activity?.setResult(RESULT_OK, intent)
        activity?.finish()
    }

    private fun showSkipDialog() {
        skipAllTasksDialog = Builder(ContextThemeWrapper(activity, style.Calypso_Dialog))
                .setTitle(getString(R.string.quick_start_dialog_skip_title))
                .setMessage(getString(R.string.quick_start_dialog_skip_message))
                .setPositiveButton(getString(R.string.quick_start_button_skip_positive)) { _, _ ->
                    (view as ScrollView).smoothScrollTo(0, 0)
                    viewModel.skipAllTasks()
                    AnalyticsTracker.track(Stat.QUICK_START_LIST_SKIP_ALL_TAPPED)
                }
                .setNegativeButton(getString(R.string.quick_start_button_skip_negative)) { _, _ ->
                }
                .setCancelable(true)
                .show()
    }

    private fun crossOutTask(task: QuickStartTask) {
        val titleView: TextView
        val checkMarkView: View

        when (task) {
            CREATE_SITE -> {
                titleView = title_create_site
                checkMarkView = done_create_site
            }
            VIEW_SITE -> {
                titleView = title_view_site
                checkMarkView = done_view_site
            }
            CHOOSE_THEME -> {
                titleView = title_browse_themes
                checkMarkView = done_browse_themes
            }
            CUSTOMIZE_SITE -> {
                titleView = title_customize_site
                checkMarkView = done_customize_site
            }
            SHARE_SITE -> {
                titleView = title_share_site
                checkMarkView = done_share_site
            }
            PUBLISH_POST -> {
                titleView = title_publish_post
                checkMarkView = done_publish_post
            }
            FOLLOW_SITE -> {
                titleView = title_follow_site
                checkMarkView = done_follow_site
            }
        }

        visuallyMarkTaskAsCompleted(titleView, checkMarkView)
    }

    private fun visuallyMarkTaskAsCompleted(taskTitleTextView: TextView, taskDoneCheckMark: View) {
        taskTitleTextView.let { it.paintFlags = it.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG }
        taskDoneCheckMark.visibility = View.VISIBLE
    }
}
