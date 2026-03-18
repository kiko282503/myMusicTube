# VS Code Android Development Setup

## Required Components

### 1. Install Java Development Kit (JDK)
```powershell
# Install via Chocolatey (if you have it)
choco install openjdk17

# Or download from: https://adoptium.net/temurin/releases/
```

### 2. Install Android SDK Command Line Tools
```powershell
# Download SDK Command Line Tools
# From: https://developer.android.com/studio/index.html#command-tools

# Create Android SDK directory
mkdir C:\Android\Sdk
cd C:\Android\Sdk

# Extract command line tools to cmdline-tools\latest\
```

### 3. Set Environment Variables
```powershell
# Add to System Environment Variables
$env:ANDROID_HOME = "C:\Android\Sdk"
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.7.7-hotspot"

# Add to PATH
$env:PATH += ";C:\Android\Sdk\platform-tools"
$env:PATH += ";C:\Android\Sdk\cmdline-tools\latest\bin"
$env:PATH += ";C:\Android\Sdk\build-tools\34.0.0"
```

### 4. Install Android SDK Components
```powershell
# Accept licenses
sdkmanager --licenses

# Install required packages
sdkmanager "platforms;android-34"
sdkmanager "platform-tools"
sdkmanager "build-tools;34.0.0"
sdkmanager "emulator"
sdkmanager "system-images;android-34;google_apis;x86_64"
```

### 5. VS Code Extensions
Install these extensions:
- **Extension Pack for Java** (by Microsoft)
- **Android iOS Emulator** (by DiemasMichiels)  
- **Gradle Language Support** (by naco-siren)
- **XML Tools** (by Josh Johnson)

### 6. Create Android Virtual Device (AVD)
```powershell
# Create AVD
avdmanager create avd -n "MusicTube_Test" -k "system-images;android-34;google_apis;x86_64"

# Start emulator
emulator -avd MusicTube_Test
```

## VS Code Workflow

### Building
```powershell
# Build debug APK
.\gradlew.bat assembleDebug

# Install on device/emulator  
.\gradlew.bat installDebug

# Run tests
.\gradlew.bat test
```

### Debugging
```powershell
# View logs
adb logcat

# Install APK manually
adb install app\build\outputs\apk\debug\app-debug.apk

# Forward ports for debugging
adb forward tcp:8080 tcp:8080
```

## VS Code Tasks Configuration
Create `.vscode/tasks.json`:
```json
{
    "version": "2.0.0",
    "tasks": [
        {
            "label": "Build Debug APK",
            "type": "shell",
            "command": ".\\gradlew.bat",
            "args": ["assembleDebug"],
            "group": "build",
            "problemMatcher": []
        },
        {
            "label": "Install Debug APK",
            "type": "shell", 
            "command": ".\\gradlew.bat",
            "args": ["installDebug"],
            "group": "build"
        },
        {
            "label": "Start Emulator",
            "type": "shell",
            "command": "emulator",
            "args": ["-avd", "MusicTube_Test"],
            "isBackground": true
        }
    ]
}
```

## Limitations of VS Code Approach

### Missing Features
- ❌ **Visual Layout Editor**: No drag-drop UI design
- ❌ **Integrated Emulator**: Must manage separately  
- ❌ **Device Manager**: Manual ADB commands needed
- ❌ **Performance Profiling**: No built-in tools
- ❌ **Android-specific IntelliSense**: Limited completion

### More Complex
- 🔧 **Manual SDK Management**: Update tools manually
- 🔧 **Emulator Setup**: Command-line AVD creation
- 🔧 **Debugging**: Less integrated debugging experience
- 🔧 **File Changes**: Manual APK rebuild/install cycle

## Recommendation

### For MusicTube Project:
**Use Android Studio** - Your app is already perfectly configured for it, and you'll get:
- ✅ **Immediate productivity**: Everything works out of the box
- ✅ **Visual debugging**: See UI changes instantly
- ✅ **Better testing**: Easy device switching and debugging
- ✅ **Professional workflow**: Industry-standard Android development

### When to Use VS Code:
- 🎯 **Cross-platform projects**: Flutter, React Native, Cordova
- 🎯 **Simple scripts**: Quick automation or utility apps
- 🎯 **Team preference**: If your team is heavily VS Code focused
- 🎯 **Resource constraints**: Limited disk space or older hardware