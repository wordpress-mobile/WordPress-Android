package org.wordpress.android.ui.engagement

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.scan_list_threat_item.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.ActionableEmptyView
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.PreviewCommentInReader
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.PreviewPostInReader
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.PreviewSiteById
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.PreviewSiteByUrl
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.ReaderActivityLauncher
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.SnackbarItem.Action
import org.wordpress.android.util.SnackbarItem.Info
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.WPUrlUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class EngagedPeopleListFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var contextProvider: ContextProvider
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var snackbarSequencer: SnackbarSequencer
    @Inject lateinit var uiHelpers: UiHelpers

    private lateinit var viewModel: EngagedPeopleListViewModel
    private lateinit var recycler: RecyclerView
    private lateinit var loadingView: View
    private lateinit var rootView: View
    private lateinit var emptyView: ActionableEmptyView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        viewModel = ViewModelProvider(this, viewModelFactory).get(EngagedPeopleListViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.engaged_people_list_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rootView = view
        recycler = view.findViewById(R.id.recycler)
        loadingView = view.findViewById(R.id.loading_view)
        emptyView = view.findViewById(R.id.actionable_empty_view)

        val listScenario = requireArguments().getParcelable<ListScenario>(KEY_LIST_SCENARIO)

        val layoutManager = LinearLayoutManager(activity)

        savedInstanceState?.getParcelable<Parcelable>(KEY_LIST_STATE)?.let {
            layoutManager.onRestoreInstanceState(it)
        }

        recycler.layoutManager = layoutManager

        viewModel.uiState.observe(viewLifecycleOwner, { state ->
            if (!isAdded) return@observe

            loadingView.visibility = if (state.showLoading) View.VISIBLE else View.GONE

            setupAdapter(state.engageItemsList)

            if (state.showEmptyState) {
                uiHelpers.setTextOrHide(emptyView.title, state.emptyStateTitle)
                uiHelpers.setTextOrHide(emptyView.button, state.emptyStateButtonText)
                emptyView.button.setOnClickListener { state.emptyStateAction?.invoke() }

                emptyView.visibility = View.VISIBLE
            } else {
                emptyView.visibility = View.GONE
            }
        })

        viewModel.onNavigationEvent.observeEvent(viewLifecycleOwner, { event ->
            if (!isAdded) return@observeEvent

            val activity = requireActivity()
            if (activity.isFinishing) return@observeEvent

            when (event) {
                is PreviewSiteById -> {
                    ReaderActivityLauncher.showReaderBlogPreview(activity, event.siteId)
                }
                is PreviewSiteByUrl -> {
                    val url = event.siteUrl
                    openUrl(activity, url)
                }
                is PreviewCommentInReader -> {
                    ReaderActivityLauncher.showReaderComments(
                            activity,
                            event.siteId,
                            event.commentPostId,
                            event.postOrCommentId
                    )
                }
                is PreviewPostInReader -> {
                    ReaderActivityLauncher.showReaderPostDetail(activity, event.siteId, event.postId)
                }
            }
        })

        viewModel.onSnackbarMessage.observeEvent(viewLifecycleOwner, { messageHolder ->
            if (!isAdded || !lifecycle.currentState.isAtLeast(State.RESUMED)) return@observeEvent

            showSnackbar(messageHolder)
        })

        viewModel.start(listScenario!!)
    }

    private fun openUrl(context: Context, url: String) {
        if (WPUrlUtils.isWordPressCom(url)) {
            WPWebViewActivity.openUrlByUsingGlobalWPCOMCredentials(context, url)
        } else {
            WPWebViewActivity.openURL(context, url)
        }
    }

    private fun setupAdapter(items: List<EngageItem>) {
        val adapter = recycler.adapter as? EngagedPeopleAdapter ?: EngagedPeopleAdapter(
                imageManager,
                contextProvider.getContext()
        ).also {
            recycler.adapter = it
        }

        val recyclerViewState = recycler.layoutManager?.onSaveInstanceState()
        adapter.loadData(items)
        recycler.layoutManager?.onRestoreInstanceState(recyclerViewState)
    }

    private fun showSnackbar(holder: SnackbarMessageHolder) {
        snackbarSequencer.enqueue(
                SnackbarItem(
                        Info(
                                view = rootView,
                                textRes = holder.message,
                                duration = Snackbar.LENGTH_LONG
                        ),
                        holder.buttonTitle?.let {
                            Action(
                                    textRes = holder.buttonTitle,
                                    clickListener = { holder.buttonAction() }
                            )
                        },
                        dismissCallback = { _, _ -> holder.onDismissAction() }
                )
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        recycler.layoutManager?.let {
            outState.putParcelable(KEY_LIST_STATE, it.onSaveInstanceState())
        }
    }

    companion object {
        private const val KEY_LIST_SCENARIO = "list_scenario"
        private const val KEY_LIST_STATE = "list_state"

        @JvmStatic
        fun newInstance(listScenario: ListScenario): EngagedPeopleListFragment {
            val args = Bundle()
            args.putParcelable(KEY_LIST_SCENARIO, listScenario)

            val fragment = EngagedPeopleListFragment()
            fragment.arguments = args

            return fragment
        }
    }
}
