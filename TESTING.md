# MusicTube App Testing Guide

## Prerequisites for Testing

### 1. Install Android Studio
- Download from: https://developer.android.com/studio
- Install with Android SDK and emulator support

### 2. Set Up Environment Variables
Add to your system PATH:
- `C:\Users\[USERNAME]\AppData\Local\Android\Sdk\platform-tools` (for ADB)
- `C:\Users\[USERNAME]\AppData\Local\Android\Sdk\tools\bin` (for Android tools)

### 3. Install JDK 17
- Download Oracle JDK 17 or OpenJDK 17
- Set JAVA_HOME environment variable

## Testing Steps

### Option 1: Android Studio (Recommended)
1. Open Android Studio
2. Click "Open an existing project"
3. Select the `myMusicTube` folder
4. Wait for Gradle sync to complete
5. Click the "Run" button (green play icon)
6. Select an emulator or connected device

### Option 2: Command Line Testing
```bash
# Navigate to project directory
cd C:\Users\kik0mch1ng\Documents\myMusicTube

# Build the project
.\gradlew.bat build

# Install on connected device/emulator
.\gradlew.bat installDebug

# Run tests
.\gradlew.bat test
```

### Option 3: APK Generation
```bash
# Generate debug APK
.\gradlew.bat assembleDebug

# APK will be created at: app/build/outputs/apk/debug/app-debug.apk
```

## Testing Features

### 1. Permissions Test
- App should request storage permissions on first launch
- Grant "Allow access to media and files"

### 2. Local Music Scanning
- Place some MP3 files in your device's Music folder
- Open the app - it should scan and display local music files
- Test playback by tapping a song

### 3. Search Functionality
- Tap the search icon or "+" button
- Enter a search query
- Should see mock search results
- Test download buttons (currently saves metadata only)

### 4. Player Controls
- Play a song and navigate to the player screen
- Test play/pause, seek controls
- Check background playback by minimizing the app

### 5. Database Operations
- Like/unlike songs
- Check if preferences persist after app restart

## Expected Behavior

### ✅ Working Features:
- Local music file scanning and display
- Music playback with ExoPlayer
- Modern Material Design UI
- Navigation between screens
- Mock search results display
- Database storage of song metadata

### ⚠️ Limited Features:
- Search returns demo results (not real YouTube data)
- Download button saves metadata only (no actual file download)
- No playlist management yet
- No equalizer controls

## Troubleshooting

### Build Issues:
- Ensure JDK 17 is installed and JAVA_HOME is set
- Check Android SDK is properly configured
- Try "Clean Project" in Android Studio

### Runtime Issues:
- Grant all requested permissions
- Ensure device has sufficient storage
- Check if music files are in standard locations

### No Music Found:
- Add MP3 files to device's Music, Downloads, or Documents folders
- Re-scan by reopening the app

## Performance Testing

### Memory Usage:
- Monitor app memory usage while playing music
- Test with large music libraries (100+ songs)

### Battery Impact:
- Test background playback battery consumption
- Verify proper audio focus handling

### UI Responsiveness:
- Test scrolling through large song lists
- Verify smooth navigation transitions
- Check touch responsiveness on all screens

## Next Steps for Full Functionality

1. **Real YouTube Search**: Replace mock SearchService with YouTube Data API v3
2. **Audio Download**: Implement actual file downloading with NewPipeExtractor
3. **Playlist Management**: Enable create/edit playlists functionality
4. **Enhanced Player**: Add shuffle, repeat, queue management
5. **Offline Storage**: Implement proper file management for downloaded content