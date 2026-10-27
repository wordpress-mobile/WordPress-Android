# User-Facing Changes

A rolling log of changes a customer could notice. Newest entries at the top.

What belongs here and how to write an entry: [`USER-FACING-CHANGES-DOCUMENTATION.md`](USER-FACING-CHANGES-DOCUMENTATION.md).

---

<!-- Add new entries directly below this line. -->

- **Stats dates follow the device language, and the date selector names the dates on screen**
  Every date in the redesigned Stats screen is now written in the order the device's language uses — "Aug 11" in US English, "11 Aug" in British English, "2026年8月11日" in Japanese — instead of always day-then-month. This covers the date selector at the top of Traffic, the Views chart's legend and axis labels, the Subscribers chart, the detail screens' subtitles, and the "Best day" and "Today" cards. The date selector also shows the dates a preset covers on a second line, so "Last 7 days" now reads with "Oct 21 - Oct 27, 2026" underneath it.
  - **Apps:** Both
  - **Type:** Changed
  - **Availability:** Flag: android_new_stats
  - **Where:** My Site → Stats → Traffic (date selector, Views card) and Insights; My Site → Stats → Subscribers
  - **Notes:** A refresh still keeps the period that is selected rather than jumping to today — that is deliberate, and the dates on the selector now make it visible. A span inside one month names the month once where the language allows it ("Jul 28-30", "28-30 Jul") and spells both ends out where it does not ("7月28日 - 7月30日"). Bare years stay as digits in every language.

- **Reader post lists restyled**
  Reader post lists keep their content and actions, but the pieces around the posts have a Material 3 look: the "New posts" pill, the empty-state messages and their buttons, and the Subscribe buttons in the site and tag headers at the top of a blog or tag preview. When a blog or tag preview has no posts, its message now shows below the header instead of over it. A failed Subscribe or Unsubscribe from a site header now shows a snackbar instead of a toast.
  - **Apps:** Jetpack
  - **Type:** Changed
  - **Availability:** Everyone
  - **Where:** Reader → Subscriptions, Discover, Saved, Liked, Lists; tag and blog previews; Reader → Search → Posts
  - **Notes:** Subscribing from a site header shows the same "enable notifications" snackbar as subscribing from a post card. Older posts start loading a few rows before the end of the list rather than at the last row.

- **Archives card in Stats**
  A new Archives card lists the views a site's archive pages received, grouped by what they archive: search terms, categories, tags, authors, dates, errors, aggregated, "other", and one "Taxonomies" group for the site's custom taxonomies. Each group expands to show the individual pages, and tapping one opens it in the browser. Taxonomies expands one level further — a row per custom taxonomy, which expands to that taxonomy's terms. The card is not shown by default — add it from "Add card" at the bottom of the Traffic tab.
  - **Apps:** Both
  - **Type:** New
  - **Availability:** Flag: android_new_stats
  - **Where:** My Site → Stats → Traffic → Add card → Archives
  - **Notes:** Groups are ranked by total views, as are the pages within a group. The card lists ten pages under each group; "Show All" opens the full Archives screen, where every group can be expanded to its whole list, itself capped at 100 pages per group — a site with thousands of search terms sees its busiest ones, not all of them. The card requests the same amount of data as the Traffic page on the web, so its numbers match what a customer sees there; "Show All" asks for everything and so can report a higher total for the same group and period.

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
