# User-Facing Changes

A rolling log of changes a customer could notice. Newest entries at the top.

What belongs here and how to write an entry: [`USER-FACING-CHANGES-DOCUMENTATION.md`](USER-FACING-CHANGES-DOCUMENTATION.md).

---

<!-- Add new entries directly below this line. -->

- **Referrers card shown by default in Stats**
  The redesigned Stats screen shows the Referrers card under Posts & pages without having to add it. Sites that already had the new Stats screen customised get the card added once, in the same position; removing it afterwards sticks.
  - **Apps:** Both
  - **Type:** Changed
  - **Availability:** Flag: android_new_stats
  - **Where:** My Site → Stats → Traffic
  - **Notes:** Anyone who had already added Referrers themselves keeps it where they put it, and no other card is moved or restored.

- **"Remove location from media" removes all GPS EXIF data**
  With the setting on, uploaded images no longer keep any GPS EXIF data. Previously it blanked only latitude, longitude, altitude and GPS time, so tags such as destination coordinates, place name, compass direction and speed still reached the server.
  - **Apps:** Both
  - **Type:** Fixed
  - **Availability:** Everyone
  - **Where:** Me → App Settings → Remove location from media
  - **Notes:** Doesn't yet cover images added with the block editor's Upload button, or HEIC/HEIF files. Location stored in XMP metadata, as written by apps like Lightroom and Photoshop, is not removed.

- **Reader search redesigned**
  Reader search has a new layout: the search field sits in the top bar, recent searches show as one list as you type (each with a delete button, plus "Clear search history" at the end), and the Posts and Sites tabs appear as soon as a search is submitted. A failed post search now shows "Unable to perform search" in the Posts tab instead of a blank screen.
  - **Apps:** Jetpack
  - **Type:** Changed
  - **Availability:** Everyone
  - **Where:** Reader → Search
  - **Notes:** Recent searches are filtered by what has been typed so far. Clearing the search field returns to the recent searches. The Posts and Sites tabs each keep their scroll position when switching between them.
