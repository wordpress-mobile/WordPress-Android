package org.wordpress.android.ui.stats.refresh.lists.widget.configuration

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.android.support.AndroidSupportInjection
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsDataTypeSelectionViewModel.DataType
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsDataTypeSelectionViewModel.DataType.COMMENTS
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsDataTypeSelectionViewModel.DataType.LIKES
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsDataTypeSelectionViewModel.DataType.VIEWS
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsDataTypeSelectionViewModel.DataType.VISITORS
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

class StatsWidgetDataTypeSelectionDialogFragment : AppCompatDialogFragment() {
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: StatsDataTypeSelectionViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel = ViewModelProviders.of(activity!!, viewModelFactory)
                .get(StatsDataTypeSelectionViewModel::class.java)
        val alertDialogBuilder = MaterialAlertDialogBuilder(activity)
        val view = activity!!.layoutInflater.inflate(R.layout.stats_data_type_selector, null) as RadioGroup
        view.setOnCheckedChangeListener { _, checkedId ->
            checkedId.toDataType()?.let { viewModel.selectDataType(it) }
        }
        alertDialogBuilder.setView(view)
        viewModel.dataType.observe(this, Observer { updatedDataType ->
            val currentDataType = view.checkedRadioButtonId.toDataType()
            if (updatedDataType != currentDataType) {
                updatedDataType?.let { view.check(updatedDataType.toViewId()) }
            }
        })
        alertDialogBuilder.setTitle(R.string.stats_widget_select_type)

        alertDialogBuilder.setPositiveButton(R.string.dialog_button_ok) { dialog, _ ->
            dialog?.dismiss()
        }
        return alertDialogBuilder.create()
    }

    private fun DataType.toViewId(): Int {
        return when (this) {
            VIEWS -> R.id.stats_widget_views
            VISITORS -> R.id.stats_widget_visitors
            COMMENTS -> R.id.stats_widget_comments
            LIKES -> R.id.stats_widget_likes
        }
    }

    private fun Int.toDataType(): DataType? {
        return when (this) {
            R.id.stats_widget_views -> VIEWS
            R.id.stats_widget_visitors -> VISITORS
            R.id.stats_widget_comments -> COMMENTS
            R.id.stats_widget_likes -> LIKES
            else -> null
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }
}
