package org.wordpress.android.fluxc.list

import com.nhaarman.mockitokotlin2.mock
import org.wordpress.android.fluxc.model.list.ListConfig
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListDescriptorTypeIdentifier
import org.wordpress.android.fluxc.model.list.ListDescriptorUniqueIdentifier
import org.wordpress.android.fluxc.model.list.datastore.InternalPagedListDataStore
import org.wordpress.android.fluxc.model.list.datastore.ListItemDataStoreInterface

internal typealias TestListIdentifier = Long
internal typealias TestPagedListResultType = String
internal typealias TestInternalPagedListDataStore =
        InternalPagedListDataStore<TestListDescriptor, TestListIdentifier, TestPagedListResultType>
internal typealias TestListItemDataStore =
        ListItemDataStoreInterface<TestListDescriptor, TestListIdentifier, TestPagedListResultType>

internal class TestListDescriptor(
    override val uniqueIdentifier: ListDescriptorUniqueIdentifier = mock(),
    override val typeIdentifier: ListDescriptorTypeIdentifier = mock(),
    override val config: ListConfig = ListConfig.default
) : ListDescriptor
