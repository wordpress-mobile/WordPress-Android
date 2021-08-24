# Frequently Asked Questions

#### I can't build/test/package the project because of a `PermGen space` error.

Create a `gradle.properties` file in the project root directory with the
following: `org.gradle.jvmargs=-XX:MaxPermSize=1024m`.

#### Should I use `AppPrefs` or `SelectedSiteRepository` as the source of truth on a selected site?

The short answer is to always use the `SelectedSiteRepository`, which is the in-memory mechanism.
This should always be your goto class when you want to work with the selected site instance, your
source of truth.

Getting the selected site from `AppPrefs`, which is the persistence mechanism, should only be
limited to specific usages. Below is a complete list of those:

- `WordPress.mUpdateSelectedSite`: Which runs in a background task and updates the site information.
- `WordPress.sanitizeMediaUploadStateForSite()`: Which is being trigger from `onCreate(...)`and
  `onAppComesFromBackground()`.
- `UploadService,onMediaUploaded(...)`: Which ensures that the handlers have already received and
  processed the incoming `OnMediaUploaded` event.
- `WPMainActivity.initSelectedSite()`: Which is being trigger from multiple place within
  `WPMainActivity` itself, including but not limited to `onCreate(...)`, `onResume(...)` and
  `onActivityResult(...)`.

#### Should I use `id`, `siteId`, `siteLocalId`, `siteRemoteId` or any other naming convention?

The `SiteModel.mId` is the local id, which is actually an auto-increment database index that gets
generated when sites are being persisted locally. However, this id is getting reset when users
log-out and as such it will NOT be the same for a specific site. As such, when working with such an
`id` you should always name it as `siteLocalId` to avoid any confusion.

The `SimeModel.mSiteId`, is the remote id, which comes from the backend, and it is generated during
site creation. Then, it gets persisted within the local database, next to the local id. This id is
NOT getting reset when users log-out and as such it will always be the same for a specific site, no
matter what. As such, when working with such an `siteId` you should always name it as `siteRemoteId`
to avoid any confusion.
