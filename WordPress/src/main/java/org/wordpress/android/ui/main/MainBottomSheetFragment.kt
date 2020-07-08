package org.wordpress.android.ui.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.add_content_bottom_sheet.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.R.dimen
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.util.QuickStartUtils.Companion.addQuickStartFocusPointAboveTheView
import org.wordpress.android.viewmodel.main.WPMainActivityViewModel
import javax.inject.Inject

class MainBottomSheetFragment : BottomSheetDialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: WPMainActivityViewModel
    private var quickStartEvent: QuickStartEvent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            quickStartEvent = savedInstanceState.getParcelable(QuickStartEvent.KEY)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.add_content_bottom_sheet, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.content_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        recyclerView.adapter = AddContentAdapter(requireActivity())

        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory).get(WPMainActivityViewModel::class.java)

        viewModel.mainActions.observe(this, Observer {
            (dialog?.content_recycler_view?.adapter as? AddContentAdapter)?.update(it ?: listOf())
        })

        dialog?.setOnShowListener { dialogInterface ->
            val sheetDialog = dialogInterface as? BottomSheetDialog

            val bottomSheet = sheetDialog?.findViewById<View>(
                    com.google.android.material.R.id.design_bottom_sheet
            ) as? FrameLayout

            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        if (quickStartEvent != null) {
            showQuickStartFocusPoint()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onEvent(event: QuickStartEvent) {
        if (!isAdded || view == null) {
            return
        }

        EventBus.getDefault().removeStickyEvent(event)
        quickStartEvent = event

        if (quickStartEvent?.task == QuickStartTask.PUBLISH_POST) {
            showQuickStartFocusPoint()
        }
    }

    private fun showQuickStartFocusPoint() {
        // we are waiting for RecyclerView to populate itself with views and then grab the one we need one when it's ready
        content_recycler_view?.viewTreeObserver?.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val holder: ViewHolder? = content_recycler_view.findViewHolderForAdapterPosition(
                        1
                )
                if (holder != null) {
                    val quickStartTarget = holder.itemView
                    quickStartTarget.post(Runnable {
                        if (view == null) {
                            return@Runnable
                        }
                        val focusPointContainer = view!!.findViewById<ViewGroup>(R.id.add_content_bottom_sheet_root_view)
                        val focusPointSize = resources.getDimensionPixelOffset(dimen.quick_start_focus_point_size)
                        val verticalOffset = (quickStartTarget.height - focusPointSize) / 2
                        val horizontalOffset = (quickStartTarget.width - focusPointSize) / 2
                        addQuickStartFocusPointAboveTheView(
                                focusPointContainer, quickStartTarget,
                                horizontalOffset, verticalOffset
                        )
                    })
                    content_recycler_view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(QuickStartEvent.KEY, quickStartEvent)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }
}
