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
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_WIDGET_ADDED
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsDataTypeSelectionViewModel
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsSiteSelectionViewModel
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetColorSelectionDialogFragment
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetDataTypeSelectionDialogFragment
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetSiteSelectionDialogFragment
import org.wordpress.android.ui.stats.refresh.utils.trackMinifiedWidget
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.mergeNotNull
import javax.inject.Inject

class StatsMinifiedWidgetConfigureFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var minifiedWidgetUpdater: MinifiedWidgetUpdater
    @Inject lateinit var appPrefsWrapper: AppPrefsWrapper
    @Inject lateinit var siteStore: SiteStore
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    private lateinit var viewModel: StatsMinifiedWidgetConfigureViewModel
    private lateinit var siteSelectionViewModel: StatsSiteSelectionViewModel
    private lateinit var colorSelectionViewModel: StatsColorSelectionViewModel
    private lateinit var dataTypeSelectionViewModel: StatsDataTypeSelectionViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.stats_widget_configure_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val nonNullActivity = requireActivity()
        viewModel = ViewModelProviders.of(nonNullActivity, viewModelFactory)
                .get(StatsMinifiedWidgetConfigureViewModel::class.java)
        siteSelectionViewModel = ViewModelProviders.of(nonNullActivity, viewModelFactory)
                .get(StatsSiteSelectionViewModel::class.java)
        colorSelectionViewModel = ViewModelProviders.of(nonNullActivity, viewModelFactory)
                .get(StatsColorSelectionViewModel::class.java)
        dataTypeSelectionViewModel = ViewModelProviders.of(nonNullActivity, viewModelFactory)
                .get(StatsDataTypeSelectionViewModel::class.java)
        nonNullActivity.setResult(AppCompatActivity.RESULT_CANCELED)

        val appWidgetId = nonNullActivity.intent?.extras?.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            nonNullActivity.finish()
            return
        }

        viewModel.start(appWidgetId, siteSelectionViewModel, colorSelectionViewModel, dataTypeSelectionViewModel)

        site_container.setOnClickListener {
            siteSelectionViewModel.openSiteDialog()
        }
        color_container.setOnClickListener {
            colorSelectionViewModel.openColorDialog()
        }
        data_type_container.visibility = View.VISIBLE
        data_type_container.setOnClickListener {
            dataTypeSelectionViewModel.openDataTypeDialog()
        }

        add_widget_button.setOnClickListener {
            viewModel.addWidget()
        }

        siteSelectionViewModel.dialogOpened.observe(viewLifecycleOwner, Observer { event ->
            event?.applyIfNotHandled {
                StatsWidgetSiteSelectionDialogFragment().show(requireFragmentManager(), "stats_site_selection_fragment")
            }
        })

        colorSelectionViewModel.dialogOpened.observe(viewLifecycleOwner, Observer { event ->
            event?.applyIfNotHandled {
                StatsWidgetColorSelectionDialogFragment().show(
                        requireFragmentManager(),
                        "stats_view_mode_selection_fragment"
                )
            }
        })

        dataTypeSelectionViewModel.dialogOpened.observe(viewLifecycleOwner, Observer { event ->
            event?.applyIfNotHandled {
                StatsWidgetDataTypeSelectionDialogFragment().show(
                        requireFragmentManager(),
                        "stats_data_type_selection_fragment"
                )
            }
        })

        mergeNotNull(
                siteSelectionViewModel.notification,
                colorSelectionViewModel.notification,
                dataTypeSelectionViewModel.notification
        ).observe(
                viewLifecycleOwner,
                Observer { event ->
                    event?.applyIfNotHandled {
                        ToastUtils.showToast(activity, this)
                    }
                })

        viewModel.settingsModel.observe(viewLifecycleOwner, Observer { uiModel ->
            uiModel?.let {
                if (uiModel.siteTitle != null) {
                    site_value.text = uiModel.siteTitle
                }
                color_value.setText(uiModel.color.title)
                data_type_value.setText(uiModel.dataType.title)
                add_widget_button.isEnabled = uiModel.buttonEnabled
            }
        })

        viewModel.widgetAdded.observe(viewLifecycleOwner, Observer { event ->
            event?.getContentIfNotHandled()?.let {
                analyticsTrackerWrapper.trackMinifiedWidget(STATS_WIDGET_ADDED)
                minifiedWidgetUpdater.updateAppWidget(requireContext(), appWidgetId = appWidgetId)
                val resultValue = Intent()
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                activity?.setResult(RESULT_OK, resultValue)
                activity?.finish()
            }
        })
    }
}
