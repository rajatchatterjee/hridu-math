# Hridu's Great Rescue (repo: hridu-math)

A kids' adventure game for Hridaan ("Hridu"). He flies Zippy the toy
plane, rides animals and rescues zoo friends. After every few minutes of play
(5 by default) a **Puzzle Pit Stop** pauses the game: one math question and one
brain puzzle (pattern, odd-one-out, counting, opposites, what-comes-next),
both multiple choice. Main target is Google TV / Android TV; the same game also
runs in any browser on phones and laptops.

## Layout
- `web/index.html` — the whole game, one file, plain canvas JS, no build step.
  Logical canvas 1280x720, letterboxed to any screen.
- `web/manifest.webmanifest`, `web/sw.js`, `web/icons/` — make the web build an
  installable app (PWA): full-screen, landscape, works offline. Title screen
  shows "Install as an app" when the browser allows it (Chrome/Edge prompt,
  iPhone/iPad gets "Share → Add to Home Screen"). Hidden inside the TV APK.
  Icons are rendered from the game's own drawing code (Zippy + Hridu).
- `app/` — tiny Android wrapper (Java, one `MainActivity` WebView). Gradle
  bundles `../web` as the APK's assets, so the APK and the web build are
  always the same game.
- `keystore/debug.keystore` — fixed debug signing key (`android` /
  `androiddebugkey`) so new builds install over old ones.
- `.github/workflows/build.yml` — builds the APK on every push to `main` and
  publishes a GitHub Release `build-<run>` with `hridu-great-rescue.apk`.
- `.github/workflows/pages.yml` — publishes `web/` to GitHub Pages, renaming
  the game to `hridu-great-rescue.html`; the site's bare `/hridu-math/` address
  is a generated redirect to it. (In the repo the file stays `web/index.html`
  because the APK loads that.) Live: https://rajatchatterjee.github.io/hridu-math/hridu-great-rescue.html

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
- Math is ONLY two-digit addition and subtraction (both numbers 10–99,
  answers 10–99) — Rajat's rule. Bands by level: 1–2 no carrying/borrowing ·
  3–4 mixed + short story problems · 5–8 mostly carrying/borrowing, plus
  missing-number (`45 + ? = 72`) and story forms. Hints walk through the
  carry/borrow step with the actual digits.
- Every question must be new: `freshQuestion()` keys each question and skips
  any of the last 600 seen (stored in `save.seen`); puzzle kinds rotate through
  a shuffled bag (number pattern, shape pattern, odd-one-out, counting,
  what-comes-next, opposites, biggest/smallest). If a small word pool runs dry
  it falls back to an endless kind (number/shape patterns, counting).
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
- Pages **Source must be "GitHub Actions"** (Settings → Pages). On "Deploy from
  a branch", GitHub's own "pages build and deployment" also runs and publishes
  the README instead of the game, giving a 404 for hridu-great-rescue.html.
  pages.yml ends with a "Check the game is live" step that fails if the page
  isn't served. After a fix, GitHub can keep a cached 404 for ~10 min —
  test with a `?v=N` cache-buster.
- No local Android build in Claude sessions (Google Maven is unreachable);
  GitHub Actions is the compiler — push, then check the Actions run.
- Fonts: Fredoka loads from Google Fonts when online; offline it falls back to
  system sans. Nothing else needs the network.
- Hridu's look (curly black hair, round face, navy zip top with sky-blue
  sleeves) was drawn from a family photo shared in chat; the photo itself is
  intentionally NOT in the repo (public repo).

## History
- 2026-10-31: Web game now lives at `/hridu-great-rescue.html` (Rajat's ask);
  old folder address redirects; installed app starts there.
- 2026-10-31: Age removed from story/docs. Web build is now an installable
  PWA (manifest, offline service worker, icons, install button). Math limited
  to two-digit +/−; no repeated questions; bigger opposites list.
- 2026-10-31: First version — 3 game modes, 8 levels with story, puzzle pit
  stops, TV/phone/laptop controls, Android TV wrapper, APK Release + Pages.
