package org.wordpress.android.ui.people.utils

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.Person.PersonType
import org.wordpress.android.ui.people.utils.PeopleUtils.FetchFollowersCallback
import org.wordpress.android.ui.people.utils.PeopleUtils.FetchUsersCallback
import org.wordpress.android.ui.people.utils.PeopleUtils.FetchViewersCallback
import org.wordpress.android.ui.people.utils.PeopleUtils.InvitationsSendCallback
import org.wordpress.android.ui.people.utils.PeopleUtils.RemovePersonCallback
import org.wordpress.android.ui.people.utils.PeopleUtils.UpdateUserCallback
import org.wordpress.android.ui.people.utils.PeopleUtils.ValidateUsernameCallback
import javax.inject.Inject

/**
 * Injectable wrapper around PeopleUtils.
 *
 * PeopleUtils is consisted of static methods, which make the client code difficult to test/mock.
 * Main purpose of this wrapper is to make testing easier.
 */
class PeopleUtilsWrapper @Inject constructor() {
    fun fetchUsers(
        site: SiteModel,
        offset: Int,
        callback: FetchUsersCallback
    ) = PeopleUtils.fetchUsers(site, offset, callback)

    fun fetchAuthors(
        site: SiteModel,
        offset: Int,
        callback: FetchUsersCallback
    ) = PeopleUtils.fetchAuthors(site, offset, callback)

    fun fetchRevisionAuthorsDetails(
        site: SiteModel,
        authors: List<String>,
        callback: FetchUsersCallback
    ) = PeopleUtils.fetchRevisionAuthorsDetails(site, authors, callback)

    fun fetchFollowers(
        site: SiteModel,
        page: Int,
        callback: FetchFollowersCallback
    ) = PeopleUtils.fetchFollowers(site, page, callback)

    fun fetchEmailFollowers(
        site: SiteModel,
        page: Int,
        callback: FetchFollowersCallback
    ) = PeopleUtils.fetchEmailFollowers(site, page, callback)

    fun fetchViewers(
        site: SiteModel,
        offset: Int,
        callback: FetchViewersCallback
    ) = PeopleUtils.fetchViewers(site, offset, callback)

    fun updateRole(
        site: SiteModel,
        personID: Long,
        newRole: String,
        localTableBlogId: Int,
        callback: UpdateUserCallback
    ) = PeopleUtils.updateRole(site, personID, newRole, localTableBlogId, callback)

    fun removeUser(
        site: SiteModel,
        personID: Long,
        callback: RemovePersonCallback
    ) = PeopleUtils.removeUser(site, personID, callback)

    fun removeFollower(
        site: SiteModel,
        personID: Long,
        personType: PersonType,
        callback: RemovePersonCallback
    ) = PeopleUtils.removeFollower(site, personID, personType, callback)

    fun removeViewer(
        site: SiteModel,
        personID: Long,
        callback: RemovePersonCallback
    ) = PeopleUtils.removeViewer(site, personID, callback)

    fun validateUsernames(
        usernames: List<String>,
        role: String,
        wpComBlogId: Long,
        callback: ValidateUsernameCallback
    ) = PeopleUtils.validateUsernames(usernames, role, wpComBlogId, callback)

    fun sendInvitations(
        usernames: List<String>,
        role: String,
        message: String,
        wpComBlogId: Long,
        callback: InvitationsSendCallback
    ) = PeopleUtils.sendInvitations(usernames, role, message, wpComBlogId, callback)
}
