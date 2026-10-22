# User-Facing Changes

A rolling log of changes a customer could notice. Newest entries at the top.

What belongs here and how to write an entry: [`USER-FACING-CHANGES-DOCUMENTATION.md`](USER-FACING-CHANGES-DOCUMENTATION.md).

---

<!-- Add new entries directly below this line. -->

- **Experimental Features moved to App Settings**
  Experimental Features is no longer a row on the Me screen. It is now a row in App Settings, below What's New.
  - **Apps:** Both
  - **Type:** Moved
  - **Availability:** Everyone
  - **Where:** Me → App Settings → Experimental Features
  - **Notes:** Debug Settings (internal/debug builds only) moved with it, to Me → App Settings → Debug Settings.

- **Me screen regrouped into cards**
  The Me screen rows are now grouped into four cards: My Profile, Account Settings, Scan Login Code and App Settings; Help & Support; Share and About; Domains and Log out. Log out is now the last row. The "Your WordPress.com profile is powered by Gravatar" card at the bottom of the screen is gone.
  - **Apps:** Both
  - **Type:** Changed
  - **Availability:** Everyone
  - **Where:** Me (Jetpack: Me tab; WordPress: My Site → Me)
  - **Notes:** The removed card held the only "What is Gravatar?" link on the Me screen. Rows keep their existing visibility rules: Share only appears when the `recommend_the_app_enabled` remote flag is on, and Domains is Jetpack-only.

- **Reader search redesigned**
  Reader search has a new layout: the search field sits in the top bar, recent searches show as one list as you type (each with a delete button, plus "Clear search history" at the end), and the Posts and Sites tabs appear as soon as a search is submitted. A failed post search now shows "Unable to perform search" in the Posts tab instead of a blank screen.
  - **Apps:** Jetpack
  - **Type:** Changed
  - **Availability:** Everyone
  - **Where:** Reader → Search
  - **Notes:** Recent searches are filtered by what has been typed so far. Clearing the search field returns to the recent searches. The Posts and Sites tabs each keep their scroll position when switching between them.
