package org.wordpress.android.ui.stats.refresh.lists.widget.weeks

import android.content.Context
import android.content.Intent
import org.wordpress.android.WordPress
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetBlockListProvider
import javax.inject.Inject

class WeekWidgetBlockListProviderFactory(val context: Context, val intent: Intent) {
    @Inject
    lateinit var viewModel: WeekWidgetBlockListViewModel

    init {
        (context.applicationContext as WordPress).component().inject(this)
    }

    fun build(): WidgetBlockListProvider {
        return WidgetBlockListProvider(context, viewModel, intent)
    }
}
