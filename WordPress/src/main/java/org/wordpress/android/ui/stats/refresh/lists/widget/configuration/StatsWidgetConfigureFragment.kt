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
import androidx.lifecycle.ViewModelProvider
import dagger.android.support.DaggerFragment
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_WIDGET_ADDED
import org.wordpress.android.databinding.StatsWidgetConfigureFragmentBinding
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.refresh.lists.widget.alltime.AllTimeWidgetUpdater
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.WidgetType.ALL_TIME_VIEWS
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.WidgetType.TODAY_VIEWS
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.WidgetType.WEEK_TOTAL
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.WidgetType.WEEK_VIEWS
import org.wordpress.android.ui.stats.refresh.lists.widget.today.TodayWidgetUpdater
import org.wordpress.android.ui.stats.refresh.lists.widget.views.ViewsWidgetUpdater
import org.wordpress.android.ui.stats.refresh.lists.widget.weeks.WeekViewsWidgetUpdater
import org.wordpress.android.ui.stats.refresh.utils.trackWithWidgetType
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class StatsWidgetConfigureFragment : DaggerFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var viewsWidgetUpdater: ViewsWidgetUpdater
    @Inject
    lateinit var allTimeWidgetUpdater: AllTimeWidgetUpdater
    @Inject
    lateinit var todayWidgetUpdater: TodayWidgetUpdater
    @Inject
    lateinit var weekViewsWidgetUpdater: WeekViewsWidgetUpdater
    @Inject
    lateinit var appPrefsWrapper: AppPrefsWrapper
    @Inject
    lateinit var siteStore: SiteStore
    @Inject
    lateinit var imageManager: ImageManager
    @Inject
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    private lateinit var viewModel: StatsWidgetConfigureViewModel
    private lateinit var siteSelectionViewModel: StatsSiteSelectionViewModel
    private lateinit var colorSelectionViewModel: StatsColorSelectionViewModel
    private lateinit var widgetType: WidgetType

    @Suppress("MagicNumber")
    override fun onInflate(context: Context, attrs: AttributeSet, savedInstanceState: Bundle?) {
        super.onInflate(context, attrs, savedInstanceState)
        activity?.let {
            val styledAttributes = it.obtainStyledAttributes(attrs, R.styleable.statsWidget)
            val views = styledAttributes.getInt(R.styleable.statsWidget_viewType, -1)
            widgetType = when (views) {
                0 -> WEEK_VIEWS
                1 -> ALL_TIME_VIEWS
                2 -> TODAY_VIEWS
                3 -> WEEK_TOTAL
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

    @Suppress("DEPRECATION", "LongMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val nonNullActivity = requireActivity()
        viewModel = ViewModelProvider(nonNullActivity, viewModelFactory)
            .get(StatsWidgetConfigureViewModel::class.java)
        siteSelectionViewModel = ViewModelProvider(nonNullActivity, viewModelFactory)
            .get(StatsSiteSelectionViewModel::class.java)
        colorSelectionViewModel = ViewModelProvider(nonNullActivity, viewModelFactory)
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
        with(StatsWidgetConfigureFragmentBinding.bind(view)) {
            siteContainer.setOnClickListener {
                siteSelectionViewModel.openSiteDialog()
            }
            colorContainer.setOnClickListener {
                colorSelectionViewModel.openColorDialog()
            }

            addWidgetButton.setOnClickListener {
                viewModel.addWidget()
            }

            siteSelectionViewModel.dialogOpened.observeEvent(viewLifecycleOwner, {
                StatsWidgetSiteSelectionDialogFragment().show(requireFragmentManager(), "stats_site_selection_fragment")
            })

            colorSelectionViewModel.dialogOpened.observeEvent(viewLifecycleOwner, {
                StatsWidgetColorSelectionDialogFragment().show(
                    requireFragmentManager(),
                    "stats_view_mode_selection_fragment"
                )
            })

            merge(siteSelectionViewModel.notification, colorSelectionViewModel.notification).observeEvent(
                viewLifecycleOwner,
                {
                    ToastUtils.showToast(activity, it)
                })

            viewModel.settingsModel.observe(viewLifecycleOwner, { uiModel ->
                uiModel?.let {
                    if (uiModel.siteTitle != null) {
                        siteValue.text = uiModel.siteTitle
                    }
                    colorValue.setText(uiModel.color.title)
                    addWidgetButton.isEnabled = uiModel.buttonEnabled
                }
            })

            viewModel.widgetAdded.observeEvent(viewLifecycleOwner) {
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
                    WEEK_TOTAL -> {
                        weekViewsWidgetUpdater.updateAppWidget(requireContext(), appWidgetId = it.appWidgetId)
                    }
                }
                val resultValue = Intent()
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                activity?.setResult(RESULT_OK, resultValue)
                activity?.finish()
            }
        }
    }

    enum class WidgetType { WEEK_VIEWS, ALL_TIME_VIEWS, TODAY_VIEWS, WEEK_TOTAL }
}
