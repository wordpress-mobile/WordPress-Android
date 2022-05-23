package org.wordpress.android.ui.quickstart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import org.wordpress.android.R
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
import org.wordpress.android.databinding.QuickStartDialogFragmentBinding
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.CREATE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
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

class QuickStartFullScreenDialogFragment : Fragment(R.layout.quick_start_dialog_fragment),
        FullScreenDialogContent,
        OnQuickStartAdapterActionListener {
    private var dialogController: FullScreenDialogController? = null
    private var tasksType: QuickStartTaskType = QuickStartTaskType.CUSTOMIZE
    private lateinit var quickStartAdapter: QuickStartAdapter

    @Inject lateinit var quickStartTracker: QuickStartTracker
    @Inject lateinit var quickStartStore: QuickStartStore
    @Inject lateinit var selectedSiteRepository: SelectedSiteRepository

    private var _binding: QuickStartDialogFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = QuickStartDialogFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tasksType = arguments?.getSerializable(EXTRA_TYPE) as QuickStartTaskType? ?: QuickStartTaskType.UNKNOWN

        val tasksUncompleted: MutableList<QuickStartTask?> = ArrayList()
        val tasksCompleted: MutableList<QuickStartTask?> = ArrayList()
        val selectedSiteLocalId = selectedSiteRepository.getSelectedSiteLocalId()
        when (tasksType) {
            QuickStartTaskType.CUSTOMIZE -> {
                tasksUncompleted.addAll(
                        quickStartStore.getUncompletedTasksByType(
                                selectedSiteLocalId.toLong(),
                                QuickStartTaskType.CUSTOMIZE
                        )
                )
                tasksCompleted.addAll(
                        quickStartStore.getCompletedTasksByType(
                                selectedSiteLocalId.toLong(),
                                QuickStartTaskType.CUSTOMIZE
                        )
                )
                binding.setCompleteViewImage(R.drawable.img_illustration_site_brush_191dp)
                quickStartTracker.track(QUICK_START_TYPE_CUSTOMIZE_VIEWED)
            }
            QuickStartTaskType.GROW -> {
                tasksUncompleted.addAll(
                        quickStartStore.getUncompletedTasksByType(
                                selectedSiteLocalId.toLong(),
                                QuickStartTaskType.GROW
                        )
                )
                tasksCompleted.addAll(
                        quickStartStore.getCompletedTasksByType(
                                selectedSiteLocalId.toLong(),
                                QuickStartTaskType.GROW
                        )
                )
                binding.setCompleteViewImage(R.drawable.img_illustration_site_about_182dp)
                quickStartTracker.track(QUICK_START_TYPE_GROW_VIEWED)
            }
            QuickStartTaskType.GET_TO_KNOW_APP -> {
                tasksUncompleted
                        .addAll(
                                quickStartStore.getUncompletedTasksByType(
                                        selectedSiteLocalId.toLong(),
                                        QuickStartTaskType.GET_TO_KNOW_APP
                                )
                        )
                tasksCompleted.addAll(
                        quickStartStore.getCompletedTasksByType(
                                selectedSiteLocalId.toLong(),
                                QuickStartTaskType.GET_TO_KNOW_APP
                        )
                )
                binding.setCompleteViewImage(R.drawable.img_illustration_site_about_182dp)
                quickStartTracker.track(QUICK_START_TYPE_GET_TO_KNOW_APP_VIEWED)
            }
            QuickStartTaskType.UNKNOWN -> {
                tasksUncompleted.addAll(
                        quickStartStore.getUncompletedTasksByType(
                                selectedSiteLocalId.toLong(),
                                QuickStartTaskType.CUSTOMIZE
                        )
                )
                tasksCompleted.addAll(
                        quickStartStore.getCompletedTasksByType(
                                selectedSiteLocalId.toLong(),
                                QuickStartTaskType.CUSTOMIZE
                        )
                )
                binding.setCompleteViewImage(R.drawable.img_illustration_site_brush_191dp)
            }
        }
        val isCompletedTasksListExpanded = (savedInstanceState != null
                && savedInstanceState.getBoolean(KEY_COMPLETED_TASKS_LIST_EXPANDED))
        quickStartAdapter = QuickStartAdapter(
                tasksUncompleted,
                tasksCompleted,
                isCompletedTasksListExpanded
        )
        if (tasksUncompleted.isEmpty()) {
            binding.quickStartCompleteView.visibility = if (!isCompletedTasksListExpanded) View.VISIBLE else View.GONE
        }
        quickStartAdapter.setOnTaskTappedListener(this@QuickStartFullScreenDialogFragment)
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = quickStartAdapter
        // Disable default change animations to avoid blinking effect when adapter data is changed.
        (binding.list.itemAnimator as DefaultItemAnimator?)!!.supportsChangeAnimations = false
    }

    override fun setController(controller: FullScreenDialogController) {
        dialogController = controller
    }

    override fun onConfirmClicked(controller: FullScreenDialogController): Boolean {
        return true
    }

    override fun onDismissClicked(controller: FullScreenDialogController): Boolean {
        when (tasksType) {
            QuickStartTaskType.CUSTOMIZE -> quickStartTracker.track(QUICK_START_TYPE_CUSTOMIZE_DISMISSED)
            QuickStartTaskType.GROW -> quickStartTracker.track(QUICK_START_TYPE_GROW_DISMISSED)
            QuickStartTaskType.GET_TO_KNOW_APP -> quickStartTracker.track(QUICK_START_TYPE_GET_TO_KNOW_APP_DISMISSED)
            QuickStartTaskType.UNKNOWN -> {
            }
        }
        controller.dismiss()
        return true
    }

    override fun onTaskTapped(task: QuickStartTask) {
        quickStartTracker.track(getQuickStartListTappedTracker(task))
        if (!showSnackbarIfNeeded(task)) {
            val result = Bundle()
            result.putSerializable(RESULT_TASK, task as Serializable?)
            dialogController!!.confirm(result)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(
                KEY_COMPLETED_TASKS_LIST_EXPANDED,
                quickStartAdapter.isCompletedTasksListExpanded
        )
    }

    override fun onSkipTaskTapped(task: QuickStartTask) {
        quickStartTracker.track(getQuickStartListSkippedTracker(task))
        val selectedSiteLocalId = selectedSiteRepository.getSelectedSiteLocalId()
        quickStartStore.setDoneTask(selectedSiteLocalId.toLong(), task, true)

        val uncompletedTasks: List<QuickStartTask?> = quickStartStore.getUncompletedTasksByType(
                selectedSiteLocalId.toLong(),
                tasksType
        )
        quickStartAdapter.updateContent(
                uncompletedTasks,
                quickStartStore.getCompletedTasksByType(
                        selectedSiteLocalId.toLong(),
                        tasksType
                )
        )
        if (uncompletedTasks.isEmpty() && !quickStartAdapter.isCompletedTasksListExpanded) {
            binding.toggleCompletedView(true)
        }
    }

    override fun onCompletedTasksListToggled(isExpanded: Boolean) {
        when (tasksType) {
            QuickStartTaskType.CUSTOMIZE -> quickStartTracker.track(
                    if (isExpanded) QUICK_START_LIST_CUSTOMIZE_EXPANDED else QUICK_START_LIST_CUSTOMIZE_COLLAPSED
            )
            QuickStartTaskType.GROW -> quickStartTracker.track(
                    if (isExpanded) QUICK_START_LIST_GROW_EXPANDED else QUICK_START_LIST_GROW_COLLAPSED
            )
            QuickStartTaskType.GET_TO_KNOW_APP -> quickStartTracker.track(
                    if (isExpanded) QUICK_START_GET_TO_KNOW_APP_EXPANDED else QUICK_START_GET_TO_KNOW_APP_COLLAPSED
            )
            QuickStartTaskType.UNKNOWN -> {
            }
        }
        if (quickStartStore.getUncompletedTasksByType(
                        selectedSiteRepository.getSelectedSiteLocalId().toLong(),
                        tasksType
                ).isEmpty()) {
            binding.toggleCompletedView(!isExpanded)
        }
    }

    private fun QuickStartDialogFragmentBinding.setCompleteViewImage(imageResourceId: Int) {
        quickStartCompleteView.image.setImageResource(imageResourceId)
        quickStartCompleteView.image.visibility = View.VISIBLE
    }

    private fun QuickStartDialogFragmentBinding.toggleCompletedView(isVisible: Boolean) {
        if (isVisible) {
            AniUtils.fadeIn(quickStartCompleteView, SHORT)
        } else {
            AniUtils.fadeOut(quickStartCompleteView, SHORT)
        }
    }

    private fun showSnackbarIfNeeded(task: QuickStartTask?): Boolean {
        return if (task === CREATE_SITE) {
            make(
                    requireView(),
                    R.string.quick_start_list_create_site_message,
                    Snackbar.LENGTH_LONG
            ).show()
            true
        } else {
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
