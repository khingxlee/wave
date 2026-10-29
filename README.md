# Wave (native Android)
Kotlin + Jetpack Compose + Media3. Builds on GitHub Actions (.github/workflows/build.yml) or Android Studio.
Package: com.khingxlee.wave   |   Contact: vellora099@gmail.com

Install `app-release.apk` (small, optimized). `app-debug.apk` is the slow test build.

## Highlights
Library (songs / albums / artists / playlists / folders), independent tempo + pitch with centred sliders, A-B loop,
big waveform, full-screen synced lyrics with translation, equalizer, full-page tag editor with cover, album pages,
Android Auto browsing, light / dark / system themes, six languages.

## Signing
* Test builds use app/debug.keystore (committed on purpose) so each build installs over the previous one.
* Release key: create once with keytool, then add GitHub secrets KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD.

## Version 6 notes
Custom side drawer (cannot hang open), floating glass mini player, swipeable categories, elastic tab indicator, pill search,
up-next queue with drag reorder, colour-sweep lyrics style, join button, crash report in Settings > Support.
