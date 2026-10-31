# Hridu's Great Rescue (repo: hridu-math)

A kids' adventure game for Hridaan ("Hridu", age 8). He flies Zippy the toy
plane, rides animals and rescues zoo friends. After every few minutes of play
(5 by default) a **Puzzle Pit Stop** pauses the game: one math question and one
brain puzzle (pattern, odd-one-out, counting, opposites, what-comes-next),
both multiple choice. Main target is Google TV / Android TV; the same game also
runs in any browser on phones and laptops.

## Layout
- `web/index.html` — the whole game, one file, plain canvas JS, no build step.
  Logical canvas 1280x720, letterboxed to any screen.
- `app/` — tiny Android wrapper (Java, one `MainActivity` WebView). Gradle
  bundles `../web` as the APK's assets, so the APK and the web build are
  always the same game.
- `keystore/debug.keystore` — fixed debug signing key (`android` /
  `androiddebugkey`) so new builds install over old ones.
- `.github/workflows/build.yml` — builds the APK on every push to `main` and
  publishes a GitHub Release `build-<run>` with `hridu-great-rescue.apk`.
- `.github/workflows/pages.yml` — publishes `web/` to GitHub Pages.

## Controls
- TV remote / keyboard: arrows, OK/Enter/Space, Back/Esc (pause or up a screen).
- Touch: on-screen Jump/Duck (run) or ▲▼/Zoom (fly) buttons, pause button.
- Android Back is forwarded to `window.hriduBack()`; it only exits the app from
  the title screen.

## Game structure
- 8 levels on an adventure map, unlocked in order: 1 Meadow Hop (run, Bibi the
  bunny), 2 Over the Hills (fly), 3 Banana Jungle (run, Chiku the cheetah),
  4 Cloud Maze (fly, birds), 5 Island Rescue (rescue 4), 6 Desert Dash (run,
  Kanta the camel), 7 Storm Flight (fly, wind), 8 Big Zoo Parade (rescue 6),
  then the ending. Story cards before/after each level; villain is Grumble the
  lonely storm cloud, puzzle host is Professor Hoot the owl.
- Puzzle difficulty bands by level: 1–2 add/sub to 20 · 3–4 to 100 ·
  5–6 tables 2/5/10 + word problems · 7–8 tables to 10, missing numbers, sharing.
- Wrong answers show a hint and grey out that choice; first-try answers give
  bonus stars. Pit stops can't be skipped.
- Progress, stars, play-clock and settings persist in localStorage
  (`hridu-math-v1`). Title screen has a grown-up setting for the puzzle
  interval (3/5/8/10 min) and sound.
- Test URL params: `?break=30` (puzzle every 30 s), `?level=N` (unlock to N).

## Install on the TV
The TV can't use ADB. Install the **Downloader** app on the TV, then enter the
APK link from the latest Release:
`https://github.com/rajatchatterjee/hridu-math/releases/latest/download/hridu-great-rescue.apk`

## Gotchas
- No local Android build in Claude sessions (Google Maven is unreachable);
  GitHub Actions is the compiler — push, then check the Actions run.
- Fonts: Fredoka loads from Google Fonts when online; offline it falls back to
  system sans. Nothing else needs the network.
- Hridu's look (curly black hair, round face, navy zip top with sky-blue
  sleeves) was drawn from a family photo shared in chat; the photo itself is
  intentionally NOT in the repo (public repo).

## History
- 2026-10-31: First version — 3 game modes, 8 levels with story, puzzle pit
  stops, TV/phone/laptop controls, Android TV wrapper, APK Release + Pages.
