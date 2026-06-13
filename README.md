# Chizuki

A modern Android streaming app for discovering and watching movies and TV shows.

![Platform](https://img.shields.io/badge/Platform-Android-green.svg)
![Language](https://img.shields.io/badge/Language-Kotlin-blue.svg)
![MinSDK](https://img.shields.io/badge/MinSDK-26-orange.svg)

## Features

- **Explore** - Browse trending, popular, and top-rated movies and TV shows
- **Search** - Find content instantly with debounced search
- **Streaming** - Watch with built-in HLS player (ExoPlayer)
- **Episode Selection** - Season and episode picker for TV shows
- **Auto-next Episode** - Seamless playback continuation

## Screenshots

<p align="center">
  <img src="./screenshots/schedule.png" width="30%" />
  <img src="./screenshots/explore.png" width="30%" />
  <img src="./screenshots/home.png" width="30%" />
</p>

<p align="center">
  <img src="./screenshots/player.png" width="91%" />
</p>

## Building from source

1. Clone the repo
2. Create a `local.properties` file in the project root with your TMDB API key:
   ```properties
   TMDB_API_KEY=your_tmdb_api_key_here
   ```
3. Open in Android Studio or build with `./gradlew assembleRelease`

> **Note:** `local.properties` is gitignored. Release signing keys are also configured via this file. Copy the format from `app/build.gradle.kts`.

## Requirements

- Android 8.0+ (API 28+)

## Installation

Download the APK from [Releases](https://github.com/Suntrax/chizuki/releases) and install.

## Tech Stack

- Kotlin
- Media3 ExoPlayer
- TMDB API
- Glide
- MVVM Architecture

## Disclaimer

This app is for educational purposes only. I do not host, upload, or distribute any content. All streaming links are provided by third-party sources.
