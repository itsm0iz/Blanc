# Blanc

A minimal, fast, distraction-free Android launcher — text-only home screen,
built for intentional phone use.

Blanc keeps the calm, instant feel of a minimal launcher and adds the things
those launchers usually miss: a real **universal search** drawer, an on-device
**usage monitor** with trends and projections, genuine **customization**, and
quality-of-life essentials like backup/restore. No ads, no wallpaper gimmicks,
no clutter. 100% on-device.

> Status: **early foundation (v0.1).** The core launcher works — text home
> screen, app drawer with search, home-app slots, alignment/theme settings.
> The bigger features below are on the roadmap.

## Foundation (working now)

- Registers as a home app / default launcher.
- Text home screen: clock, date, and up to 8 chosen apps.
- Gestures: swipe up → app drawer, long-press → settings.
- App drawer with instant search across all profiles (main / work / private).
- Assign, replace, and remove home apps; add apps by long-press in the drawer.
- Settings: home alignment (left / center / right), theme (system / light /
  dark), status-bar visibility, set-as-default-launcher.

## Universal search (working now)

Swipe up opens one search box that spans:

- **Apps** across all profiles
- **Calculator** — type `2+2*3`, get `= 8`; tap to copy
- **Offline dictionary** — type a word (or `define <word>`) for an instant,
  fully on-device definition. 108k words, ~3.8 MB, built from the Wordset
  dictionary (CC BY-SA 4.0, derived from WordNet 3.0)
- **Settings** shortcuts (Wi-Fi, Bluetooth, Display, …)
- **Web** search fallback

Enter launches the top app match, or searches the web when there's none.

## Roadmap

- **Search: contacts** — add contact lookup / quick-dial (needs the contacts
  permission flow).
- **Usage monitor** — a lightweight screen (opened from the Blanc app entry)
  with charts, forward projections, and optional motivational nudges. Uses a
  local database that records daily totals over time, since Android only keeps
  ~10 days of raw usage events.
- **Suggestions row** — most-used / recent apps pinned above the A–Z drawer.
- **More customization** — fonts, remappable gestures, layout, optional weather.
- **Quality of life** — backup / restore settings, app lock for hidden apps.

## Tech stack

- Kotlin, Jetpack Compose, Material 3
- DataStore (settings), coroutines / Flow
- Single module; `minSdk 24`, `targetSdk 35`

## Build

Requires the Android SDK (platform 35, build-tools 35).

```bash
./gradlew assembleDebug
```

The debug APK lands in `app/build/outputs/apk/debug/`. CI builds every push
via GitHub Actions.

## License

MIT — see [LICENSE](LICENSE).
