package org.wordpress.android.fluxc.list

import com.nhaarman.mockitokotlin2.mock
import org.wordpress.android.fluxc.model.list.ListConfig
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListDescriptorTypeIdentifier
import org.wordpress.android.fluxc.model.list.ListDescriptorUniqueIdentifier
import org.wordpress.android.fluxc.model.list.datastore.InternalPagedListDataSource
import org.wordpress.android.fluxc.model.list.datastore.ListItemDataSourceInterface

internal typealias TestListIdentifier = Long
internal typealias TestPagedListResultType = String
internal typealias TestInternalPagedListDataSource =
        InternalPagedListDataSource<TestListDescriptor, TestListIdentifier, TestPagedListResultType>
internal typealias TestListItemDataSource =
        ListItemDataSourceInterface<TestListDescriptor, TestListIdentifier, TestPagedListResultType>

internal class TestListDescriptor(
    override val uniqueIdentifier: ListDescriptorUniqueIdentifier = mock(),
    override val typeIdentifier: ListDescriptorTypeIdentifier = mock(),
    override val config: ListConfig = ListConfig.default
) : ListDescriptor
