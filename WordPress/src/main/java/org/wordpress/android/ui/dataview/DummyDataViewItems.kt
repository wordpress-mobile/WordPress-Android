package org.wordpress.android.ui.dataview

import org.wordpress.android.ui.dataview.DataViewViewModel.Companion.PAGE_SIZE

/**
 * Returns a list of "dummy" [DataViewItem]s for use in Compose previews
 */
object DummyDataViewItems {
    @Suppress("MagicNumber")
    fun getDummyDataViewItems(startIndex: Int = 0): List<DataViewItem> {
        val items = ArrayList<DataViewItem>()
        fun addWithId(id: Long) {
            items.add(
                DataViewItem(
                    id = id,
                    title = "Title $id",
                    subtitle = "Subtitle $id",
                    dateLine = "2023-01-01",
                )
            )
        }
        repeat(PAGE_SIZE) { index ->
            val id = (index + startIndex).toLong()
            addWithId(id)
        }
        return items
    }
}
