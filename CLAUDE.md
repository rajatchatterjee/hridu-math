# Hridu's Great Rescue (repo: hridu-math)

A kids' adventure game for Hridaan ("Hridu"; "Heidi" in voice-typed messages
means Hridu). 28 levels in 5 chapters: he flies Zippy, rides animals, dives
with a dolphin, pilots a rocket, solves Number Gates, catches things and
escapes mazes. After every few minutes of play (3 or 5) a **Puzzle Pit Stop**
pauses the game: 2 math problems + 1 brain puzzle. Math and number puzzles
are TYPED; picture/word puzzles are multiple choice with ONE try. Main target is Google TV / Android TV; the same game also
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

## Voice-over, music, outfits
- **Voice-over** (`Voice` in index.html): story cards, pit-stop intro,
  questions, hints and praise are read aloud with the device's own TTS —
  `speechSynthesis` on the web, Android `TextToSpeech` in the APK via the
  `window.HriduTTS` bridge in MainActivity (WebView has no speechSynthesis;
  manifest declares the TTS_SERVICE `<queries>` for Android 11+). Prefers an
  en-IN voice. Each character has its own pitch/rate (`VOICES`).
- **iPhone/iPad (Safari, home-screen app)**: Safari blocks speech until a line
  is spoken from a tap. `Voice.unlock()` runs on the first tap/key anywhere:
  speaks a silent line, sets `navigator.audioSession.type='playback'` (sound
  even with the silent switch on), resumes WebAudio, then re-reads the line
  that was waiting. Safari also drops a line spoken right after `cancel()`,
  so `say()` waits 120 ms after a cancel; `stop()` only cancels if talking.
- **Can't be skipped** (Rajat's rule): a story card only advances once the
  voice has finished (Android signals `hriduVoiceDone(id)`); Back never skips
  story (it only leaves a level intro to the map). In pit stops, answering is
  locked until the question has been read. If no voice is available, the
  same lock runs on a reading-time timer (`Voice.done()`).
- **Music** (`Music`, `STYLES`, `buildTune`): generated in code with WebAudio,
  no files. Every level has its OWN tune: a style picked from its world/game
  type (`styleForLevel`: meadow bouncy, jungle drums, desert eastern scale,
  island calypso, storm driving minor, reef dreamy, snow bells, space
  whole-tone, mazes sneaky, Number Gates quiz-show, castle march, level 28
  finale) + a melody/rhythm/tempo generated from the level number (seeded),
  with synthesized kick/snare/hat and bass. Title/map/settings play 'title',
  pit stops the calm 'think' tune; a level's stories keep the level's tune
  (`Story opts.levelN`). Ducks while someone is talking; on/off in Settings.
- **Outfits** (`OUTFITS`): Space suit (default, from his real top), Pilot
  jacket, Superhero, Cricket kit, Festive kurta, Explorer, Dino hoodie.
  Chosen in Settings (big preview); used everywhere Hridu is drawn.
- **Settings screen**: outfit, puzzle timer (3/5 min), sound effects, music.
  Title screen is Play / Settings / Install as an app.

## Controls
- TV remote / keyboard: arrows, OK/Enter/Space, Back/Esc (pause or up a screen).
- Touch: on-screen Jump/Duck (run) or ▲▼/Zoom (fly) buttons, pause button.
- Android Back is forwarded to `window.hriduBack()`; it only exits the app from
  the title screen.

## Game structure
- **Chapter 1 · Sunnyvale Zoo (1–8):** 1 Meadow Hop (run, Bibi the bunny),
  2 Over the Hills (fly), 3 Banana Jungle (run, Chiku the cheetah), 4 Cloud
  Maze (fly, birds), 5 Island Rescue (rescue 4), 6 Desert Dash (run, Kanta the
  camel), 7 Storm Flight (fly, wind), 8 Big Zoo Parade (rescue 6) → ending 1.
  Villain Grumble the lonely storm cloud (becomes a friend).
- **Chapters 2–5 (9–28)** — Ziggy the raccoon steals the zoo's Golden Numbers:
  - Ch2 Ocean Quest (needs 16★): 9 Coral Dive (dive), 10 Number Clouds (gates),
    11 Shell Catch (catch), 12 Sunken Maze (maze), 13 Shark Bay Rescue (8).
  - Ch3 Snowy Mountains (28★): 14 Snowy Slopes (run, combos), 15 Ice Cave
    Maze, 16 Avalanche Flight (fly, wind), 17 Number Peaks (gates, two-step),
    18 Snowflake Catch.
  - Ch4 Space Adventure (40★): 19 Rocket Launch (space), 20 Moon Maze,
    21 Star Catch (math catch: catch the ball with the right answer),
    22 Asteroid Alley, 23 Planet Gates.
  - Ch5 Ziggy's Cloud Castle (52★): 24 Thunder Run (run, combos), 25 Castle
    Maze, 26 Lightning Rescue (10), 27 Number Storm (gates), 28 Grand Finale
    (fly) → ending 2 (Ziggy becomes a friend).
  - Chapter star gates (`CHAPTERS[].need`) force replaying levels well:
    stars = hearts left (1–3). Map shows one chapter per page; ◀ ▶ across.
- **Game types** (`Play`, `L.mode`): run · fly · rescue · dive (dolphin, fly
  physics, currents = `wind`) · space (rocket, asteroids) · gates (3 lanes,
  fly through the cloud with the right answer to the sum on screen; wrong =
  a bump) · catch (◀ ▶ basket; `mathCatch` = catch the number ball with the
  right answer) · maze (generated each time; collect all items, dodge
  patrolling guards, then reach the exit; pac-man style movement).
- **Collect-to-finish (Rajat's rule: every level needs X items):** run/fly/
  dive/space levels have a target (`artifactNeed`: ~55% of treats in run
  levels, ~45% of rings/stars/pearls/fuel elsewhere; override with `L.need`).
  Counter top-left. Reaching the finish line short of it keeps the level
  going ("Collect N more…") until met. Number Gates need N CORRECT answers
  (wrong ones don't count); rescue/catch/maze already require their goal.
- **In-game sums are EASY (Rajat: Number Clouds was too hard):** `gateQuestion`
  — SINGLE digits only (1–9 + 1–9, or a − b with a ≤ 9), never two-step,
  wrong choices are ±1–3. (The two-digit rule applies to pit stops only.) Gates move slowly (~6 s to read each
  sum), goals 8–12. A wrong cloud/ball does NOT cost a heart; stars come from
  first-time accuracy (≥80% → 3★, ≥60% → 2★), so guessing still only earns 1★.
  Harder sums (carrying, missing numbers, two-step) live in the pit stops.
- **No edge camping (he hid at the top of the screen):** in fly/dive/space/
  rescue the top (and bottom, except rescue) edges are hazards — lingering
  >0.45 s costs a heart and bounces him back; ~35% of obstacles spawn at his
  current height (`aimY`). A top-camping bot loses all hearts in seconds and
  collects ~7 of 35–47 needed items.
- **Mid-level stories:** every level 9+ has `mid` lines that interrupt play at
  50% progress as an unskippable voiced Story ("Along the way…"); chapter
  openers (`CHAPTER_INTRO`) play once before levels 9/14/19/24.
- Story cards before/after each level; puzzle host is Professor Hoot the owl.
- Math is ONLY two-digit addition and subtraction (both numbers 10–99,
  answers 10–99) — Rajat's rule. Bands by level: 1–2 no carrying/borrowing ·
  3–4 mixed + short story problems · 5–8 mostly carrying/borrowing · 9–16 +
  missing-number (`45 + ? = 72`) and story forms · 17–28 + two-step sums
  (`34 + 25 − 18`, every in-between result also 10–99).
- **Column maths (Rajat's ask, `teachSteps` + `PitStop.drawColumn`):** every
  pit-stop add/subtract (plain, story, missing-number) is shown stacked —
  tens under tens (blue), ones under ones (orange), a line, and the two
  answer boxes under it (missing-number: boxes in the second row, total
  under the line). Hoot then walks through the METHOD, one voiced step at a
  time, with the active column glowing: digits fly down from the written
  sum into the column → "start with the ones" → carry (a "1 ten" bubble hops
  over to the top of the tens) or borrow (tens digit crossed out and one
  less written above, a "10" hops to the ones, ones digit crossed out and
  +10 written above) → "then the tens" (+ "the 1 we carried") → "Now you
  work it out!". It never writes or says an answer digit or a column result.
  Typing is locked until the walkthrough ends (it can't be skipped). First
  wrong answer replays the steps; second → a different question as before.
  Two-step sums keep the plain layout.
- **Any order (Rajat's ask):** in column sums he fills the ones or the tens
  box first, whichever he likes (`slots`, `slot`). It starts on the ones box
  (the method starts there); after a digit the cursor jumps to the other
  empty box. Touch: tap a box to choose it. TV: ◀ from the pad's left
  column moves into the boxes (◀ ▶ pick tens/ones, OK/▶ back to the pad);
  remote number keys type into the chosen box. ⌫ clears the chosen box (or
  the other one if it's empty). ✓ with one box empty asks for the other.
- Every question must be new: `freshQuestion()` keys each question and skips
  any of the last 600 seen (stored in `save.seen`); puzzle kinds rotate through
  a shuffled bag (number pattern, shape pattern, odd-one-out, counting,
  what-comes-next, opposites, biggest/smallest). If a small word pool runs dry
  it falls back to an endless kind (number/shape patterns, counting).
- Math answers are typed (Rajat's rule: no multiple choice for math): two
  answer boxes + an on-screen 3x4 number pad (1–9, ⌫, 0, ✓). TV: D-pad moves
  around the pad, OK presses, remote number keys and Back (deletes a digit)
  also work. Laptop: type digits, Backspace, Enter. Phone: tap the pad.
- **Anti-guessing (Rajat's rules — he caught Hridu gaming both):**
  - The answer is NEVER shown or spoken, anywhere (pit stops, gates, catch).
    Hints describe the method only, with no worked-out numbers; a check makes
    sure a puzzle hint never accidentally contains its answer.
  - Typed: wrong → method hint + 3 s think-time lock; 2nd wrong → a DIFFERENT
    question of the same kind (never the answer).
  - Multiple choice: ONE try; wrong → hint + a different puzzle. The correct
    option is never highlighted after a miss.
  - Number puzzles (patterns, counting) are typed, not multiple choice.
  - First-try answers give bonus stars. Pit stops can't be skipped.
- Progress, stars, play-clock and settings persist in localStorage
  (`hridu-math-v1`). Title screen has a grown-up setting for the puzzle
  interval — ONLY 3 or 5 min (Rajat's rule; older saves with 8/10 reset to 5)
  — and sound.
- Test URL params: `?break=30` (puzzle every 30 s), `?level=N` (unlock to N).

## Install on the TV
The TV can't use ADB. Install the **Downloader** app on the TV, then enter the
APK link from the latest Release:
`https://github.com/rajatchatterjee/hridu-math/releases/latest/download/hridu-great-rescue.apk`

**Updates after that happen inside the app** (build 9+): on launch the app
asks GitHub's API for the latest Release (`HriduApp.checkUpdate`); if its
`build-N` is higher than the installed versionCode, the title screen shows
"Update to new version" (blue button + "v1.X · new: v1.Y" bottom-right).
Choosing it downloads the APK straight into Android's PackageInstaller
session and opens the system "Update" confirmation. First time only, Android
asks to allow "Install unknown apps" for the game (REQUEST_INSTALL_PACKAGES).
Status comes back to the page via `hriduUpdateStatus(state, detail)`.

Why: re-installing through Downloader kept failing on the TV even though all
builds share the same signing key and rising versionCodes (verified builds
1/7/8: same cert SHA-256 54:EC:6E…, versionCode = run number). Downloader
likely hands over a stale cached copy of the same-named file, which Android
rejects as a downgrade.

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

## Celebrations
- `FX` (global confetti + firework particles, drawn over any scene in the frame
  loop) and `SFX.fanfare/cheer/ding/boom`.
- **Level complete** (`LevelDone`): sunburst, fireworks, confetti, stars slam in
  one by one with a ding, bouncing praise (PERFECT!/GREAT JOB!/YOU DID IT!),
  score count-up, Hridu jumping with the level's buddy (`buddyOf`: mount,
  Zippy, Dolly, rocket or Hoot, who also says the voice line), fanfare +
  applause, then the 'party' tune. OK works after 4 s.
- **Chapter complete** (last level of each chapter): bigger version with a
  golden trophy, "CHAPTER COMPLETE!", parade of friends; OK after 6 s.
- In play: "TARGET REACHED!" banner when the collect target is met (maze:
  "DOOR OPEN!"); confetti + fireworks at the finish line; pit stop "Fuel tank
  full!" gets fireworks, confetti and a fanfare.

## History
- 2026-11-05: Edges made kinder in fly/space/dive/rescue levels (Rajat: Avalanche
  Flight too hard): thinner danger zone, soft push back out, a heart is lost only
  after lingering ~1.3 s (was 0.45 s); wind halved and fades near the edges so it
  never shoves Hridu into one. Camping at an edge still costs hearts.
- 2026-11-02: Every typed answer (two-step sums, number puzzles too, not just
  column sums) uses labelled tens + ones boxes; starts on ones, any order, works
  with the remote (◀ from the pad's left column picks a box). One-digit puzzle
  answers can leave tens empty.
- 2026-11-01: Column sums: fill the ones or tens box in any order.
- 2026-11-01: Pit-stop sums shown in columns with an animated, voiced
  walkthrough of carrying and borrowing (method only, never the answer).
- 2026-10-31: Fix: no voice-over on iPhone/iPad (Safari speech unlock on first tap,
  delay after cancel, play through the silent switch).
- 2026-10-31: Grand celebrations after every level, chapter, target and pit stop.
- 2026-10-31: Number Gates / Star Catch sums are single digits only.
- 2026-10-31: Number Gates / Star Catch made easier (no-carry sums, ±10/20
  choices, slower, fewer); wrong answers don't cost hearts; stars by accuracy.
- 2026-10-31: Every level needs X items collected to finish; Number Gates need
  N correct answers; dangerous screen edges + aimed obstacles stop camping.
- 2026-10-31: Different music for every level (30 unique generated tunes with
  drums and bass), styled to each world and game type.
- 2026-10-31: Fix: saves from before chapters 2–5 had `unlocked` capped at 8; on
  load, unlock the level after the highest completed one.
- 2026-10-31: 20 new levels (9–28) in 4 chapters with star gates, Ziggy story
  arc, unskippable mid-level stories; 5 new game types (dive, space, gates,
  catch, maze); pit stops = 2 math + 1 puzzle; anti-guessing rules (answers
  never revealed, one-try MCQ, typed number puzzles); two-step sums.
- 2026-10-31: In-app updater for the TV/phone app (no Downloader needed for
  updates); version shown on the title screen.
- 2026-10-31: Voice-over for all story and puzzle text (can't be skipped),
  background music, 7 outfits, Settings screen.
- 2026-10-31: Math answers are now typed on a number pad (no MCQ for
  math); puzzle interval limited to 3 or 5 min.
- 2026-10-31: Web game now lives at `/hridu-great-rescue.html` (Rajat's ask);
  old folder address redirects; installed app starts there.
- 2026-10-31: Age removed from story/docs. Web build is now an installable
  PWA (manifest, offline service worker, icons, install button). Math limited
  to two-digit +/−; no repeated questions; bigger opposites list.
- 2026-10-31: First version — 3 game modes, 8 levels with story, puzzle pit
  stops, TV/phone/laptop controls, Android TV wrapper, APK Release + Pages.
