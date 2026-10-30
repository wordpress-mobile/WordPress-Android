# User-Facing Changes

A rolling log of changes a customer could notice. Newest entries at the top.

What belongs here and how to write an entry: [`USER-FACING-CHANGES-DOCUMENTATION.md`](USER-FACING-CHANGES-DOCUMENTATION.md).

---

<!-- Add new entries directly below this line. -->

- **A post's Stats menu item opens the redesigned Stats**
  Posts → a published post's ⋯ menu → Stats now opens the per-item screen of the redesigned Stats, the same screen a row in Posts & pages opens, instead of the old per-post stats screen. The post's title, chart, totals and calendars are the ones the redesigned Stats shows everywhere else, and its header opens the post itself. A page's Stats menu item, where the Pages list offers one, opens the same screen.
  - **Apps:** Both
  - **Type:** Changed
  - **Availability:** Flag: android_new_stats
  - **Where:** My Site → Posts → a published post's ⋯ menu → Stats
  - **Notes:** Only for people on the redesigned Stats; anyone who has switched back to the old Stats keeps the old screen here too. The menu item itself is unchanged, and where it appears differs by app: in Jetpack it still shows only for a published post with no unsynced changes, on a site whose stats you can view; in WordPress it shows only on the newer Posts screen, for a published post on a WordPress.com-connected site whose stats you can view, with no unsynced-changes condition. Opening it for a site other than the one currently selected switches the selected site, as the Stats widgets already do. The Pages list offers a Stats menu item only on sites using the newer Pages screen, so most people see no change there yet.

- **Reader's Manage subscriptions screen redesigned**
  The screen for managing followed tags and blogs has a Material 3 look. The Followed tags and Subscribed blogs tabs still swipe, each row has a Subscribe/Subscribed button, the field for following a tag or URL stays at the bottom, and the + button still opens the topic picker. Searching subscribed blogs now uses a search icon in the top bar on the blogs tab. Errors from the Subscribe buttons in the lists now show as a snackbar instead of a toast.
  - **Apps:** Jetpack
  - **Type:** Changed
  - **Availability:** Everyone
  - **Where:** Reader → Subscriptions → the Blogs or Tags chip → Manage
  - **Notes:** Unsubscribing from a row leaves it in place so it can be re-followed, but only until the list refreshes: the row goes once the screen reloads its lists, for example on returning from a blog preview, after adding a tag or blog, or when a background sync finishes. The old screen behaved the same way.

- **Open a post's likes or comments from its Stats totals**
  The Likes and Comments totals on the top card of the per-item screen in the redesigned Stats screen are now tappable. Likes opens the full list of likers — the same list the likers row above it opens, and the one the Reader and notifications open. Comments opens that post's comments, the threaded comments screen the Reader and notifications open, where you can read, like and reply to them. A total you can open is shown in the same colour as the item's title, so the ones that lead somewhere are visible at a glance.
  - **Apps:** Both
  - **Type:** New
  - **Availability:** Flag: android_new_stats
  - **Where:** My Site → Stats → Traffic → Posts & pages → tap a row (or Show all → tap a row); My Site → Stats → Insights → Latest post
  - **Notes:** A total of zero stays a plain figure and does nothing, since there is nothing to open. Comments is tappable only for the items whose title opens in the Reader: pages and attachments have no Reader representation, and an item that is a draft, scheduled or trashed has no public permalink to open. The Views total is never tappable.

- **Avg. Views Per Day is a calendar grid**
  The Avg. Views Per Day card on the per-item screen in the redesigned Stats screen now draws each year as twelve squares, January to December, shaded by the item's average daily views that month — the same grid the two cards above it use. An Average row under the grid gives the item's average over its whole life. This replaces the list of yearly averages, and it is what wordpress.com shows under "Average per day".
  - **Apps:** Both
  - **Type:** Changed
  - **Availability:** Flag: android_new_stats
  - **Where:** My Site → Stats → Traffic → Posts & pages → tap a row (or Show all → tap a row); My Site → Stats → Insights → Latest post
  - **Notes:** The monthly averages are the server's own figures, and they are whole numbers — a month that averaged less than one view a day shows as an empty square, even where Months and Years above shows views for it. Average is the server's figure for an item that spans a single year; over several years the server reports one per year only, so it is worked out as the views the item has had over the days it has existed.

- **Months and Years is a calendar grid**
  The Months and Years card on the per-item screen in the redesigned Stats screen now draws each year as twelve squares, January to December, shaded from pale to solid by how many views that month got — the same grid the Recent Weeks card above it uses. The current year is always shown, even for an item that has had no views this year. A Total row under the grid adds the years up. This replaces the list of year totals.
  - **Apps:** Both
  - **Type:** Changed
  - **Availability:** Flag: android_new_stats
  - **Where:** My Site → Stats → Traffic → Posts & pages → tap a row (or Show all → tap a row); My Site → Stats → Insights → Latest post
  - **Notes:** The twelve months are drawn as two rows of six, because twelve across leaves each square too narrow to write a figure in on a phone. Months the server left out — including the ones that have not happened yet — are drawn as empty squares, so the columns mean the same month in every year. Each square with views names its month, year and count to a screen reader. The shading scale is shared with Recent Weeks but the two grids are scaled separately, each against its own busiest period.

- **Recent Weeks is a calendar grid**
  The Recent Weeks card on the per-item screen in the redesigned Stats screen now draws each week as a row of seven squares, one per day, shaded from pale to solid by how many views that day got. The busiest day of the range is the darkest square, a "Less … More" key sits under the grid, and a Total row under that adds the weeks up. Each square names its date and view count to a screen reader. This replaces the list of week totals with their week-on-week percentages.
  - **Apps:** Both
  - **Type:** Changed
  - **Availability:** Flag: android_new_stats
  - **Where:** My Site → Stats → Traffic → Posts & pages → tap a row (or Show all → tap a row); My Site → Stats → Insights → Latest post
  - **Notes:** The weeks start on whichever day your language starts the week on — Sunday in US English, Monday in most of Europe — not on the Monday the server buckets its weeks from. Days that have not happened yet, in the week still running, are drawn as empty squares so the columns line up by weekday; the days at the far end, before the range the server sent, are left blank instead, since the item may well have been read on them. The grid no longer shows each week's own total or its change against the week before; it shows per-day counts, plus one total for the whole range.

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

- **Reader articles no longer get stuck on a spinner**
  Opening a post in the Reader shows the article text without waiting on a network request for its stylesheet, so a slow or stalled connection no longer leaves a blank body behind a spinner that never stops. The comments snippet, tags and likers below the article also appear as soon as the article itself is on screen, instead of waiting for every embed and iframe in the post to finish loading.
  - **Apps:** Both
  - **Type:** Fixed
  - **Availability:** Everyone
  - **Where:** Reader → any post
  - **Notes:** The stylesheet is now bundled with the app rather than fetched from wordpress.com, so the article body renders identically offline and online. Posts that were affected needed the app restarting or the post reopening to recover; that workaround is no longer documented behaviour.

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
