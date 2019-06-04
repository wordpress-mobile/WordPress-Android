package org.wordpress.android.ui.stats.refresh.lists.widget

import android.app.AlertDialog
import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatDialogFragment
import android.widget.RadioGroup
import dagger.android.support.AndroidSupportInjection
import org.wordpress.android.R
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.lists.widget.ViewsWidgetViewModel.Color
import org.wordpress.android.ui.stats.refresh.lists.widget.ViewsWidgetViewModel.Color.DARK
import org.wordpress.android.ui.stats.refresh.lists.widget.ViewsWidgetViewModel.Color.LIGHT
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

class ColorSelectionDialogFragment : AppCompatDialogFragment() {
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ViewsWidgetViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel = ViewModelProviders.of(activity!!, viewModelFactory).get(ViewsWidgetViewModel::class.java)
        val alertDialogBuilder = AlertDialog.Builder(activity)
        val view = activity!!.layoutInflater.inflate(layout.stats_color_selector, null) as RadioGroup
        view.setOnCheckedChangeListener { _, checkedId ->
            checkedId.toColor()?.let { viewModel.colorClicked(it) }
        }
        alertDialogBuilder.setView(view)
        viewModel.settingsModel.observe(this, Observer {
            val updatedColor = it?.color
            val currentColor = view.checkedRadioButtonId.toColor()
            if (updatedColor != currentColor) {
                updatedColor?.let { view.check(updatedColor.toViewId()) }
            }
        })
        alertDialogBuilder.setTitle(R.string.stats_widget_select_color)

        alertDialogBuilder.setPositiveButton(R.string.dialog_button_ok) { dialog, _ ->
            dialog?.dismiss()
        }
        return alertDialogBuilder.create()
    }

    private fun Color.toViewId(): Int {
        return when (this) {
            LIGHT -> R.id.stats_widget_light_color
            DARK -> R.id.stats_widget_dark_color
        }
    }

    private fun Int.toColor(): Color? {
        return when (this) {
            R.id.stats_widget_light_color -> LIGHT
            R.id.stats_widget_dark_color -> DARK
            else -> null
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }
}
