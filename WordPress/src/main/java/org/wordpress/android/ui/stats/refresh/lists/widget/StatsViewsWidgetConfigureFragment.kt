package org.wordpress.android.ui.stats.refresh.lists.widget

import android.app.Activity.RESULT_OK
import android.appwidget.AppWidgetManager
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.stats_views_widget_configure_fragment.*
import org.wordpress.android.R
import javax.inject.Inject

class StatsViewsWidgetConfigureFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    lateinit var viewModel: ViewsWidgetViewModel
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.stats_views_widget_configure_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ViewsWidgetViewModel::class.java)
        activity?.setResult(AppCompatActivity.RESULT_CANCELED)

        val appWidgetId = activity?.intent?.extras?.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            activity?.finish()
            return
        }

        site_container.setOnClickListener {
            viewModel.siteClicked()
        }
        color_container.setOnClickListener {
            viewModel.colorClicked()
        }

        add_widget_button.setOnClickListener {
            viewModel.addWidget()
        }

        viewModel.uiModel.observe(this, Observer { uiModel ->
            uiModel?.let {
                if (uiModel.siteTitle != null) {
                    site_value.text = uiModel.siteTitle
                }
                if (uiModel.viewMode != null) {
                    color_value.setText(uiModel.viewMode.title)
                }
                add_widget_button.isEnabled = uiModel.buttonEnabled
            }
        })

        viewModel.widgetAdded.observe(this, Observer { event ->
            event?.applyIfNotHandled {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                StatsViewsWidget.updateAppWidget(context!!, appWidgetManager, appWidgetId)
                val resultValue = Intent()
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                activity?.setResult(RESULT_OK, resultValue)
                activity?.finish()
            }
        })

        viewModel.start(appWidgetId)
    }
}

