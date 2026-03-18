# MusicTube - Ad-Free Music Player

MusicTube is a modern Android music player app built with Kotlin and Jetpack Compose. It provides local music playback capabilities and online music search functionality, similar to Mixtube but without ads.

## Features

### ✅ Implemented
- **Local Music Player**: Play music files stored on your device
- **Modern UI**: Built with Jetpack Compose and Material Design
- **Music Library**: Browse songs by title, artist, and album
- **Search Functionality**: Search and browse music online (demo implementation)
- **Background Playback**: Continue playing music while using other apps
- **Ad-Free Experience**: Clean interface without advertisements
- **Database Storage**: Room database for managing music library
- **Dependency Injection**: Hilt for clean architecture

### 🚧 To Be Implemented
- **Real Music Search**: Integration with YouTube API or similar services
- **Audio Download**: Download and save music from online sources
- **Playlists**: Create and manage custom playlists
- **Equalizer**: Built-in audio equalizer
- **Lyrics Display**: Show song lyrics
- **Offline Cache**: Cache downloaded music for offline playback
- **Widget Support**: Home screen widget for music controls

## Architecture

The app follows modern Android development best practices:

- **MVVM Architecture**: Clean separation of concerns
- **Jetpack Compose**: Modern declarative UI framework
- **Room Database**: Local data persistence
- **Hilt**: Dependency injection
- **ExoPlayer**: High-quality audio playback
- **Coroutines & Flow**: Reactive programming for async operations

## Project Structure

```
app/src/main/java/com/musictube/player/
├── data/
│   ├── database/        # Room database, DAOs
│   ├── model/          # Data models (Song, Playlist, etc.)
│   └── repository/     # Data repositories
├── di/                 # Hilt dependency injection modules
├── service/            # Background services (Player, Search)
├── ui/
│   ├── component/      # Reusable UI components
│   ├── screen/         # App screens (Home, Player, Search)
│   └── theme/          # App theming and colors
└── viewmodel/          # ViewModels for UI state management
```

## Getting Started

### Prerequisites
- Android Studio (latest version)
- Android SDK API 24+ (Android 7.0)
- Kotlin 1.9.22+

### Building the App

1. Clone this repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Run the app on an emulator or physical device

### Permissions

The app requires the following permissions:
- `READ_EXTERNAL_STORAGE` / `READ_MEDIA_AUDIO`: Access local music files
- `INTERNET`: Search and download music online
- `FOREGROUND_SERVICE`: Background music playback
- `WAKE_LOCK`: Keep device awake during playback

## Usage

1. **Grant Permissions**: Allow access to media files when prompted
2. **Local Music**: The app automatically scans for music files on your device
3. **Search Music**: Use the search feature to find new songs (demo implementation)
4. **Play Music**: Tap any song to start playing
5. **Player Controls**: Use the full-screen player for advanced controls

## Development Notes

### Search Implementation
The current search functionality is a mock implementation for demonstration purposes. In a production app, you would integrate with:
- YouTube Data API v3
- Spotify Web API
- Other music streaming APIs

### Download Functionality
Audio download features are currently placeholders. Real implementation would require:
- Legal compliance with content policies
- Proper audio extraction libraries
- File management and storage handling

### Disclaimer
This app is for educational and personal use only. Ensure compliance with copyright laws and terms of service of any music platforms you integrate with.

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

## License

This project is for educational purposes. Please respect copyright laws and platform terms of service.