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
import org.wordpress.android.util.extensions.setVisible
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
        quickStartTracker.trackQuickStartListViewed(tasksType)
        val selectedSiteLocalId = selectedSiteRepository.getSelectedSiteLocalId().toLong()
        val tasksUncompleted = quickStartStore.getUncompletedTasksByType(selectedSiteLocalId, tasksType)
        val tasksCompleted = quickStartStore.getCompletedTasksByType(selectedSiteLocalId, tasksType)
        with(binding) {
            setupCompleteView(tasksUncompleted.isEmpty())
            setupQuickStartList(tasksUncompleted, tasksCompleted)
        }
    }

    private fun QuickStartDialogFragmentBinding.setupCompleteView(
        isTasksUncompletedEmpty: Boolean
    ) {
        val completeViewImage = when (tasksType) {
            QuickStartTaskType.CUSTOMIZE -> R.drawable.img_illustration_site_brush_191dp
            QuickStartTaskType.GROW -> R.drawable.img_illustration_site_about_182dp
            QuickStartTaskType.GET_TO_KNOW_APP -> R.drawable.img_illustration_site_about_182dp
            QuickStartTaskType.UNKNOWN -> R.drawable.img_illustration_site_brush_191dp
        }
        setCompleteViewImage(completeViewImage)
        quickStartCompleteView.setVisible(isTasksUncompletedEmpty)
    }

    private fun QuickStartDialogFragmentBinding.setupQuickStartList(
        tasksUncompleted: List<QuickStartTask>,
        tasksCompleted: List<QuickStartTask>
    ) {
        val tasks = QuickStartTask.getTasksByTaskType(tasksType).filterNot { it.taskType == QuickStartTaskType.UNKNOWN }
        quickStartAdapter = QuickStartAdapter(tasks, tasksUncompleted, tasksCompleted)
        quickStartAdapter.setOnTaskTappedListener(this@QuickStartFullScreenDialogFragment)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = quickStartAdapter
        // Disable default change animations to avoid blinking effect when adapter data is changed.
        (list.itemAnimator as DefaultItemAnimator?)?.supportsChangeAnimations = false
    }

    override fun setController(controller: FullScreenDialogController) {
        dialogController = controller
    }

    override fun onConfirmClicked(controller: FullScreenDialogController): Boolean {
        return true
    }

    override fun onDismissClicked(controller: FullScreenDialogController): Boolean {
        quickStartTracker.trackQuickStartListDismissed(tasksType)
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

    override fun onSkipTaskTapped(task: QuickStartTask) {
        quickStartTracker.track(getQuickStartListSkippedTracker(task))
        val selectedSiteLocalId = selectedSiteRepository.getSelectedSiteLocalId()
        quickStartStore.setDoneTask(selectedSiteLocalId.toLong(), task, true)

        val uncompletedTasks = quickStartStore.getUncompletedTasksByType(selectedSiteLocalId.toLong(), tasksType)
        val completedTasks = quickStartStore.getCompletedTasksByType(selectedSiteLocalId.toLong(), tasksType)

        quickStartAdapter.updateContent(uncompletedTasks, completedTasks)

        if (uncompletedTasks.isEmpty()) binding.toggleCompletedView(true)
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
        const val EXTRA_TYPE = "EXTRA_TYPE"
        const val RESULT_TASK = "RESULT_TASK"
        fun newBundle(type: QuickStartTaskType?): Bundle {
            val bundle = Bundle()
            bundle.putSerializable(EXTRA_TYPE, type)
            return bundle
        }
    }
}
