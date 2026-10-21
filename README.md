# p1ll

A pill tracker built around a chart like GitHub's contribution graph. Each square is one day, cut into one slice per dose, clockwise from twelve o'clock in time order. Take every dose and the square closes into a solid block.

<p>
  <img src="docs/screenshots/home-light.png" width="32%" alt="Home, light" />
  <img src="docs/screenshots/home-dark.png" width="32%" alt="Home, dark" />
  <img src="docs/screenshots/editor-light.png" width="32%" alt="Editor" />
</p>

## What it does

- **Pills**: name, dose, shape and colour. Schedules can be every day, chosen weekdays, every N days, or as needed, with any number of reminder times.
- **End dates**: a course stops reminding after its last day and moves to *Finished*, and its history stays in the chart.
- **Optional pills**: you still get reminders, but they never count as missed.
- **Reminders**: a notification with **Taken** and **Snooze** buttons. You can also set repeat nudges until you act on a reminder (Settings → Reminders).
- **Home**: shows doses left today and what's due next, then the year chart, then a card per pill. Tap the round button or swipe right to take a dose; swipe left to skip. Tap the dots to fix a single dose, or tap any chart square to edit that day.
- **Supply** (optional): counts down as you take doses and warns you at your refill point.
- **Feel**: each dose plays the next note of a pentatonic scale, and the day's last dose plays a chord. Haptics use Android's composed vibration primitives where the phone supports them. Predictive back scrubs the real screen transition, so the card-to-detail morph follows your thumb.

## Build

```sh
./gradlew testDebugUnitTest assembleRelease   # signed APK in app/build/outputs/apk/release/
./gradlew recordRoborazziDebug                # re-render docs/screenshots
python3 tools/sounds.py                       # re-synthesise the sounds in res/raw
```

Requires JDK 17+ and the Android SDK (compileSdk 36).

## Releases and signing

Push a tag and CI publishes a signed APK to that tag's GitHub release:

```sh
git tag v0.1.0 && git push origin v0.1.0
```

The version name comes from the tag (`v1.2.3` gives `1.2.3`, with version code `10203`).

**The release keystore and its passwords are committed on purpose**, in `keystore/`. That's what keeps builds reproducible and lets every CI run sign without any secrets. The trade-off: anyone with this repo can sign an APK that Android will accept as an update to p1ll. [Android's own guidance](https://developer.android.com/studio/publish/app-signing#secure-shared-keystore) is to keep keys out of public source control. To change that later, move the files into GitHub Secrets and rotate the key. Rotating means existing installs must uninstall before they can take the newly signed build.

Signing certificate SHA-256:

```
2C:EE:D7:33:AB:DF:7B:72:CA:8D:B2:82:C3:0B:66:3E:CF:C7:A5:15:00:28:0A:88:DF:E7:74:97:34:2A:F3:82
```

## Notes

- Exact reminders use `USE_EXACT_ALARM`, which Android 13+ grants at install. Google Play restricts it to alarm and calendar apps, so a Play build would need the `SCHEDULE_EXACT_ALARM` permission flow instead ([Android 14 exact-alarm changes](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)).
- All reminders run off a single alarm. When it fires, everything due is posted together and the next alarm is armed. This keeps doses at the same time from being throttled under Doze, and lets the app catch up after a reboot.
- Typeface: [Instrument Sans](https://github.com/Instrument/instrument-sans), SIL Open Font License (`licenses/`).
