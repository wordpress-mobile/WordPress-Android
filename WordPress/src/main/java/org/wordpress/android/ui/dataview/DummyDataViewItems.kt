package org.wordpress.android.ui.dataview

/**
 * Returns a list of "dummy" [DataViewItem]s for use in Compose previews
 */
object DummyDataViewItems {
    @Suppress("MagicNumber")
    fun getDummyDataViewItems(): List<DataViewItem> {
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
        repeat(25) { id ->
            addWithId(id.toLong())
        }
        return items
    }
}
