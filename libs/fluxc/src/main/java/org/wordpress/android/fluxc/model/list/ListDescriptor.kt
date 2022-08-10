package org.wordpress.android.fluxc.model.list

data class ListDescriptorTypeIdentifier(val value: Int)
data class ListDescriptorUniqueIdentifier(val value: Int)

/**
 * This is the interface every list descriptor needs to implement for it to work with `ListStore`. It can be used to
 * describe which site a list belongs to, which filters it has, how to order it or anything and everything else a
 * feature might need.
 *
 * This presents an interesting challenge because if every list can be different, how do we use a generic interface
 * to save it in the DB and more importantly how do we query it back from it. In a traditional solution, one approach
 * would be to separate each list descriptor and save them in the DB in separate tables, and another approach would be
 * to add common fields in one table and try to work off of it. Neither approach is very flexible.
 *
 * `ListStore` takes a completely different approach to this problem by shying away from using lists directly and
 * tying everything to [ListDescriptor]s. For example, there is no way to access a list by its id, `ListSqlUtils`
 * ensures this. That means, we don't necessarily need to save every bit of information about a list in the DB,
 * as long as there is a way to identify it. That's where the [uniqueIdentifier] property comes in.
 * By using a unique identifier, we can save them in the DB and then retrieve them from it as long as we can calculate
 * the exact same identifier. This also means that we erase the information about a list, and we can no longer access it
 * even if we queried the list in some other way. However, as previously stated, all the components in `ListStore` is
 * designed in a way to work with that constraint.
 *
 * There is another interesting challenge this decision brings. What if we want to be able to identify lists that
 * has a certain "type". For example, let's say we are dealing with several post lists all belonging to the same site.
 * If a post in that site is updated, we'd expect all the post lists for that site to be notified of this change.
 * That's where the [typeIdentifier] comes in. It gives the class that's implementing this interface a way to group
 * them so the changes for the items in them notifies all of them together.
 *
 * TODO: Please note that "type" is not the correct term for this and should be renamed.
 *
 * @property uniqueIdentifier is a globally unique value for the described list. The responsibility of calculating a
 * unique value for the described list relies on the developer implementing this interface. If there is ever a collision
 * between two identifiers, incorrect items could be shown.
 *
 * @property typeIdentifier is an identifier used to describe lists belonging to the same "type". For example, post
 * lists belonging to the same site, would have the same [typeIdentifier]. Whereas, comments for that site, or posts
 * of another site would have a different identifier.
 */
@Suppress("ForbiddenComment")
interface ListDescriptor {
    val uniqueIdentifier: ListDescriptorUniqueIdentifier
    val typeIdentifier: ListDescriptorTypeIdentifier
    val config: ListConfig
}
