# User-Facing Changes

A rolling log of changes a customer could notice. Newest entries at the top.

What belongs here and how to write an entry: [`USER-FACING-CHANGES-DOCUMENTATION.md`](USER-FACING-CHANGES-DOCUMENTATION.md).

---

<!-- Add new entries directly below this line. -->

- **Recent Weeks is a calendar grid**
  The Recent Weeks card on the per-item screen in the redesigned Stats screen now draws each week as a row of seven squares, one per day, shaded from pale to solid by how many views that day got. The busiest day of the range is the darkest square, and a "Less … More" key sits under the grid. Each square names its date and view count to a screen reader. This replaces the list of week totals with their week-on-week percentages.
  - **Apps:** Both
  - **Type:** Changed
  - **Availability:** Flag: android_new_stats
  - **Where:** My Site → Stats → Traffic → Posts & pages → tap a row (or Show all → tap a row); My Site → Stats → Insights → Latest post
  - **Notes:** The weeks run Monday to Sunday, following what the server sends. The first and last weeks of the range are usually cut short — by the day the item was published and by the days that have not happened yet — and those days are drawn as empty squares so the columns still line up by weekday. The grid no longer shows each week's total or its change against the week before; it shows per-day counts instead.

- **See who liked a post from its Stats screen**
  The top card of the per-item screen in the redesigned Stats screen now shows the faces of the people who liked the item, with a "46 Likes" row beside them. Tapping that row opens the full list of likers — the same list the Reader and notifications open — where tapping someone shows their profile. The card also now reads "Published <date>" with a small link icon, and the views, likes and comments totals sit under a rule in larger type.
  - **Apps:** Both
  - **Type:** New
  - **Availability:** Flag: android_new_stats
  - **Where:** My Site → Stats → Traffic → Posts & pages → tap a row (or Show all → tap a row); My Site → Stats → Insights → Latest post
  - **Notes:** The likes row appears only for items with at least one like, so never for the site's home page. The faces arrive in a second request after the screen loads; if it fails the row still shows the count, which comes from the stats response. Opened this way the likers list has no post header at the top, because Stats knows only the item's id and like count — the Reader and notifications still show theirs.

- **Per-item Stats screen is laid out in cards**
  The per-item screen in the redesigned Stats screen is now built from the same cards as the rest of Stats, instead of being one flat list on the window background. It has five cards: the item's title, date and totals; the selected day's views with its chart; Recent Weeks; Months and Years; and Avg. Views Per Day. The information shown and what you can do with it are unchanged.
  - **Apps:** Both
  - **Type:** Changed
  - **Availability:** Flag: android_new_stats
  - **Where:** My Site → Stats → Traffic → Posts & pages → tap a row (or Show all → tap a row); My Site → Stats → Insights → Latest post

- **Open the post or picture from its Stats screen**
  On the per-item screen in the redesigned Stats screen, the title is shown in the link colour with an arrow icon after it, and tapping anywhere across the title and the publication date below it opens the item: a post opens in the Reader, and a page or a media attachment opens in the browser. Attachments are usually titled with bare numbers, so this is what tells you which picture a row is about.
  - **Apps:** Both
  - **Type:** Fixed
  - **Availability:** Flag: android_new_stats
  - **Where:** My Site → Stats → Traffic → Posts & pages → tap a row (or Show all → tap a row); My Site → Stats → Insights → Latest post
  - **Notes:** The old Stats screen offered the same thing by making the "Showing stats for:" header tappable. The title stays plain and inert, with no arrow, for the site's home page, when WordPress.com supplies no link for the item, and when the item is no longer public — a post that was published, picked up views and was later moved back to a draft or to the trash keeps its view history here, but its link would no longer open.

- **Re-arranged items in the settings screen.**
  - **Apps:** Both
  - **Type:** Changed
  - **Availability:** Everyone
  - **Where:** Me

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
