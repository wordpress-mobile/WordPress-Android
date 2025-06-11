package org.wordpress.android.ui.dataview

object DummyDataViewItems {
    fun getDummyData(): List<DataViewItem> {
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
