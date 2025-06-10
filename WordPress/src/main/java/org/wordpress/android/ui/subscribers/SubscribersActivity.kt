package org.wordpress.android.ui.subscribers

import android.os.Build
import android.os.Bundle
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import org.wordpress.android.R
import org.wordpress.android.ui.dataview.DataViewScreen
import org.wordpress.android.ui.dataview.DummyDataViewItem
import org.wordpress.android.ui.main.BaseAppCompatActivity

class SubscribersActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            ComposeView(this).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    this.isForceDarkAllowed = false
                }
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    DataViewScreen(
                        titleRes = R.string.app_name,
                        items = listOf(
                            DummyDataViewItem,
                            DummyDataViewItem,
                            DummyDataViewItem,
                            DummyDataViewItem,
                            DummyDataViewItem,
                            DummyDataViewItem,
                            DummyDataViewItem,
                            DummyDataViewItem,
                            DummyDataViewItem,
                            DummyDataViewItem,
                            DummyDataViewItem,
                            DummyDataViewItem,
                            DummyDataViewItem,
                            DummyDataViewItem,
                            DummyDataViewItem
                        ),
                        onSearchQueryChange = {},
                        onFilterClick = {}
                    )
                }
            }
        )
    }
}
