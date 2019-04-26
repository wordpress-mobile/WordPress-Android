package org.wordpress.android.fluxc.list

import org.wordpress.android.fluxc.list.post.assertDifferentTypeIdentifiers
import org.wordpress.android.fluxc.list.post.assertDifferentUniqueIdentifiers
import org.wordpress.android.fluxc.list.post.assertSameTypeIdentifiers
import org.wordpress.android.fluxc.list.post.assertSameUniqueIdentifiers
import org.wordpress.android.fluxc.model.list.ListDescriptor

internal class ListDescriptorUnitTestCase<T : ListDescriptor>(
    val typeIdentifierReason: String,
    val uniqueIdentifierReason: String,
    val descriptor1: T,
    val descriptor2: T,
    val shouldHaveSameTypeIdentifier: Boolean,
    val shouldHaveSameUniqueIdentifier: Boolean
) {
    fun testTypeIdentifier() {
        if (shouldHaveSameTypeIdentifier) {
            assertSameTypeIdentifiers(
                    reason = typeIdentifierReason,
                    descriptor1 = descriptor1,
                    descriptor2 = descriptor2
            )
        } else {
            assertDifferentTypeIdentifiers(
                    reason = typeIdentifierReason,
                    descriptor1 = descriptor1,
                    descriptor2 = descriptor2
            )
        }
    }

    fun testUniqueIdentifier() {
        if (shouldHaveSameUniqueIdentifier) {
            assertSameUniqueIdentifiers(
                    reason = uniqueIdentifierReason,
                    descriptor1 = descriptor1,
                    descriptor2 = descriptor2
            )
        } else {
            assertDifferentUniqueIdentifiers(
                    reason = uniqueIdentifierReason,
                    descriptor1 = descriptor1,
                    descriptor2 = descriptor2
            )
        }
    }
}
