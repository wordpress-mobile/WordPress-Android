# User-Facing Changes — Documentation

How to use [`USER-FACING-CHANGES.md`](USER-FACING-CHANGES.md): what belongs in it, and how to write an entry.

## What the log is

A running record of changes a customer could notice. It exists to answer two questions: *what changed*, and *is it live for customers yet*.

It is written for the people who document and support our apps, and for anyone — developer or AI assistant — reconstructing the app's user-visible state without reading the whole diff.

It is **not release notes**. Nothing here is copy that ships to customers.

## When to add an entry

Add one if a customer could experience something different because of your change:

- a new screen, setting, or capability
- a menu item or setting that moved, was renamed, or went away
- different steps to accomplish a task
- something that was broken and now works, where the old behaviour might be documented
- a feature becoming visible to people who couldn't see it before

That last case is the one people miss. Switching on a remote flag, widening a rollout, or enabling something server-side changes what customers see while touching no code here — open a pull request and add an entry anyway. The log tracks the app's user-visible state, not the commits that produced it.

Size is not the test. A refactor nobody could notice does not belong here, however large.

## How the log is organised

**New entries go at the top, always.** No version headings, no "unreleased" section, and nothing is moved, re-sorted, or edited after the fact.

Which release an entry shipped in is not recorded, because when you write it nobody knows — the cut happens when an internal build is produced, after your change lands. Git answers it instead: comparing the file between two commits gives you exactly what went into a build.

So when something about an earlier entry changes, add a new entry at the top rather than editing the old one. A release snapshot is copied out of this file; the file itself keeps everything.

## Entry format

Every field is a labelled line. Keep the description factual and specific — someone should be able to write a documentation page from it without opening the app.

```markdown
- **Short title**
  One or two sentences describing what a customer can now see or do.
  - **Apps:** Both
  - **Type:** New
  - **Availability:** Everyone
  - **Where:** Post editor → Add media → Google Photos
  - **Notes:** Anything that will surprise someone documenting this.
```

**Apps** — `WordPress`, `Jetpack`, or `Both`.

**Type** — one of:

| Type | Use it for |
| --- | --- |
| `New` | a screen, setting, or capability that did not exist before |
| `Moved` | something renamed, relocated, or removed |
| `Changed` | the steps to accomplish a task are different |
| `Fixed` | something that did not work now does, where the old behaviour may be documented |
| `Rollout` | who can see an existing feature changed; usually no code change |

**Availability** — whether a customer on this build can see the change *today*, not which release it ships in:

- `Everyone` — anyone running a build containing this change sees it
- `Flag: <flag-name>` — behind a remote or build flag that is not on for everyone
- `Internal` — internal or debug builds only

When a flag is later switched on, that is a new entry with `Type: Rollout`. Title it after the same feature so the two are recognisably related.

**Where** — required whenever there is somewhere in the app to point at. Support asks for this more than any other field, and it is the hardest to recover from a diff.

**Notes** — optional. Use it for the things that generate support tickets.

There is deliberately no field for a pull request link. Git already knows which commit added the line, and a number you can't look up until after you've opened the pull request is friction for nothing.

## Worked examples

```markdown
- **Excerpts no longer generated on update**
  Posts and pages with no excerpt keep it empty when you update them, instead of having one generated and saved.
  - **Apps:** Both
  - **Type:** Fixed
  - **Availability:** Everyone
  - **Where:** Post editor → Post settings → Excerpt
  - **Notes:** Documentation describing the old auto-fill behaviour is now wrong.

- **Redesigned Stats screen on for everyone**
  The redesigned Stats screen is now on for all users. It was previously behind a flag for a subset.
  - **Apps:** Both
  - **Type:** Rollout
  - **Availability:** Everyone
  - **Where:** My Site → Stats
  - **Notes:** No code change — the remote flag was switched on.

- **Google Photos in the media picker**
  You can browse Google Photos albums, collections, and search when adding photos or videos to a post.
  - **Apps:** Both
  - **Type:** New
  - **Availability:** Everyone
  - **Where:** Post editor → Add media → Google Photos
```
