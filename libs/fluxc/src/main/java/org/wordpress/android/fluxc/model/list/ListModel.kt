package org.wordpress.android.fluxc.model.list

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.list.ListState.CAN_LOAD_MORE
import org.wordpress.android.fluxc.model.list.ListState.FETCHING_FIRST_PAGE
import org.wordpress.android.fluxc.model.list.ListState.LOADING_MORE
import org.wordpress.android.util.DateTimeUtils
import java.util.Date

const val LIST_STATE_TIMEOUT = 60 * 1000 // 1 minute

@Table
class ListModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var lastModified: String? = null // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z

    // These fields shouldn't be used directly.
    @Column var typeDbValue: Int? = null
    @Column var localSiteIdDbValue: Int? = null
    @Column var filterDbValue: String? = null
    @Column var orderDbValue: String? = null
    @Column var stateDbValue: Int = ListState.defaultState.value

    override fun getId(): Int = id

    override fun setId(id: Int) {
        this.id = id
    }

    val listDescriptor: ListDescriptor
        get() = ListDescriptor(typeDbValue, localSiteIdDbValue, filterDbValue, orderDbValue)

    private val state: ListState
        get() {
            /**
             * Since we keep the state in the DB, in the case of application being closed during a fetch, it'll carry
             * over to the next session. To prevent such cases, we use a timeout approach and if it has been a certain
             * time since the list is last updated, we'll simply ignore the state and return the default state.
             *
             * If certain amount of time passed since the state last updated, we should always fetch the first page.
             * For example, consider the case where we are fetching the first page of a list and the user closes the app.
             * Since we keep the state in the DB, it'll preserve it until the next session even though there is not
             * actually any request going on. This kind of check prevents such cases and also makes sure that we have
             * proper timeout.
             */
            if (lastModified != null) {
                val lastModified = DateTimeUtils.dateUTCFromIso8601(lastModified)
                val timePassed = (Date().time - lastModified.time)
                if (timePassed > LIST_STATE_TIMEOUT) {
                    return ListState.defaultState
                }
            }
            return ListState.values().firstOrNull { it.value == this.stateDbValue }!!
        }

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
