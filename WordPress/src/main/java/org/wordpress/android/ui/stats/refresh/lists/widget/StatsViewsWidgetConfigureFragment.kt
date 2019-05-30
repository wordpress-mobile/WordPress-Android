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
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

class StatsViewsWidgetConfigureFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var appPrefsWrapper: AppPrefsWrapper
    @Inject lateinit var siteStore: SiteStore
    @Inject lateinit var imageManager: ImageManager
    private lateinit var viewModel: StatsViewsWidgetConfigureViewModel
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.stats_views_widget_configure_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProviders.of(activity!!, viewModelFactory)
                .get(StatsViewsWidgetConfigureViewModel::class.java)
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
            StatsWidgetSiteSelectionDialogFragment().show(fragmentManager, "stats_site_selection_fragment")
        }
        color_container.setOnClickListener {
            ColorSelectionDialogFragment().show(fragmentManager, "stats_view_mode_selection_fragment")
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
                add_widget_button.isEnabled = uiModel.buttonEnabled
            }
        })

        viewModel.widgetAdded.observe(this, Observer { event ->
            event?.applyIfNotHandled {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                StatsViewsWidget.updateAppWidget(
                        context!!,
                        appWidgetManager,
                        appWidgetId,
                        appPrefsWrapper,
                        siteStore,
                        imageManager
                )
                val resultValue = Intent()
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                activity?.setResult(RESULT_OK, resultValue)
                activity?.finish()
            }
        })

        viewModel.start(appWidgetId)
    }
}
