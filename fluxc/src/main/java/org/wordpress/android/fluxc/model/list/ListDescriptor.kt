package org.wordpress.android.fluxc.model.list

data class ListDescriptorTypeIdentifier(val value: Int)
data class ListDescriptorUniqueIdentifier(val value: Int)

interface ListDescriptor {
    val uniqueIdentifier: ListDescriptorUniqueIdentifier
    val typeIdentifier: ListDescriptorTypeIdentifier
}
