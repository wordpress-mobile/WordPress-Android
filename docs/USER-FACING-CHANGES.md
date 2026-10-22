# User-Facing Changes

A rolling log of changes a customer could notice. Newest entries at the top.

What belongs here and how to write an entry: [`USER-FACING-CHANGES-DOCUMENTATION.md`](USER-FACING-CHANGES-DOCUMENTATION.md).

---

<!-- Add new entries directly below this line. -->

- **Archives card in Stats**
  A new Archives card lists the views a site's archive pages received, grouped by what they archive: search terms, categories, tags, authors, dates, errors, aggregated, "other", and one "Taxonomies" group for the site's custom taxonomies. Each group expands to show the individual pages, and tapping one opens it in the browser. Taxonomies expands one level further — a row per custom taxonomy, which expands to that taxonomy's terms. The card is not shown by default — add it from "Add card" at the bottom of the Traffic tab.
  - **Apps:** Both
  - **Type:** New
  - **Availability:** Flag: android_new_stats
  - **Where:** My Site → Stats → Traffic → Add card → Archives
  - **Notes:** Groups are ranked by total views, as are the pages within a group. The card lists ten pages under each group and "Show All" opens the group's full list, which is itself capped at 100 pages — a site with thousands of search terms sees its busiest ones, not all of them. The card requests the same amount of data as the Traffic page on the web, so its numbers match what a customer sees there; "Show All" asks for everything and so can report a higher total for the same group and period.
