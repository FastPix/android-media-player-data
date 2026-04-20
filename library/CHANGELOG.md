# Changelog

All notable changes to this project are documented in this file.

## [1.0.0] — 2025-06-25

First published release of the FastPix **Media Player** Android library (`io.fastpix.data:mediaplayer`).

### Added

- **`FastPixBaseMediaPlayer`**: analytics wrapper around Android [`MediaPlayer`](https://developer.android.com/reference/android/media/MediaPlayer) that wires playback lifecycle into FastPix metrics and streaming.
- **Customer and session data**: support for `CustomerDataEntity`, `CustomOptions`, and related entities from **`io.fastpix.data:core:1.0.1`** for dashboard-aligned analytics.
- **Playback and quality signals**: hooks for play, pause, seek, completion, buffering, resolution / video size changes, and network-aware context where applicable.
- **Errors**: automatic error reporting with optional manual reporting and toggles for automatic error tracking.
- **Build metadata**: `BuildConfig.LIBRARY_VERSION` exposed for runtime version reporting (requires `LIBRARY_VERSION` to be defined for Gradle configuration, e.g. in `gradle.properties`).

[1.0.0]: https://github.com/FastPix/android-media-player-data/releases/tag/v1.0.0
