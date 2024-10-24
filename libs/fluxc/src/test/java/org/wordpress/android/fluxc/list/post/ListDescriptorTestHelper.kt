package org.wordpress.android.fluxc.list.post

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.wordpress.android.fluxc.model.list.ListConfig
import org.wordpress.android.fluxc.model.list.ListDescriptor

const val LIST_DESCRIPTOR_TEST_FIRST_MOCK_SITE_LOCAL_SITE_ID = 1
const val LIST_DESCRIPTOR_TEST_SECOND_MOCK_SITE_LOCAL_SITE_ID = 2
const val LIST_DESCRIPTOR_TEST_QUERY_1 = "some query"
const val LIST_DESCRIPTOR_TEST_QUERY_2 = "another query"
val LIST_DESCRIPTOR_TEST_LIST_CONFIG_1 = ListConfig(
        networkPageSize = 10,
        initialLoadSize = 10,
        dbPageSize = 10,
        prefetchDistance = 10
)
val LIST_DESCRIPTOR_TEST_LIST_CONFIG_2 = ListConfig(
        networkPageSize = 20,
        initialLoadSize = 20,
        dbPageSize = 20,
        prefetchDistance = 20
)

fun assertSameTypeIdentifiers(reason: String, descriptor1: ListDescriptor, descriptor2: ListDescriptor) {
    assertThat(reason, descriptor1.typeIdentifier, equalTo(descriptor2.typeIdentifier))
}

fun assertDifferentTypeIdentifiers(reason: String, descriptor1: ListDescriptor, descriptor2: ListDescriptor) {
    assertThat(reason, descriptor1.typeIdentifier, not(equalTo(descriptor2.typeIdentifier)))
}

fun assertSameUniqueIdentifiers(reason: String, descriptor1: ListDescriptor, descriptor2: ListDescriptor) {
    assertThat(reason, descriptor1.uniqueIdentifier, equalTo(descriptor2.uniqueIdentifier))
}

fun assertDifferentUniqueIdentifiers(reason: String, descriptor1: ListDescriptor, descriptor2: ListDescriptor) {
    assertThat(reason, descriptor1.uniqueIdentifier, not(equalTo(descriptor2.uniqueIdentifier)))
}
