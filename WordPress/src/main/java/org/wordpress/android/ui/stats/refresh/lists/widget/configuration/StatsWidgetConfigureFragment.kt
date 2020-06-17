package org.wordpress.android.ui.stats.refresh.lists.widget.configuration

import android.app.Activity.RESULT_OK
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
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
import org.wordpress.android.ui.stats.refresh.lists.widget.alltime.AllTimeWidgetUpdater
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.WidgetType.ALL_TIME_VIEWS
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.WidgetType.TODAY_VIEWS
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.WidgetType.WEEK_VIEWS
import org.wordpress.android.ui.stats.refresh.lists.widget.today.TodayWidgetUpdater
import org.wordpress.android.ui.stats.refresh.lists.widget.views.ViewsWidgetUpdater
import org.wordpress.android.ui.stats.refresh.utils.trackWithWidgetType
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.merge
import javax.inject.Inject

class StatsWidgetConfigureFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var viewsWidgetUpdater: ViewsWidgetUpdater
    @Inject lateinit var allTimeWidgetUpdater: AllTimeWidgetUpdater
    @Inject lateinit var todayWidgetUpdater: TodayWidgetUpdater
    @Inject lateinit var appPrefsWrapper: AppPrefsWrapper
    @Inject lateinit var siteStore: SiteStore
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    private lateinit var viewModel: StatsWidgetConfigureViewModel
    private lateinit var siteSelectionViewModel: StatsSiteSelectionViewModel
    private lateinit var colorSelectionViewModel: StatsColorSelectionViewModel
    private lateinit var widgetType: WidgetType

    override fun onInflate(context: Context, attrs: AttributeSet, savedInstanceState: Bundle?) {
        super.onInflate(context, attrs, savedInstanceState)
        activity?.let {
            val styledAttributes = it.obtainStyledAttributes(attrs, R.styleable.statsWidget)
            val views = styledAttributes.getInt(R.styleable.statsWidget_viewType, -1)
            widgetType = when (views) {
                0 -> WEEK_VIEWS
                1 -> ALL_TIME_VIEWS
                2 -> TODAY_VIEWS
                else -> {
                    throw IllegalArgumentException("The view type with the value $views needs to be specified")
                }
            }
            styledAttributes.recycle()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.stats_widget_configure_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val nonNullActivity = requireActivity()
        viewModel = ViewModelProviders.of(nonNullActivity, viewModelFactory)
                .get(StatsWidgetConfigureViewModel::class.java)
        siteSelectionViewModel = ViewModelProviders.of(nonNullActivity, viewModelFactory)
                .get(StatsSiteSelectionViewModel::class.java)
        colorSelectionViewModel = ViewModelProviders.of(nonNullActivity, viewModelFactory)
                .get(StatsColorSelectionViewModel::class.java)
        nonNullActivity.setResult(AppCompatActivity.RESULT_CANCELED)

        val appWidgetId = nonNullActivity.intent?.extras?.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            nonNullActivity.finish()
            return
        }

        viewModel.start(appWidgetId, widgetType, siteSelectionViewModel, colorSelectionViewModel)

        site_container.setOnClickListener {
            siteSelectionViewModel.openSiteDialog()
        }
        color_container.setOnClickListener {
            colorSelectionViewModel.openColorDialog()
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

        merge(siteSelectionViewModel.notification, colorSelectionViewModel.notification).observe(
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
                add_widget_button.isEnabled = uiModel.buttonEnabled
            }
        })

        viewModel.widgetAdded.observe(viewLifecycleOwner, Observer { event ->
            event?.getContentIfNotHandled()?.let {
                analyticsTrackerWrapper.trackWithWidgetType(STATS_WIDGET_ADDED, it.widgetType)
                when (it.widgetType) {
                    WEEK_VIEWS -> {
                        viewsWidgetUpdater.updateAppWidget(requireContext(), appWidgetId = it.appWidgetId)
                    }
                    ALL_TIME_VIEWS -> {
                        allTimeWidgetUpdater.updateAppWidget(requireContext(), appWidgetId = it.appWidgetId)
                    }
                    TODAY_VIEWS -> {
                        todayWidgetUpdater.updateAppWidget(requireContext(), appWidgetId = it.appWidgetId)
                    }
                }
                val resultValue = Intent()
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                activity?.setResult(RESULT_OK, resultValue)
                activity?.finish()
            }
        })
    }

    enum class WidgetType { WEEK_VIEWS, ALL_TIME_VIEWS, TODAY_VIEWS }
}
