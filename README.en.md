# Fake Call

[فارسی](README.md) · **English**

An Android app that schedules a realistic fake incoming call — with a caller name, number and photo you choose — to ring exactly when you need a polite way out of a meeting, a date, or any awkward situation.

`Version 0.1.0-beta1 (beta)` · Android 8 (API 26)+ · MIT

---

## What it does

- **Create caller profiles:** save a name, number, photo and even a recorded voice for each caller — "Boss", "Mom", "Colleague".
- **Precise scheduling:** ring in 10 seconds, 30 seconds, 1 minute, 5 minutes, or any custom delay. Exact alarms make the call ring on time even when the app is closed or the phone is locked.
- **Realistic call experience:** a full-screen incoming call with the caller's photo and name, ringtone and vibration; after you answer, the profile's recorded voice plays so everything sounds natural.
- **History:** every executed fake call is logged inside the app.
- **Fully offline and private:** no internet, no account, no tracking, no ads.

## Features

- Bilingual (Persian and English), follows the system language by default, switchable in Settings
- Light and dark themes matching the system
- Full RTL layout support
- Unlimited profiles with custom photo and voice
- Live countdown for the scheduled call with cancel option

## Tech stack

| Area | Technology |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose · Material 3 |
| Architecture | MVVM · Clean Architecture |
| Storage | Room · DataStore |
| Scheduling | AlarmManager (exact alarm) · Full-screen intent |
| Min Android | API 26 (Android 8) |
| Target | API 35 (Android 15) |

## Build locally

```bash
git clone https://github.com/mostafaafrouzi/fake-call-android.git
cd fake-call-android
./gradlew assembleDebug
```

The Cafe Bazaar and Myket builds differ only in the developer-page link inside the app:

```bash
./gradlew assembleBazaarRelease assembleMyketRelease
```

## Get it

- [GitHub Releases](https://github.com/mostafaafrouzi/fake-call-android/releases)

## Contact

- Website: [afrouzi.ir](https://afrouzi.ir/en/?utm_source=fakecall&utm_medium=github_readme&utm_campaign=fakecall)
- LinkedIn: [linkedin.com/in/mostafaafrouzi](https://linkedin.com/in/mostafaafrouzi)
- GitHub: [github.com/mostafaafrouzi](https://github.com/mostafaafrouzi)

## License

Free and open source under the MIT license.
