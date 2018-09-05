package org.wordpress.android.fluxc.model.list

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.list.ListState.CAN_LOAD_MORE
import org.wordpress.android.fluxc.model.list.ListState.FETCHING_FIRST_PAGE
import org.wordpress.android.fluxc.model.list.ListState.LOADING_MORE

@Table
class ListModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var lastModified: String? = null // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z

    // These fields shouldn't be used directly.
    @Column var typeDbValue: Int? = null
    @Column var localSiteIdDbValue: Int? = null
    @Column var filterDbValue: String? = null
    @Column var orderDbValue: String? = null
    @Column var stateDbValue: Int = ListState.CAN_LOAD_MORE.value

    override fun getId(): Int = id

    override fun setId(id: Int) {
        this.id = id
    }

    val listDescriptor: ListDescriptor
        get() = ListDescriptor(typeDbValue, localSiteIdDbValue, filterDbValue, orderDbValue)

    val state: ListState
        get() = ListState.values().firstOrNull { it.value == this.stateDbValue }!!

    fun setListDescriptor(listDescriptor: ListDescriptor) {
        typeDbValue = listDescriptor.type.value
        localSiteIdDbValue = listDescriptor.localSiteId
        filterDbValue = listDescriptor.filter?.value
        orderDbValue = listDescriptor.order?.value
    }

    fun canLoadMore() = state == CAN_LOAD_MORE

    fun isFetchingFirstPage() = state == FETCHING_FIRST_PAGE

    fun isLoadingMore() = state == LOADING_MORE
}
