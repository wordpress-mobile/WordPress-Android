package org.wordpress.android.ui.quickstart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import org.wordpress.android.R
import org.wordpress.android.R.drawable
import org.wordpress.android.R.layout
import org.wordpress.android.R.string
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_GET_TO_KNOW_APP_COLLAPSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_GET_TO_KNOW_APP_EXPANDED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_LIST_CUSTOMIZE_COLLAPSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_LIST_CUSTOMIZE_EXPANDED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_LIST_GROW_COLLAPSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_LIST_GROW_EXPANDED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_TYPE_CUSTOMIZE_DISMISSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_TYPE_CUSTOMIZE_VIEWED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_TYPE_GET_TO_KNOW_APP_DISMISSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_TYPE_GET_TO_KNOW_APP_VIEWED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_TYPE_GROW_DISMISSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_TYPE_GROW_VIEWED
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.CREATE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.ActionableEmptyView
import org.wordpress.android.ui.FullScreenDialogFragment.FullScreenDialogContent
import org.wordpress.android.ui.FullScreenDialogFragment.FullScreenDialogController
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.quickstart.QuickStartAdapter.OnQuickStartAdapterActionListener
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AniUtils.Duration.SHORT
import org.wordpress.android.util.QuickStartUtils.getQuickStartListSkippedTracker
import org.wordpress.android.util.QuickStartUtils.getQuickStartListTappedTracker
import org.wordpress.android.widgets.WPSnackbar.Companion.make
import java.io.Serializable
import javax.inject.Inject

class QuickStartFullScreenDialogFragment : Fragment(),
        FullScreenDialogContent,
        OnQuickStartAdapterActionListener {
    private var mDialogController: FullScreenDialogController? = null
    private var mQuickStartAdapter: QuickStartAdapter? = null
    private var mQuickStartCompleteView: ActionableEmptyView? = null
    private var mTasksType: QuickStartTaskType = QuickStartTaskType.CUSTOMIZE

    @Inject lateinit var mQuickStartTracker: QuickStartTracker
    @Inject lateinit var mQuickStartStore: QuickStartStore
    @Inject lateinit var mSelectedSiteRepository: SelectedSiteRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(
                layout.quick_start_dialog_fragment,
                container,
                false
        ) as ViewGroup

        mTasksType = arguments?.getSerializable(EXTRA_TYPE) as QuickStartTaskType? ?: QuickStartTaskType.UNKNOWN

        val list: RecyclerView = rootView.findViewById(R.id.list)
        val tasksUncompleted: MutableList<QuickStartTask?> = ArrayList()
        val tasksCompleted: MutableList<QuickStartTask?> = ArrayList()
        val selectedSiteLocalId = mSelectedSiteRepository.getSelectedSiteLocalId()
        mQuickStartCompleteView = rootView.findViewById(R.id.quick_start_complete_view)
        when (mTasksType) {
            QuickStartTaskType.CUSTOMIZE -> {
                tasksUncompleted.addAll(
                        mQuickStartStore.getUncompletedTasksByType(
                                selectedSiteLocalId.toLong(),
                                QuickStartTaskType.CUSTOMIZE
                        )
                )
                tasksCompleted.addAll(
                        mQuickStartStore.getCompletedTasksByType(
                                selectedSiteLocalId.toLong(),
                                QuickStartTaskType.CUSTOMIZE
                        )
                )
                setCompleteViewImage(drawable.img_illustration_site_brush_191dp)
                mQuickStartTracker.track(QUICK_START_TYPE_CUSTOMIZE_VIEWED)
            }
            QuickStartTaskType.GROW -> {
                tasksUncompleted.addAll(
                        mQuickStartStore.getUncompletedTasksByType(
                                selectedSiteLocalId.toLong(),
                                QuickStartTaskType.GROW
                        )
                )
                tasksCompleted.addAll(
                        mQuickStartStore.getCompletedTasksByType(
                                selectedSiteLocalId.toLong(),
                                QuickStartTaskType.GROW
                        )
                )
                setCompleteViewImage(drawable.img_illustration_site_about_182dp)
                mQuickStartTracker.track(QUICK_START_TYPE_GROW_VIEWED)
            }
            QuickStartTaskType.GET_TO_KNOW_APP -> {
                tasksUncompleted
                        .addAll(
                                mQuickStartStore.getUncompletedTasksByType(
                                        selectedSiteLocalId.toLong(),
                                        QuickStartTaskType.GET_TO_KNOW_APP
                                )
                        )
                tasksCompleted.addAll(
                        mQuickStartStore.getCompletedTasksByType(
                                selectedSiteLocalId.toLong(),
                                QuickStartTaskType.GET_TO_KNOW_APP
                        )
                )
                setCompleteViewImage(drawable.img_illustration_site_about_182dp)
                mQuickStartTracker.track(QUICK_START_TYPE_GET_TO_KNOW_APP_VIEWED)
            }
            QuickStartTaskType.UNKNOWN -> {
                tasksUncompleted.addAll(
                        mQuickStartStore.getUncompletedTasksByType(
                                selectedSiteLocalId.toLong(),
                                QuickStartTaskType.CUSTOMIZE
                        )
                )
                tasksCompleted.addAll(
                        mQuickStartStore.getCompletedTasksByType(
                                selectedSiteLocalId.toLong(),
                                QuickStartTaskType.CUSTOMIZE
                        )
                )
                setCompleteViewImage(drawable.img_illustration_site_brush_191dp)
            }
        }
        val isCompletedTasksListExpanded = (savedInstanceState != null
                && savedInstanceState.getBoolean(KEY_COMPLETED_TASKS_LIST_EXPANDED))
        mQuickStartAdapter = QuickStartAdapter(
                tasksUncompleted,
                tasksCompleted,
                isCompletedTasksListExpanded
        )
        if (tasksUncompleted.isEmpty()) {
            mQuickStartCompleteView?.visibility = if (!isCompletedTasksListExpanded) View.VISIBLE else View.GONE
        }
        mQuickStartAdapter!!.setOnTaskTappedListener(this@QuickStartFullScreenDialogFragment)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = mQuickStartAdapter
        // Disable default change animations to avoid blinking effect when adapter data is changed.
        (list.itemAnimator as DefaultItemAnimator?)!!.supportsChangeAnimations = false
        return rootView
    }

    override fun setController(controller: FullScreenDialogController) {
        mDialogController = controller
    }

    override fun onConfirmClicked(controller: FullScreenDialogController): Boolean {
        return true
    }

    override fun onDismissClicked(controller: FullScreenDialogController): Boolean {
        when (mTasksType) {
            QuickStartTaskType.CUSTOMIZE -> mQuickStartTracker.track(QUICK_START_TYPE_CUSTOMIZE_DISMISSED)
            QuickStartTaskType.GROW -> mQuickStartTracker.track(QUICK_START_TYPE_GROW_DISMISSED)
            QuickStartTaskType.GET_TO_KNOW_APP -> mQuickStartTracker.track(QUICK_START_TYPE_GET_TO_KNOW_APP_DISMISSED)
            QuickStartTaskType.UNKNOWN -> {
            }
        }
        controller.dismiss()
        return true
    }

    override fun onTaskTapped(task: QuickStartTask) {
        mQuickStartTracker.track(getQuickStartListTappedTracker(task))
        if (!showSnackbarIfNeeded(task)) {
            val result = Bundle()
            result.putSerializable(RESULT_TASK, task as Serializable?)
            mDialogController!!.confirm(result)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mQuickStartAdapter != null) {
            outState.putBoolean(
                    KEY_COMPLETED_TASKS_LIST_EXPANDED,
                    mQuickStartAdapter!!.isCompletedTasksListExpanded
            )
        }
    }

    override fun onSkipTaskTapped(task: QuickStartTask) {
        mQuickStartTracker.track(getQuickStartListSkippedTracker(task))
        val selectedSiteLocalId = mSelectedSiteRepository.getSelectedSiteLocalId()
        mQuickStartStore.setDoneTask(selectedSiteLocalId.toLong(), task, true)
        if (mQuickStartAdapter != null) {
            val uncompletedTasks: List<QuickStartTask?> = mQuickStartStore.getUncompletedTasksByType(
                    selectedSiteLocalId.toLong(),
                    mTasksType
            )
            mQuickStartAdapter!!.updateContent(
                    uncompletedTasks,
                    mQuickStartStore.getCompletedTasksByType(
                            selectedSiteLocalId.toLong(),
                            mTasksType
                    )
            )
            if (uncompletedTasks.isEmpty() && !mQuickStartAdapter!!.isCompletedTasksListExpanded) {
                toggleCompletedView(true)
            }
        }
    }

    override fun onCompletedTasksListToggled(isExpanded: Boolean) {
        when (mTasksType) {
            QuickStartTaskType.CUSTOMIZE -> mQuickStartTracker.track(
                    if (isExpanded) QUICK_START_LIST_CUSTOMIZE_EXPANDED else QUICK_START_LIST_CUSTOMIZE_COLLAPSED
            )
            QuickStartTaskType.GROW -> mQuickStartTracker.track(
                    if (isExpanded) QUICK_START_LIST_GROW_EXPANDED else QUICK_START_LIST_GROW_COLLAPSED
            )
            QuickStartTaskType.GET_TO_KNOW_APP -> mQuickStartTracker.track(
                    if (isExpanded) QUICK_START_GET_TO_KNOW_APP_EXPANDED else QUICK_START_GET_TO_KNOW_APP_COLLAPSED
            )
            QuickStartTaskType.UNKNOWN -> {
            }
        }
        if (mQuickStartStore.getUncompletedTasksByType(
                        mSelectedSiteRepository.getSelectedSiteLocalId().toLong(),
                        mTasksType
                ).isEmpty()) {
            toggleCompletedView(!isExpanded)
        }
    }

    private fun setCompleteViewImage(imageResourceId: Int) {
        mQuickStartCompleteView!!.image.setImageResource(imageResourceId)
        mQuickStartCompleteView!!.image.visibility = View.VISIBLE
    }

    private fun toggleCompletedView(isVisible: Boolean) {
        if (isVisible) {
            AniUtils.fadeIn(mQuickStartCompleteView, SHORT)
        } else {
            AniUtils.fadeOut(mQuickStartCompleteView, SHORT)
        }
    }

    private fun showSnackbarIfNeeded(task: QuickStartTask?): Boolean {
        return if (task === CREATE_SITE) {
            make(
                    requireView(),
                    string.quick_start_list_create_site_message,
                    Snackbar.LENGTH_LONG
            ).show()
            true
        } else {
            false
        }
    }

    companion object {
        const val KEY_COMPLETED_TASKS_LIST_EXPANDED = "completed_tasks_list_expanded"
        const val EXTRA_TYPE = "EXTRA_TYPE"
        const val RESULT_TASK = "RESULT_TASK"
        fun newBundle(type: QuickStartTaskType?): Bundle {
            val bundle = Bundle()
            bundle.putSerializable(EXTRA_TYPE, type)
            return bundle
        }
    }
}
