package org.wordpress.android.ui.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.add_content_bottom_sheet.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.viewmodel.main.WPMainActivityViewModel
import javax.inject.Inject

class MainBottomSheetFragment : BottomSheetDialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: WPMainActivityViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.add_content_bottom_sheet, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.create_actions_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        recyclerView.adapter = AddContentAdapter()

        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory).get(WPMainActivityViewModel::class.java)

        viewModel.mainActions.observe(this, Observer {
            (dialog.create_actions_recycler_view.adapter as? AddContentAdapter)?.update(it ?: listOf())
        })
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }
}
