# RandomLauncher

**A minimal Android launcher that moves your apps every time you unlock your phone.**

You probably open some apps without deciding to. Your thumb knows where Instagram is before
you've thought about it. RandomLauncher works against that habit: each time you unlock, the
app drawer is reshuffled, and **no app ever stays in the cell it had last time**. To open
something, you have to look for it, and that short pause is often enough to ask yourself
whether you meant to open it at all.

It is inspired by the minimalism of [Olauncher](https://github.com/tanujnotes/Olauncher) but
it is not a clone. It uses normal app icons, and it is built around this one idea.

- No internet permission. No analytics, no ads, no tracking.
- Fully open source under GPL-3.0.
- Built with Kotlin and Jetpack Compose. Android 8.0+ (API 26).

---

## Contents

- [How it works](#how-it-works)
- [Features](#features)
- [Gestures](#gestures)
- [The wait screen](#the-wait-screen)
- [Permissions](#permissions)
- [Installing](#installing)
- [Building from source](#building-from-source)
- [Project structure](#project-structure)
- [Design notes](#design-notes)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)

---

## How it works

The home screen is intentionally almost empty:

```
  12:45
  Tuesday, 6 October




  [pinned] [pinned] [pinned] [pinned]     ← dock: never moves
                 ︿                        ← swipe up for the drawer
```

Swipe up and you get the app drawer: a grid of every app that isn't pinned or hidden, in an
order that changes on every unlock.

```
 unlock #1               unlock #2               unlock #3
 [Maps ][Mail ][Chat]    [Notes][Maps ][Bank]    [Chat ][Notes][Mail]
 [Notes][Bank ][Cam ]    [Cam  ][Chat ][Mail]    [Bank ][Cam  ][Maps]
```

The shuffle is a *derangement*: every app ends up in a different cell from its previous one,
so the position you remember is always wrong.

Apps you actually need to reach without thinking, like the phone, camera or a password
manager, go in the **dock**. The dock has 3, 4 or 5 slots and never changes.

## Features

| | |
|---|---|
| **Shuffle on unlock** | The drawer is reshuffled after every unlock or screen-off. Every app moves. |
| **Dock** | 3, 4 or 5 configurable slots. Pinned apps stay where they are and leave the drawer. |
| **Fixed gestures** | Swipes, double-tap and clock tap always do the same thing, so the escape routes are predictable. |
| **Wait screen** | Optional per-app pause before an app opens, with daily limits that make the pause grow. |
| **Usage stats** | With Usage Access, shows real screen time and open counts per app for today. |
| **Hide & rename** | Remove apps from the drawer or give them a different name. |
| **Optional search** | Off by default, since search would defeat the shuffle. Turn it on if you need it. |
| **Labels toggle** | Turn icon labels off for a harder, icon-only drawer. |
| **Themes** | System, light or dark. Your wallpaper shows through the home screen. |
| **Pull to close** | When the drawer is scrolled to the top, keep pulling down to close it. |

## Gestures

| Where | Gesture | Action |
|---|---|---|
| Home | Swipe up | Open the app drawer |
| Home | Swipe down | Expand the notification shade |
| Home | Swipe left / right | Open the app you assigned (optional) |
| Home | Double-tap | Lock the screen (needs the accessibility service, Android 9+) |
| Home | Tap the clock | Open the clock app, or an app you assign |
| Home | Long-press empty space | Settings |
| Home / drawer | Long-press an icon | App menu: pin, rename, wait screen, hide, app info, uninstall |
| Drawer | Pull down at the top, or drag the handle | Close the drawer |
| Anywhere | Back / Home | Go back to the home screen |

## The wait screen

Some apps are worth a short pause before they open. For any app you can turn on a **wait
screen**: a full-screen countdown with the app's icon, today's usage and an optional message
you write yourself (for example *"Is this what you came here for?"*). When the countdown
finishes you still have to tap **Open anyway**. The app does not open on its own.

Each rule can be configured separately:

| Setting | Meaning |
|---|---|
| Wait | Base countdown, 1–60 seconds |
| Daily opens | After this many opens today, the wait gets longer |
| Daily time | After this many minutes today, the wait gets longer |
| Over limit | Seconds added for each open past the limit (and for every 10 minutes past the time limit) |
| Message | Text shown on the wait screen |

Nothing is ever blocked. Going over a limit only makes the wait longer, up to a maximum of
five minutes. You decide whether to open the app.

## Permissions

RandomLauncher works without granting anything. Two optional permissions add features:

| Permission | Used for | Without it |
|---|---|---|
| **Usage access** | Today's real screen time and open count per app; more reliable detection of unlocks. | Only launches made from RandomLauncher are counted, and time limits don't apply. |
| **Accessibility service** | Only to lock the screen when you double-tap. It does not read window content (`canRetrieveWindowContent="false"`). | Double-tap to lock is unavailable. |

There is **no internet permission**. Nothing leaves your device.

The *Permissions* page in settings shows what is granted and links straight to each system
setting.

## Installing

### F-Droid

An F-Droid submission is in progress. This section will link to it once it is published.

### From a release / build

1. Build or download an APK (see below).
2. Install it: `adb install -r app-release.apk`, or open the file on your phone.
3. Press the Home button and choose **RandomLauncher**, or go to
   *Settings → Permissions → Default home app*.
4. Optionally grant **Usage access** and enable the **accessibility service** from the same page.

## Building from source

Requirements: **JDK 17+** (21 recommended) and the **Android SDK** with platform 36.

```sh
git clone https://github.com/AgopSagopyan/RandomLauncher.git
cd RandomLauncher

./gradlew assembleDebug          # debug APK → app/build/outputs/apk/debug/
./gradlew testDebugUnitTest      # unit tests
./gradlew assembleRelease        # minified release APK (unsigned by default)
```

**Release signing:** release builds are unsigned so that F-Droid can sign its own builds. To
get an installable release APK for sideloading, add this to `local.properties` (not committed):

```properties
signReleaseWithDebugKey=true
```

**Nix users:** the repo includes a [devenv](https://devenv.sh) setup (`devenv.nix`) that
provides JDK 21 and the Android SDK. Run `devenv shell`, then use the Gradle commands above.

## Project structure

```
app/src/main/java/com/agopsagopyan/randomlauncher/
├── LauncherApp.kt            Application: wiring, unlock/screen-off receivers
├── domain/                   Pure Kotlin, no Android dependencies, unit tested
│   ├── Shuffler.kt           Derangement shuffle (no app keeps its cell)
│   └── Friction.kt           Wait-screen rules and delay calculation
├── data/
│   ├── AppRepository.kt      Installed apps via LauncherApps, icons, launching
│   ├── ConfigStore.kt        DataStore: settings, drawer order, launch counts
│   ├── LauncherConfig.kt     Serializable settings model
│   ├── ShuffleManager.kt     Decides *when* to shuffle (see below)
│   └── UsageTracker.kt       UsageStatsManager: screen time, opens, unlock events
├── system/
│   ├── LockAccessibilityService.kt   Double-tap to lock
│   └── SystemActions.kt              Notification shade, system settings intents
└── ui/                       Jetpack Compose
    ├── MainActivity.kt       HOME activity, lifecycle hooks
    ├── LauncherViewModel.kt  UI state and actions
    ├── LauncherRoot.kt       Screen layering (home, drawer, settings, wait screen)
    ├── HomeScreen.kt         Clock, dock, gestures
    ├── DrawerScreen.kt       Shuffled grid, pull-to-close, optional search
    ├── DelayScreen.kt        The wait screen
    ├── SettingsScreen.kt     Settings pages
    ├── Dialogs.kt            App menu, rename, app picker
    ├── AppIcon.kt, Theme.kt
```

## Design notes

### When does it shuffle?

Detecting "the user just unlocked the phone" on modern Android is less simple than it sounds:

- `ACTION_USER_PRESENT` and `ACTION_SCREEN_OFF` can't be declared in the manifest. They have to
  be registered at runtime.
- On Android 14+, broadcasts to a *cached* process can be deferred until the process becomes
  active again, and a launcher in the background is often cached.
- Some devices with a swipe-only lock screen don't send `USER_PRESENT` reliably.

So no single broadcast is trusted. Several signals set the same **pending** flag:

1. `SCREEN_OFF` / `USER_PRESENT` broadcasts.
2. The activity being stopped while the screen is off.
3. A fresh process start (boot, or the process was killed).

Whenever the home screen comes to the foreground (`onStart`), the launcher checks that flag.
It also checks `UsageStatsManager` for a `KEYGUARD_HIDDEN` / `SCREEN_INTERACTIVE` event since
the last shuffle. If either says yes, the drawer is reshuffled before you can open it. An open
drawer is also closed when the screen turns off, so it never reorders in front of you.

Every shuffle is logged with its reason:

```sh
adb logcat -s RandomLauncher
```

### The shuffle

`Shuffler` uses rejection sampling: shuffle, and accept the result if no app is in its old
position. A random permutation is a derangement with probability ≈ 1/e, so this usually takes
two or three tries. For edge cases there is a deterministic repair fallback. Apps installed
since the last shuffle are simply placed at the end until the next one.

### Performance

- The drawer is **always composed**. When closed it is transparent and layered *below* the home
  screen, so opening it costs only a `graphicsLayer` animation, not composing the grid.
- UI state classes are `@Immutable`, so icons only recompose when their own data changes.
- Release builds use R8 and ship Compose's baseline profiles via `profileinstaller`.

## Roadmap

- [ ] F-Droid release
- [ ] Translations (the UI is English-only for now)
- [ ] Widgets
- [ ] Work profile apps
- [ ] Optional "shuffle the dock too" hard mode

## Contributing

Issues and pull requests are welcome. Please keep the spirit of the project: minimal,
offline and free of tracking. Run `./gradlew testDebugUnitTest lintDebug` before opening a
PR.

## License

RandomLauncher is free software, released under the
[GNU General Public License v3.0](LICENSE).
