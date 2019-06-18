package org.wordpress.android.ui.stats.refresh.lists.widget.minified

import android.app.Activity.RESULT_OK
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.stats_widget_configure_fragment.*
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.refresh.lists.widget.alltime.AllTimeWidgetUpdater
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.ColorSelectionDialogFragment
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.DataTypeSelectionDialogFragment
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsDataTypeSelectionViewModel
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsSiteSelectionViewModel
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetSiteSelectionDialogFragment
import org.wordpress.android.ui.stats.refresh.lists.widget.today.TodayWidgetUpdater
import org.wordpress.android.ui.stats.refresh.lists.widget.views.ViewsWidgetUpdater
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

class StatsMinifiedWidgetConfigureFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var viewsWidgetUpdater: ViewsWidgetUpdater
    @Inject lateinit var allTimeWidgetUpdater: AllTimeWidgetUpdater
    @Inject lateinit var todayWidgetUpdater: TodayWidgetUpdater
    @Inject lateinit var appPrefsWrapper: AppPrefsWrapper
    @Inject lateinit var siteStore: SiteStore
    @Inject lateinit var imageManager: ImageManager
    private lateinit var viewModel: StatsMinifiedWidgetConfigureViewModel
    private lateinit var siteSelectionViewModel: StatsSiteSelectionViewModel
    private lateinit var colorSelectionViewModel: StatsColorSelectionViewModel
    private lateinit var dataTypeSelectionViewModel: StatsDataTypeSelectionViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.stats_widget_configure_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProviders.of(activity!!, viewModelFactory)
                .get(StatsMinifiedWidgetConfigureViewModel::class.java)
        siteSelectionViewModel = ViewModelProviders.of(activity!!, viewModelFactory)
                .get(StatsSiteSelectionViewModel::class.java)
        colorSelectionViewModel = ViewModelProviders.of(activity!!, viewModelFactory)
                .get(StatsColorSelectionViewModel::class.java)
        dataTypeSelectionViewModel = ViewModelProviders.of(activity!!, viewModelFactory)
                .get(StatsDataTypeSelectionViewModel::class.java)
        activity?.setResult(AppCompatActivity.RESULT_CANCELED)

        val appWidgetId = activity?.intent?.extras?.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            activity?.finish()
            return
        }

        viewModel.start(appWidgetId, siteSelectionViewModel, colorSelectionViewModel, dataTypeSelectionViewModel)

        site_container.setOnClickListener {
            StatsWidgetSiteSelectionDialogFragment().show(fragmentManager, "stats_site_selection_fragment")
        }
        color_container.setOnClickListener {
            ColorSelectionDialogFragment().show(fragmentManager, "stats_view_mode_selection_fragment")
        }
        data_type_container.visibility = View.VISIBLE
        data_type_container.setOnClickListener {
            DataTypeSelectionDialogFragment().show(fragmentManager, "stats_data_type_selection_fragment")
        }

        add_widget_button.setOnClickListener {
            viewModel.addWidget()
        }

        viewModel.settingsModel.observe(this, Observer { uiModel ->
            uiModel?.let {
                if (uiModel.siteTitle != null) {
                    site_value.text = uiModel.siteTitle
                }
                color_value.setText(uiModel.color.title)
                if (uiModel.dataType != null) {
                    data_type_value.setText(uiModel.dataType.title)
                }
                add_widget_button.isEnabled = uiModel.buttonEnabled
            }
        })

        viewModel.widgetAdded.observe(this, Observer { event ->
            event?.getContentIfNotHandled()?.let {
                // TODO Update minified widget
                val resultValue = Intent()
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                activity?.setResult(RESULT_OK, resultValue)
                activity?.finish()
            }
        })
    }

    enum class ViewType { WEEK_VIEWS, ALL_TIME_VIEWS, TODAY_VIEWS }
}
