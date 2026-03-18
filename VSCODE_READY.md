# VS Code Android Development - Quick Reference

## ✅ What's Already Set Up

### Environment
- **Java JDK 17**: `C:\Java\jdk-17`
- **Android SDK**: `C:\Android\Sdk`  
- **ADB (Android Debug Bridge)**: Working ✓
- **Environment Variables**: Configured ✓
- **VS Code Tasks**: Ready ✓

### Available Commands
```powershell
# Build MusicTube
.\gradlew.bat assembleDebug

# Install on device
.\gradlew.bat installDebug

# Clean project
.\gradlew.bat clean

# Check connected devices
adb devices
```

## 🔧 Complete These Steps

### 1. Install VS Code Extensions
Open VS Code and install these extensions:
- **Extension Pack for Java** (ms-vscode.vscode-java-pack)
- **Gradle Language Support** (naco-siren.gradle-language)  
- **XML Tools** (DotJoshJohnson.xml)

### 2. VS Code Tasks (Already Configured)
Press `Ctrl+Shift+P` → `Tasks: Run Task`:
- **Build MusicTube Debug** - Compile the app
- **Install MusicTube Debug** - Install on device
- **Clean Project** - Clean build files

### 3. Device Setup Options

#### Option A: Physical Device (Recommended)
1. Enable Developer Options on Android device
2. Enable USB Debugging  
3. Connect via USB
4. Run: `adb devices` to verify connection

#### Option B: Android Emulator (Optional)
```powershell
# Create emulator (if needed later)
avdmanager create avd -n "MusicTube_Test" -k "system-images;android-34;google_apis;x86_64"

# Start emulator  
emulator -avd MusicTube_Test
```

## 🚀 Testing MusicTube

### First Build Test
1. Open terminal in VS Code (`Ctrl+``)
2. Run: `.\gradlew.bat assembleDebug`
3. Look for: `BUILD SUCCESSFUL`

### Install and Run
1. Connect Android device or start emulator
2. Run: `.\gradlew.bat installDebug`
3. Find "MusicTube" app on device and launch

### Development Workflow
1. Make code changes in VS Code
2. Save files (`Ctrl+S`)
3. Run build task (`Ctrl+Shift+P` → Tasks)
4. Install updated APK on device

## 🐛 Troubleshooting

### Build Issues
```powershell
# Clean and rebuild
.\gradlew.bat clean
.\gradlew.bat assembleDebug
```

### Device Not Found
```powershell
# Check device connection
adb devices

# Restart ADB server
adb kill-server
adb start-server
```

### Java Issues
```powershell
# Verify Java installation
java -version
# Should show: openjdk version "17.0.2"
```

## 📱 MusicTube Features to Test

### ✅ Working Features
- **Local Music Playback**: Scans device music files
- **Search Interface**: Demo YouTube-style search
- **Modern UI**: Material Design, dark theme
- **Background Playback**: Music continues when app minimized
- **Download Simulation**: Shows download progress (mock)

### 🔧 Development Areas
- **Real YouTube Integration**: Replace mock SearchService
- **Audio Download**: Implement actual file downloading  
- **Playlists**: Build playlist management
- **Equalizer**: Add audio controls

## 💡 VS Code Tips for Android

### Useful Shortcuts
- `Ctrl+Shift+P`: Command palette
- `Ctrl+``: Open terminal
- `F5`: Debug (after configuring)
- `Ctrl+Shift+X`: Extensions marketplace

### Recommended Workspace Settings
Already configured in `.vscode/settings.json`:
- Auto-format on save
- Organize imports
- Java configuration
- File exclusions for build folders

## 🎯 You're Ready!
Your MusicTube project is now set up for VS Code development. Just install the extensions and start building!