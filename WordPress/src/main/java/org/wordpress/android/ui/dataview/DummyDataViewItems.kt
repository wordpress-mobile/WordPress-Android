package org.wordpress.android.ui.dataview

import org.wordpress.android.R
import org.wordpress.android.ui.dataview.DataViewViewModel.Companion.PAGE_SIZE

/**
 * Returns a list of "dummy" [DataViewItem]s for use in Compose previews
 */
object DummyDataViewItems {
    @Suppress("MagicNumber")
    fun getDummyDataViewItems(startIndex: Int = 0): List<DataViewItem> {
        val items = ArrayList<DataViewItem>()
        repeat(PAGE_SIZE) { index ->
            val id = (index + startIndex).toLong()
            val fields = listOf(
                DataViewItemField(
                    value = "Title $id",
                    subValue = "Subtitle $id",
                    fieldType = DataViewFieldType.TITLE,
                ),
                DataViewItemField(
                    value = "2023-01-$id",
                    fieldType = DataViewFieldType.DATE,
                ),
            )
            val item = DataViewItem(
                id = id,
                image = DataViewItemImage(
                    imageUrl = null,
                    fallbackImageRes = R.drawable.ic_user_placeholder_primary_24,
                ),
                fields = fields,
            )
            items.add(item)
        }
        return items
    }
}
