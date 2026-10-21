# p1ll

Pill tracker for Android. A year chart shows one square per day, split into one slice per dose.

<p>
  <img src="docs/screenshots/home-light.png" width="32%" alt="Home, light" />
  <img src="docs/screenshots/home-dark.png" width="32%" alt="Home, dark" />
  <img src="docs/screenshots/editor-light.png" width="32%" alt="Editor" />
</p>

## Features

- Schedules for every day, chosen weekdays, every N days, or as needed
- Notifications with Take and Snooze, and optional repeats
- End dates, with history kept
- Optional pills, never counted as missed
- Supply count with a refill warning
- Swipe right to take, left to skip, tap a chart square to edit that day
- Backdate a pill, or fill in any past day from the chart

## Build

```sh
./gradlew testDebugUnitTest assembleRelease   # signed APK in app/build/outputs/apk/release/
./gradlew recordRoborazziDebug                # re-render docs/screenshots
python3 tools/sounds.py                       # re-synthesise the sounds in res/raw
python3 tools/fonts.py <RedHatDisplay[wght].ttf> <RedHatText[wght].ttf>   # re-cut the static font weights
```

Requires JDK 17+ and the Android SDK (compileSdk 36).

## Releases and signing

Push a tag and CI attaches a signed APK to its GitHub release:

```sh
git tag v0.1.0 && git push origin v0.1.0
```

`v1.2.3` builds version name `1.2.3`, version code `10203`.

The release keystore and its passwords are committed in `keystore/`, so CI signs without secrets. Anyone with this repo can sign an APK that installs as an update to p1ll. Moving the key to GitHub Secrets means rotating it, after which existing installs must uninstall before updating.

Signing certificate SHA-256:

```
2C:EE:D7:33:AB:DF:7B:72:CA:8D:B2:82:C3:0B:66:3E:CF:C7:A5:15:00:28:0A:88:DF:E7:74:97:34:2A:F3:82
```

## Notes

- Reminders use `USE_EXACT_ALARM`, granted at install on Android 13+. Play allows it only for alarm and calendar apps, so a Play build would need `SCHEDULE_EXACT_ALARM`.
- One alarm drives all reminders, so doses due together aren't throttled under Doze and missed ones post after a reboot.
- Fonts: [Red Hat Display and Red Hat Text](https://github.com/RedHatOfficial/RedHatFont), SIL Open Font License (`licenses/`).
