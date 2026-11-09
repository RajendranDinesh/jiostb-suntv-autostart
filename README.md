# Sun TV Autostart (JioTV+ STB boot helper)

A tiny Android TV helper that makes a Jio set-top box **open a live channel
automatically on power-on** — so the TV lands straight on the channel (e.g.
**Sun TV**) the moment it boots, the way an old cable/DTH box did.

It was built for grandparents who just want to switch the TV on and watch one
channel — no launcher, no app-picking, no remote gymnastics.

- **No root. No bootloader unlock.** Installs over `adb` like any sideloaded app.
- **Pure Java, no native code** → one universal APK installs on any ABI.
- Fires the channel deep-link **once** on boot, confirms the player is up, then
  stops (no repeated reloading).
- Also adds a **"Sun TV" tile** to the launcher — tapping it opens the channel
  directly (and re-arms the boot autostart).

> Targets the JioTV+ live-TV app (`com.jio.media.jiotvplus`) on Jio Android TV
> STBs (tested on **JMSD200A**, Android 11). The deep-link scheme is JioTV+'s, so
> this is specific to that app.

---

## Install (grab the prebuilt APK)

1. Download **`suntvboot.apk`** from the [latest Release](../../releases/latest).
2. Enable ADB on the box and connect to it:
   ```sh
   adb connect <BOX_IP>:5555
   ```
3. Install and arm it:
   ```sh
   adb install suntvboot.apk

   # allow it to launch the player from the background on boot
   adb shell appops set com.home.suntvboot SYSTEM_ALERT_WINDOW allow
   # allow it to detect when the player is already on-screen (fire-once)
   adb shell appops set com.home.suntvboot GET_USAGE_STATS allow

   # open it ONCE to arm the BOOT_COMPLETED receiver
   adb shell am start -n com.home.suntvboot/.MainActivity
   ```
4. Reboot to test: `adb reboot`. On boot it waits for network, fires the
   channel deep-link, confirms the player is foreground, and stops.

> **Updating:** releases are all signed with the same key, so a newer APK installs
> straight over an older one. If you previously installed a build you signed
> yourself, `adb uninstall com.home.suntvboot` first.

> **Caveat:** if the app is ever *Force stopped* or its data cleared, open it once
> more (tap the tile, or the `am start` above) to re-arm the boot autostart.

---

## Pick a different channel

The channel is a JioTV+ deep-link id. To change it, edit `DEEPLINK` in
[`BootService.java`](app/src/main/java/com/home/suntvboot/BootService.java) and
rebuild. Format: `jiotvplus://<id>?resume=true&source=watchnext`.

| Channel | Deep-link id |
|---|---|
| Sun TV | 304189 |
| Star Vijay HD | 302085 |
| Zee Tamil HD | 304101 |
| Colors Tamil HD | 301155 |
| Star Sports 1 Tamil HD | 302122 |
| News Tamil 24x7 | 301474 |
| Polimer News | 300070 |
| News18 Tamil Nadu | 300279 |
| News18 India | 300209 |
| Hungama TV | 302072 |

> These ids were captured from the player's "watch-next" logs — the set grows as
> more channels are watched. To find any channel's id: tune to it in the player,
> then read `cid=<id>` from `adb logcat`.

The live-TV app must also be able to reach its CDNs for playback. If you run a
DNS/firewall app (e.g. RethinkDNS) on the box, allow or bypass the JioTV+ app.

---

## Build from source

Needs nothing locally — [GitHub Actions](.github/workflows/release.yml) builds
and publishes the APK on every `v*` tag. To build yourself:

```sh
./gradlew assembleRelease
# -> app/build/outputs/apk/release/app-release.apk
```

Requirements: JDK 17 + Android SDK (platform 34). `minSdk 21`, `targetSdk 28`
(kept at 28 on purpose — the boot auto-launch relies on pre-Android-10
background-start behaviour plus the `SYSTEM_ALERT_WINDOW` appop).

### About the committed signing key
`keystore/suntvboot.keystore` is an **intentionally public, debug-grade key**
(password `android`, alias `d`). It is committed so every release shares one
signature, letting updates install cleanly over each other. It is **not a secret**
and grants no privilege beyond letting the APK install. Swap in your own key in
[`app/build.gradle`](app/build.gradle) if you prefer.

---

## How it works

- `BootReceiver` catches `BOOT_COMPLETED` → starts `BootService` (a foreground
  service).
- `BootService` waits for network, fires the channel deep-link, then uses
  `UsageStatsManager` to check whether the player is now foreground; if it is, it
  stops (fires at most twice). This "fire-once" check is what stops the channel
  from reloading several times on boot.
- `MainActivity` is the launcher tile: tapping it opens the channel and re-arms
  the autostart.

## License

[MIT](LICENSE).
