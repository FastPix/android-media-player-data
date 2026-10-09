# FastPix Video Data SDK for Android MediaPlayer - video analytics and QoS monitoring

[![Release](https://img.shields.io/github/v/release/FastPix/android-media-player-data?label=release)](https://github.com/FastPix/android-media-player-data/releases)
[![Platform](https://img.shields.io/badge/platform-Android-3DDC84.svg)](https://developer.android.com/media/platform/mediaplayer)
[![minSdk](https://img.shields.io/badge/minSdk-24-blue.svg)](https://developer.android.com/tools/releases/platforms)
[![Language](https://img.shields.io/badge/Kotlin-1.9-7F52FF.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/license-Apache--2.0-green.svg)](LICENSE)

**FastPix is an Android MediaPlayer analytics SDK.** It tracks playback performance, user engagement, and quality of service (QoS / QoE) for Android's native [`MediaPlayer`](https://developer.android.com/reference/android/media/MediaPlayer) and streams them to the [FastPix dashboard](https://dashboard.fastpix.com) in real time. Drop the wrapper around the `MediaPlayer` you already ship - no new player, no manual instrumentation - and every session reports startup time, rebuffering, playback failures, resolution changes, and viewer engagement. It builds on [FastPix's Android Data Core SDK](https://github.com/FastPix/android-core-data-sdk).

This SDK is for Android's **native (legacy) MediaPlayer**. If your app uses **ExoPlayer or AndroidX Media3**, use [android-data-androidXmedia3](https://github.com/FastPix/android-data-androidXmedia3) instead - see [Which FastPix data SDK do I need?](#which-fastpix-data-sdk-do-i-need).

Track video performance and viewer engagement across Android phones, tablets, and Android TV, for streaming apps in OTT, e-learning, live events, short-form video, and user-generated content (UGC) platforms.

**Works with:** Android's native `MediaPlayer` on Android 7.0 (API 24) and above, HLS and MP4 sources, Kotlin and Java.

[FastPix docs](https://fastpix.com/docs) · [Video Data overview](https://fastpix.com/docs/video-data/overview) · [Create a free account](https://dashboard.fastpix.com/signup?utm_source=github&utm_medium=referral&utm_campaign=github_signup)
---

## Start here

New to the SDK? Follow these steps in order and you will have analytics flowing to your dashboard:

1. [Why FastPix Video Data?](#why-fastpix-video-data)
2. [What you can track](#what-you-can-track)
3. [How the data flows](#how-the-data-flows)
4. [Before you start](#before-you-start)
5. [Add the GitHub Packages repositories](#add-the-github-packages-repositories)
6. [Add the SDK dependency](#add-the-sdk-dependency)
7. [Add the required permissions](#add-the-required-permissions)
8. [Attach the SDK to your MediaPlayer](#attach-the-sdk-to-your-mediaplayer)
9. [Verify events in your dashboard](#verify-events-in-your-dashboard)
10. [Changing the video source](#changing-the-video-source)
11. [Error handling](#error-handling)
12. [Which FastPix data SDK do I need?](#which-fastpix-data-sdk-do-i-need)
13. [FAQ](#faq)
14. [Troubleshooting](#troubleshooting)

---

## Why FastPix Video Data?

Native `MediaPlayer` gives you playback but no visibility into how that playback performs for real viewers. FastPix fills that gap: drop in the wrapper, pass your workspace key, and every session shows up on the dashboard with QoE metrics you can slice by device, app version, video, and custom dimensions.

- **No new player required.** Keep the `MediaPlayer` you already ship. The SDK observes it; it does not replace it.
- **Real-time QoE analytics.** Startup time, rebuffering ratio, playback failures, and resolution changes, collected as the video plays.
- **Viewer engagement.** Play, pause, seek, and completion tell you what people actually watch.
- **Lightweight.** A thin wrapper over the FastPix Data Core - no heavy player dependencies pulled in.
- **Usage-based pricing.** FastPix bills per minute of monitored video, so costs track your actual usage. See [pricing](https://fastpix.com/pricing).

## What you can track

- **User engagement** - play, pause, seek, and video completion.
- **Playback quality** - buffering events, resolution / video-size changes, and network-aware context.
- **Device and app diagnostics** - manufacturer, model, OS version, and app version, so you can spot device-specific issues.
- **Errors** - automatic capture of fatal and handled playback errors, with an option to report errors manually.
- **Custom metadata** - up to 10 free-form dimensions (`customData1` ... `customData10`) to filter the dashboard by anything meaningful to your app.

## How the data flows

![How FastPix Video Data flows from your Android MediaPlayer to the dashboard](mediaplayer-data-workflow.png)

Your app plays a video on `MediaPlayer`. `FastPixBaseMediaPlayer` wraps that player, reads its listeners, and turns playback signals (play, pause, seek, buffering, errors, resolution changes) into beacons sent over HTTPS to FastPix, keyed to your workspace. A few minutes later the session appears in your FastPix dashboard as QoE and engagement analytics.

## Before you start

You will need:

- **A FastPix account and workspace key.** [Sign up free](https://dashboard.fastpix.com/signup?utm_source=github&utm_medium=referral&utm_campaign=github_signup), then copy the **Workspace Key** from your FastPix dashboard. This is the only value the SDK strictly requires.
- **Android Studio** (Arctic Fox or newer) with **JDK 17** - the library and sample build against Java 17.
- **Android SDK 24+.** The library's `minSdk` is **24** (Android 7.0). (Note: earlier docs said 21; the published library requires 24.)
- **A GitHub Personal Access Token (PAT)** with the `read:packages` scope. The SDK and its core dependency are published to **GitHub Packages**, which requires authentication even for public packages. [Create a token](https://github.com/settings/tokens).

---

## Add the GitHub Packages repositories

The SDK (`io.fastpix.data:mediaplayer`) and its required core dependency (`io.fastpix.data:core`) live in **two** GitHub Packages repositories. Add **both**, or the core dependency will fail to resolve. Put your GitHub username and PAT in `gradle.properties` (or `~/.gradle/gradle.properties`, so credentials stay out of source control) and reference them here.

In `settings.gradle`:

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/FastPix/android-media-player-data")
            credentials {
                username = "<your-github-username>"
                password = "<your-personal-access-token>"
            }
        }
        maven {
            url = uri("https://maven.pkg.github.com/FastPix/android-core-data-sdk")
            credentials {
                username = "<your-github-username>"
                password = "<your-personal-access-token>"
            }
        }
    }
}
```

## Add the SDK dependency

In your app module's `build.gradle`:

```groovy
dependencies {
    implementation 'io.fastpix.data:mediaplayer:1.0.0'
}
```

This pulls in `io.fastpix.data:core:1.0.1` transitively, which is why the core GitHub Packages repository added above is required.

## Add the required permissions

The SDK sends analytics over the network, so your app's `AndroidManifest.xml` needs both:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## Attach the SDK to your MediaPlayer

The wrapper needs three things: your `MediaPlayer`, your customer/video/view metadata (with the workspace key), and the player's listeners routed through the SDK so it can observe playback. The user-driven transitions - **play, pause, and seek** - must also be reported explicitly right after the corresponding `MediaPlayer` call.

Every snippet below is taken verbatim from the runnable sample in this repo, [`app/src/main/java/io/fastpix/data/mediaplayer/sample/VideoPlayerActivity.kt`](app/src/main/java/io/fastpix/data/mediaplayer/sample/VideoPlayerActivity.kt). Open that file to see the full, tested Activity in one place.

**Create the wrapper once your `MediaPlayer` is set up, then attach the FastPix listeners and prepare the player:**

```kotlin
    private fun startPlayback(holder: SurfaceHolder) {
        if (mediaPlayer != null) return

        try {
            val player = MediaPlayer()
            player.setDataSource(VIDEO_URL)
            player.setDisplay(holder)
            // Only takes effect once a SurfaceHolder is attached, so it has to follow setDisplay().
            player.setScreenOnWhilePlaying(true)
            mediaPlayer = player

            fastPixBaseMediaPlayer = FastPixBaseMediaPlayer(
                this,
                player,
                PLAYER_NAME,
                buildCustomerData(),
                CustomOptions()
            ).apply {
                // Lets the SDK read the player view dimensions for view-size metrics.
                setPlayerView(surfaceView)
                // Automatic error tracking is on by default; call this to opt out and report
                // errors yourself via error(...).
                setAutomaticErrorTracking(true)
            }

            attachFastPixListeners(player)
            player.prepareAsync()
        } catch (e: IOException) {
            Log.e(TAG, "Unable to open $VIDEO_URL", e)
            toast("Unable to open the video source")
        }
    }
```

**Keep the listeners as fields** (not inline lambdas) so the SDK can hold them. The wrapper records each event and forwards the callback to your listener, so your app keeps full control while FastPix still sees playback:

```kotlin
    // The SDK stores the listeners handed to getOnXxxListener() in WeakReferences, so an inline
    // lambda is collected on the next GC and the app silently stops receiving callbacks. Keep them
    // as fields — anything that outlives the player works.
    private val preparedListener = MediaPlayer.OnPreparedListener {
        isPrepared = true
        // The SDK flips its own prepared flag *after* this callback returns, so start playback on
        // the next loop iteration — otherwise the play event is recorded against a player the SDK
        // still considers unprepared.
        surfaceView.post { play() }
    }

    private val completionListener = MediaPlayer.OnCompletionListener {
        // Back to a "play" affordance so the video can be replayed from the overlay.
        updatePlayPauseIcon()
        showControls()
        toast("Playback finished")
    }

    private val seekCompleteListener = MediaPlayer.OnSeekCompleteListener { /* no-op */ }

    private val videoSizeChangedListener =
        MediaPlayer.OnVideoSizeChangedListener { _, width, height ->
            Log.d(TAG, "Video size: ${width}x$height")
        }

    private val infoListener = MediaPlayer.OnInfoListener { _, what, extra ->
        Log.d(TAG, "MediaPlayer info: what=$what extra=$extra")
        false
    }

    private val errorListener = MediaPlayer.OnErrorListener { _, what, extra ->
        // MediaPlayer moves to its Error state here; nothing may be queried until reset().
        isPrepared = false
        updatePlayPauseIcon()
        Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
        toast("Playback error ($what)")
        true
    }
```

**Register those listeners through the SDK** so it sees prepare, completion, seek, size changes, info, and errors:

```kotlin
    private fun attachFastPixListeners(player: MediaPlayer) {
        val fastPix = fastPixBaseMediaPlayer ?: return

        player.setOnPreparedListener(fastPix.getOnPreparedListener(preparedListener))
        player.setOnCompletionListener(fastPix.getOnCompletionListener(completionListener))
        player.setOnSeekCompleteListener(fastPix.getOnSeekCompleteListener(seekCompleteListener))
        player.setOnVideoSizeChangedListener(
            fastPix.getOnVideoSizeChangedListener(videoSizeChangedListener)
        )
        player.setOnInfoListener(fastPix.getOnInfoListener(infoListener))
        player.setOnErrorListener(fastPix.getOnErrorListener(errorListener))
    }
```

**Build the metadata that travels with the session** (`workspaceKey` is the only required field - copy it from your FastPix dashboard):

```kotlin
    private fun buildCustomerData(): CustomerDataEntity {
        val playerData = CustomerPlayerDataEntity().apply {
            workspaceKey = WORKSPACE_KEY
        }

        val videoData = CustomerVideoDataEntity().apply {
            videoId = VIDEO_ID
            videoTitle = VIDEO_TITLE
            videoSourceUrl = VIDEO_URL
        }

        val viewData = CustomerViewDataEntity().apply {
            // A fresh id per view; keep it stable for the lifetime of a single playback session.
            viewSessionId = UUID.randomUUID().toString()
        }

        return CustomerDataEntity(playerData, videoData, viewData).apply {
            // Up to 10 free-form dimensions you can filter by on the dashboard.
            customData = CustomDataEntity().apply {
                customData1 = "sample-app"
                customData2 = BuildConfig.BUILD_TYPE
            }
        }
    }
```

**Report the three user-driven transitions explicitly**, right after the matching `MediaPlayer` call:

```kotlin
    private fun play() {
        val player = mediaPlayer ?: return
        if (!isPrepared || player.isPlaying) return
        player.start()
        fastPixBaseMediaPlayer?.play()
        updatePlayPauseIcon()
        showControls()
    }

    private fun pause() {
        val player = mediaPlayer ?: return
        if (isPrepared && player.isPlaying) {
            player.pause()
            fastPixBaseMediaPlayer?.pause()
            updatePlayPauseIcon()
            showControls()
        }
    }

    private fun seekForward() {
        val player = mediaPlayer ?: return
        if (!isPrepared) return
        val target = (player.currentPosition + SEEK_STEP_MS).coerceAtMost(player.duration)
        player.seekTo(target)
        fastPixBaseMediaPlayer?.seeking()
    }
```

**Release the SDK when the player goes away**, so the in-flight session is flushed:

```kotlin
    private fun releasePlayers() {
        isPrepared = false
        playerControls.removeCallbacks(hideControlsRunnable)
        // Release FastPix first so the in-flight session is flushed while the player is still alive.
        fastPixBaseMediaPlayer?.release()
        fastPixBaseMediaPlayer = null

        mediaPlayer?.release()
        mediaPlayer = null
    }
```

## Verify events in your dashboard

1. Run the app and play a video.
2. Use the player: press play, pause, and seek at least once.
3. Open the [FastPix dashboard](https://dashboard.fastpix.com) and go to **Video Data**.
4. A few minutes after playback, your session appears with views, startup time, rebuffering, and any errors.

If nothing shows up, see [Troubleshooting](#troubleshooting).

---

## Changing the video source

When you switch to a new video on the same player, call `videoChange(...)` so analytics start a fresh view instead of attributing the new video to the old session:

```kotlin
val newVideoData = CustomerVideoDataEntity().apply {
    videoId = "newId"
    videoTitle = "New Video"
    videoSourceUrl = "newUrl"
}
fastPixBaseMediaPlayer.videoChange(newVideoData)
```

## Error handling

Automatic error tracking is **on by default** - fatal and handled `MediaPlayer` errors are captured for you through the error listener wrapper you configured earlier.

To turn automatic tracking off and report errors yourself:

```kotlin
fastPixBaseMediaPlayer.setAutomaticErrorTracking(false)
```

You can then report a handled error manually with `fastPixBaseMediaPlayer.error(...)`, which takes a `RequestFailureException`. See [`FastPixBaseMediaPlayer`](library/src/main/java/io/fastpix/data/exo/FastPixBaseMediaPlayer.kt) for the full method signatures.

---

## Which FastPix data SDK do I need?

Pick the data SDK that matches the player you ship. All of them report to the same FastPix Video Data dashboard.

| You are using | Use this SDK |
|---|---|
| Android native `MediaPlayer` | **This repo** ([android-media-player-data](https://github.com/FastPix/android-media-player-data)) |
| AndroidX Media3 / ExoPlayer | [android-data-androidXmedia3](https://github.com/FastPix/android-data-androidXmedia3) |
| The FastPix player on Android (data built in) | [fastpix-android-player](https://github.com/FastPix/fastpix-android-player) |
| React Native (Android + iOS) | [react-native-video-data](https://github.com/FastPix/react-native-video-data) |
| An HTML5 web player (Video.js, Shaka, HLS.js, etc.) | [web-data-sdk](https://github.com/FastPix/web-data-sdk) |

Related building blocks:

- [android-core-data-sdk](https://github.com/FastPix/android-core-data-sdk) - the foundational data layer this SDK wraps.
- [android-uploads-sdk](https://github.com/FastPix/android-uploads-sdk) - resumable, direct-to-cloud video uploads for Android.
- [android-StreamGate](https://github.com/FastPix/android-StreamGate) - a ready-to-run Android demo app for capture and upload.
- [fastpix-python](https://github.com/FastPix/fastpix-python) - a server SDK for uploads, live streaming, playback IDs, and in-video AI.

## FAQ

**What is Android media player analytics?**
Android media player analytics means tracking playback performance, user engagement, and quality of service (QoS / QoE) for video played through Android's native MediaPlayer - metrics like startup time, rebuffering, and playback failures. You can collect it from native framework metrics or a dedicated SDK; this repo is the FastPix option for the native MediaPlayer.

**How do I monitor the Android MediaPlayer?**
Wrap your `MediaPlayer` with `FastPixBaseMediaPlayer`, pass your workspace key, and route the player's listeners through the SDK (see [Attach the SDK to your MediaPlayer](#attach-the-sdk-to-your-mediaplayer)). From then on, startup time, rebuffering, errors, and engagement appear in the FastPix dashboard with no manual event code.

**How do I track video analytics in an Android app?**
Install the SDK from GitHub Packages, add the `INTERNET` permission, wrap your `MediaPlayer`, and pass your workspace key. Playback is tracked automatically and sent to the FastPix dashboard. Start at [Add the GitHub Packages repositories](#add-the-github-packages-repositories).

**Does it work with ExoPlayer or AndroidX Media3?**
No - this SDK is for Android's native (legacy) MediaPlayer. For ExoPlayer / Media3, use [android-data-androidXmedia3](https://github.com/FastPix/android-data-androidXmedia3). Both report to the same FastPix dashboard.

**Is this an alternative to Mux Data or api.video for the Android MediaPlayer?**
Yes. Like those, FastPix provides a native tracking wrapper for Android's MediaPlayer that monitors startup time, rebuffering, and playback failures and reports them to the FastPix dashboard. See [Which FastPix data SDK do I need?](#which-fastpix-data-sdk-do-i-need) for other players.

**What is the FastPix Video Data SDK for Android MediaPlayer?**
It is an analytics SDK that wraps Android's native `MediaPlayer` and sends real-time playback and quality-of-experience metrics to the FastPix dashboard, so you can measure how your videos perform on real devices.

**Do I have to switch video players to use it?**
No. It observes the `MediaPlayer` you already use. You do not replace your player or change how you load streams.

**What is the minimum Android version?**
Android 7.0 (API level 24). The published library sets `minSdk` to 24.

**Why do I need a GitHub Personal Access Token to install it?**
The SDK and its core dependency are hosted on GitHub Packages, which requires authentication (a PAT with `read:packages`) even for public packages. This is a GitHub requirement, not a FastPix paywall.

**Why does Gradle say it can't find `io.fastpix.data:core:1.0.1`?**
Because you only added the media-player repository. The SDK depends on the core artifact in a separate GitHub Packages repo. Add both repositories from [Add the GitHub Packages repositories](#add-the-github-packages-repositories).

**Where do I get my workspace key?**
From the FastPix dashboard. Create a free account at [dashboard.fastpix.com](https://dashboard.fastpix.com/signup?utm_source=github&utm_medium=referral&utm_campaign=github_signup) and copy the Workspace Key.

**Why don't play, pause, and seek show up automatically?**
`MediaPlayer` does not expose those transitions to observers, so you report them explicitly by calling `play()`, `pause()`, and `seeking()` right after the matching `MediaPlayer` call, as shown in [Attach the SDK to your MediaPlayer](#attach-the-sdk-to-your-mediaplayer).

**How soon do sessions appear in the dashboard?**
Usually within a few minutes of playback stopping.

**How is it priced?**
FastPix Video Data is billed per minute of monitored video. See [fastpix.com/pricing](https://fastpix.com/pricing).

**Is it open source?**
Yes, under the Apache-2.0 license.

## Troubleshooting

- **No data in the dashboard.** Confirm the `INTERNET` permission is present ([Add the required permissions](#add-the-required-permissions)), that `workspaceKey` is set to your real key, and that you called `play()` at least once. Data can take a few minutes to appear.
- **Build fails resolving `io.fastpix.data:core`.** Add the core GitHub Packages repository ([Add the GitHub Packages repositories](#add-the-github-packages-repositories)).
- **401 / 403 from GitHub Packages.** Your PAT is missing or lacks the `read:packages` scope. [Regenerate it](https://github.com/settings/tokens) and re-run the build.
- **Callbacks stop firing after a while.** You passed inline lambdas to `getOnXxxListener()`. Hold the listeners as fields so they are not garbage-collected (see the note in [Attach the SDK to your MediaPlayer](#attach-the-sdk-to-your-mediaplayer)).

## Documentation

Full FastPix Video Data documentation lives at [fastpix.com/docs](https://fastpix.com/docs), starting with the [Video Data overview](https://fastpix.com/docs/video-data/overview). For monitoring other players, browse the [monitors guides](https://fastpix.com/docs/video-data/monitors/androidx-media3).

## Contributing

Issues and pull requests are welcome. Please use the templates under [`.github/ISSUE_TEMPLATE`](.github/ISSUE_TEMPLATE).

## License

Apache-2.0. See [LICENSE](LICENSE).
