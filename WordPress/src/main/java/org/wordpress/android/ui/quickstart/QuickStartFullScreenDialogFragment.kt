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
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.UNKNOWN
import org.wordpress.android.ui.FullScreenDialogFragment.FullScreenDialogContent
import org.wordpress.android.ui.FullScreenDialogFragment.FullScreenDialogController
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardBuilder
import org.wordpress.android.ui.quickstart.QuickStartFullScreenDialogFragment.QuickStartListCard.QuickStartHeaderCard
import org.wordpress.android.ui.quickstart.QuickStartFullScreenDialogFragment.QuickStartListCard.QuickStartTaskCard
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.QuickStartUtils.getQuickStartListSkippedTracker
import org.wordpress.android.util.QuickStartUtils.getQuickStartListTappedTracker
import org.wordpress.android.widgets.WPSnackbar.Companion.make
import java.io.Serializable
import javax.inject.Inject

class QuickStartFullScreenDialogFragment : Fragment(R.layout.quick_start_dialog_fragment),
        FullScreenDialogContent {
    private var dialogController: FullScreenDialogController? = null
    private var tasksType: QuickStartTaskType = QuickStartTaskType.CUSTOMIZE
    private lateinit var quickStartAdapter: QuickStartAdapter

    @Inject lateinit var quickStartTracker: QuickStartTracker
    @Inject lateinit var quickStartStore: QuickStartStore
    @Inject lateinit var selectedSiteRepository: SelectedSiteRepository
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var quickStartCardBuilder: QuickStartCardBuilder
    @Inject lateinit var displayUtilsWrapper: DisplayUtilsWrapper

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
        binding.setupQuickStartList()
    }

    private fun QuickStartDialogFragmentBinding.setupQuickStartList() {
        quickStartAdapter = QuickStartAdapter(uiHelpers)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = quickStartAdapter
        // Disable default change animations to avoid blinking effect when adapter data is changed.
        (list.itemAnimator as DefaultItemAnimator?)?.supportsChangeAnimations = false
        updateQuickStartList()
    }

    private fun updateQuickStartList() {
        val quickStartList = mutableListOf<QuickStartListCard>().apply {
            add(buildHeaderCard())
            addAll(buildTaskCards())
        }.toList()
        quickStartAdapter.submitList(quickStartList)
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

    private fun onTaskTapped(task: QuickStartTask) {
        quickStartTracker.track(getQuickStartListTappedTracker(task))
        if (!showSnackbarIfNeeded(task)) {
            val result = Bundle()
            result.putSerializable(RESULT_TASK, task as Serializable?)
            dialogController?.confirm(result)
        }
    }

    private fun onSkipTaskTapped(task: QuickStartTask) {
        quickStartTracker.track(getQuickStartListSkippedTracker(task))
        val selectedSiteLocalId = selectedSiteRepository.getSelectedSiteLocalId().toLong()
        quickStartStore.setDoneTask(selectedSiteLocalId, task, true)
        updateQuickStartList()
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

    private fun buildHeaderCard() = QuickStartHeaderCard(
            title = UiStringRes(quickStartCardBuilder.getTitle(tasksType)),
            shouldShowHeaderImage = !displayUtilsWrapper.isPhoneLandscape()
    )

    private fun buildTaskCards(): List<QuickStartTaskCard> {
        val tasks = QuickStartTask.getTasksByTaskType(tasksType).filterNot { it.taskType == UNKNOWN }
        val selectedSiteLocalId = selectedSiteRepository.getSelectedSiteLocalId().toLong()
        val tasksCompleted = quickStartStore.getCompletedTasksByType(selectedSiteLocalId, tasksType)
        return tasks.mapToQuickStartTaskCard(tasksCompleted)
    }

    private fun List<QuickStartTask>.mapToQuickStartTaskCard(tasksCompleted: List<QuickStartTask>) = this.map {
        QuickStartTaskCard(
                task = it,
                isCompleted = tasksCompleted.contains(it),
                onTaskTapped = this@QuickStartFullScreenDialogFragment::onTaskTapped,
                onSkipTaskTapped = this@QuickStartFullScreenDialogFragment::onSkipTaskTapped
        )
    }

    sealed class QuickStartListCard {
        data class QuickStartHeaderCard(
            val title: UiString,
            val shouldShowHeaderImage: Boolean
        ) : QuickStartListCard()

        data class QuickStartTaskCard(
            val task: QuickStartTask,
            val isCompleted: Boolean,
            val onTaskTapped: (task: QuickStartTask) -> Unit,
            val onSkipTaskTapped: (task: QuickStartTask) -> Unit
        ) : QuickStartListCard()
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
