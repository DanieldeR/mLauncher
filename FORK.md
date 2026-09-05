# Fork notes

This is a fork of [CodeWorksCreativeHub/mLauncher](https://github.com/CodeWorksCreativeHub/mLauncher),
kept close enough to upstream that it can be rebased onto it periodically.

## What this fork adds

| Feature | Behaviour |
| --- | --- |
| Enter searches the web | Pressing enter in the app drawer opens the configured search engine when no app matches, instead of the Play Store. |
| AI search button | An optional third button in the search row hands the query to a locally installed AI app (Claude, Gemini, ChatGPT, ...), chosen in settings. |
| Search bar at the bottom | The app drawer search row sits above the keyboard rather than at the top of the screen, with the result list stacked upwards. Configurable, defaults to bottom. |

Behaviour intentionally dropped from upstream: the first-open tip in the app drawer is no longer
shown. The view is still in the layout; only the observer that made it visible is gone.

## How the fork is structured

Everything the fork owns lives in files upstream does not have:

| Path | Contents |
| --- | --- |
| `app/src/main/java/com/github/codeworkscreativehub/fork/ForkPrefs.kt` | `SearchBarPosition`, and the fork's settings as extension properties on `Prefs`. |
| `app/src/main/java/com/github/codeworkscreativehub/fork/AiSearch.kt` | Handing a query to an installed AI app; deep link first, share as fallback. |
| `app/src/main/java/com/github/codeworkscreativehub/fork/DrawerSearchBar.kt` | Bottom placement, IME tracking, reversed lists, and the AI button. |
| `app/src/main/java/com/github/codeworkscreativehub/fork/ForkSettings.kt` | The fork's settings rows as one composable. |
| `app/src/main/res/values/fork.xml` | The fork's strings and dimens. |
| `app/src/main/res/drawable/ic_ai_search.xml` | The AI button icon. |
| `tools/sync-upstream.sh` | Rebase helper. |

Upstream files carry call sites only. Rules to keep it that way:

- New settings go in `ForkPrefs.kt` as `Prefs` extensions, never in `Prefs.kt`/`PrefsKeys.kt`.
- New strings, dimens and colours go in `res/values/fork.xml`, never in `strings.xml`/`dimens.xml`.
  `strings.xml` is upstream's hottest file (~20 commits a year) and is also Crowdin managed.
- New views go in through code (`DrawerSearchBar.attachAiSearchButton`), not by editing a layout.
- Fork behaviour that only applies in a non-upstream mode should no-op otherwise, so the upstream
  path stays untouched. `DrawerSearchBar.attach()` returns immediately when the search bar is at
  the top, leaving upstream's own inset handling in charge.

## What the fork still changes in upstream files

Checked against upstream `main`; the churn column is upstream commits touching that file in the
last 12 months, so it is roughly how likely each anchor is to conflict.

| File | Lines +/- | Churn | Anchors the fork depends on |
| --- | --- | --- | --- |
| `ui/AppDrawerFragment.kt` | +22 / -16 | 7 | `DrawerSearchBar` field and `attach()` after the `binding.apply { ... }` block; `layoutManager()` for both recyclers; four `canScrollVertically` call sites in the two scroll listeners; `attachAiSearchButton` in the `LaunchApp` branch; the enter-to-search branch in `onQueryTextSubmit`; the deleted `firstOpen` tip observer. |
| `ui/SettingsFragment.kt` | +7 / -3 | 8 | Third entry in `appListButtonOptionLabels`; `"00"` → `"000"` in both `APPLIST_BUTTON_FLAGS` defaults; one `ForkSettings(...)` call after the search engine row. |
| `ui/components/AZSidebarView.kt` | +15 / -1 | 2 | `reversed` property and `applyLetterOrder()`, so the sidebar can draw bottom up. |

If upstream rewrites the app drawer's inset handling or its scroll listeners, those are the two
places to re-read carefully; everything else is additive.

## Rebasing onto upstream

```sh
tools/sync-upstream.sh                      # current branch onto upstream/main
tools/sync-upstream.sh my-branch main       # or name them explicitly
```

The script refuses to run on a dirty tree, enables `git rerere` so a conflict resolved once is
replayed next time, adds the `upstream` remote if missing, tags a `backup/<branch>-<timestamp>`
branch, and rebases. It never pushes.

Doing it by hand:

```sh
git config rerere.enabled true
git remote add upstream https://github.com/CodeWorksCreativeHub/mLauncher.git
git fetch upstream main
git rebase upstream/main
```

Keep the fork as separate commits, one per feature, rather than squashing: a feature that upstream
implements itself can then be dropped during the rebase with `git rebase --onto`, or by deleting its
line in an interactive rebase.

After a rebase, before force pushing:

```sh
./gradlew assembleProdDebug     # CI builds assembleProdRelease
./gradlew testProdDebugUnitTest # the repo has one test class
```

Then check by hand, since none of this is covered by tests:

- App drawer opens with the search bar above the keyboard, results stacked upwards.
- Enter with no matching app opens the search engine; enter on an exact match still launches it.
- The AI button appears when enabled in Settings → App List Buttons and hands a query over.
- Settings → Search Bar Position switches between top and bottom, and top still matches upstream.
- The A-Z sidebar letters line up with the list in both positions.
