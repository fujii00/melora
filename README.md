<div align="center">

<br />

<img src="Logo.png" alt="Melora" width="160" />

# Melora

### Your music, beautifully played.

Melora is an Android music player for the music stored on your device. Browse your collection, organize playlists, and enjoy a polished listening experience built around your own files.

[Download](#download) · [Features](#features) · [Build](#build) · [Contributing](#contributing) · [License](#license)

</div>

<div align="center">

<img src="meloba%20banner.png" alt="Melora app banner" width="100%" />

</div>

---

## Features

- **Local music library** with songs, albums, artists, folders, playlists, favorites, and listening history.
- **Artwork and metadata** from your audio files, with optional online lookups when local information is missing.
- **Lyrics** with timed and animated presentation where available; lyric lookup may require an internet connection.
- **High quality playback** for formats supported by Melora and the device, with gapless playback, crossfade, playback speed, and silence skipping.
- **Audio tools** including an equalizer, playback statistics, and bit-perfect USB output for compatible DACs.
- **Personalized home screen** with saved playlists, recent music, and recommendations based on your local library.
- **Customizable look** with themes, app icon variants, and profile settings.
- **Private music storage** from device folders, USB, SMB shares, and WebDAV servers.

Melora plays music available to the user as local files or personal library sources. It does not require a streaming account to play local music.

## Download

Download the latest Android APK from [Melora Releases](https://github.com/fujii00/melora/releases). For most Android phones, choose the **arm64-v8a** APK. The universal APK supports more device types but is larger.

To install an APK, allow your browser or file manager to install unknown apps when Android prompts you, then open the downloaded file.

## Build

1. Install Android Studio and its Android SDK.
2. Clone this repository and open it in Android Studio, or build from the project folder with the Gradle wrapper.
3. Build a development APK:

   ```powershell
   .\gradlew.bat :app:assembleDevDebug
   ```

4. Build a production APK:

   ```powershell
   .\gradlew.bat :app:assembleProdRelease
   ```

Production builds are signed with the release keystore configured in the local, gitignored `keystore.properties` file. Never commit or share the keystore or its passwords. Keep a secure backup because future app updates must use the same signing key.

## Contributing

Contributions are welcome. Please read the [Contributing Guide](CONTRIBUTING.md) and [Code of Conduct](CODE_OF_CONDUCT.md) before opening a pull request.

## Disclaimer

Melora is an independent music player. Users are responsible for ensuring they have the right to access and play the files and library sources they use with the app. Optional online artwork, metadata, and lyric lookups are provided by third parties and may be subject to their terms and availability.

## License

Melora is licensed under the [GNU General Public License v3.0](LICENSE).
