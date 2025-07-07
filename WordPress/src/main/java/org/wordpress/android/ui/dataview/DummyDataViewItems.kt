package org.wordpress.android.ui.dataview

import org.wordpress.android.R
import org.wordpress.android.ui.dataview.DataViewViewModel.Companion.PAGE_SIZE

/**
 * Returns a list of "dummy" [DataViewItem]s for use in Compose previews
 */
object DummyDataViewItems {
    fun getDummyDataViewItems(startIndex: Int = 0): List<DataViewItem> {
        val items = ArrayList<DataViewItem>()
        repeat(PAGE_SIZE) { index ->
            val id = (index + startIndex).toLong()
            val item = DataViewItem(
                id = id,
                title = "Title $id",
                image = DataViewItemImage(
                    imageUrl = null,
                    fallbackImageRes = R.drawable.ic_user_placeholder_primary_24,
                ),
                fields = listOf(
                    DataViewItemField(
                        value = "Text column $id",
                        valueType = DataViewFieldType.TEXT,
                        weight = .6f,
                    ),
                    DataViewItemField(
                        value = "2023-01-$id",
                        valueType = DataViewFieldType.DATE,
                        weight = .4f,
                    ),
                ),
            )
            items.add(item)
        }
        return items
    }
}
