# Android Studio Setup Guide for MusicTube

## 📥 Step 1: Download Android Studio

### Download Link
- **Official Site**: https://developer.android.com/studio
- **Direct Download**: https://developer.android.com/studio#downloads
- **Version**: Android Studio Hedgehog | 2023.1.1 or newer

### System Requirements
- **Windows**: Windows 10/11 (64-bit)
- **RAM**: 8 GB minimum, 16 GB recommended  
- **Disk**: 4 GB for Android Studio + 2 GB for Android SDK
- **Resolution**: 1280 x 800 minimum

## 🛠️ Step 2: Install Android Studio

### Installation Process
1. **Run the installer** (`android-studio-2023.1.1.28-windows.exe`)
2. **Follow setup wizard**:
   - ✅ Install Android Studio
   - ✅ Install Android SDK
   - ✅ Install Android Virtual Device (AVD)
   - ✅ Install Intel HAXM (for emulator acceleration)

3. **Choose installation location** (default is fine):
   - Android Studio: `C:\Program Files\Android\Android Studio`
   - Android SDK: `C:\Users\[USERNAME]\AppData\Local\Android\Sdk`

## 🎯 Step 3: First Launch Setup

### Initial Configuration
1. **Launch Android Studio**
2. **Import Settings**: Choose "Do not import settings" if first time
3. **Setup Wizard**:
   - Choose "Standard" setup type
   - Select UI theme (Darcula recommended)
   - Verify Android SDK location
   - Accept license agreements
   - Wait for initial downloads (SDK, build tools, etc.)

### SDK Configuration  
Android Studio will automatically download:
- **Android API 34** (Target for MusicTube)
- **Android API 24** (Minimum for MusicTube) 
- **Build Tools 34.0.0**
- **Platform Tools** (ADB, Fastboot)
- **Emulator**

## 📱 Step 4: Set Up Android Emulator

### Create Virtual Device
1. **Open AVD Manager**: Tools → AVD Manager
2. **Create Virtual Device**:
   - **Phone**: Pixel 6 or similar
   - **System Image**: API 34 (Android 14) with Google Play
   - **Configuration**: 
     - RAM: 4 GB
     - Storage: 6 GB
     - Graphics: Hardware - GLES 2.0

### Alternative: Physical Device
1. **Enable Developer Options** on your Android phone:
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
2. **Enable USB Debugging**: Settings → Developer Options → USB Debugging
3. **Connect via USB** and allow debugging when prompted

## 🚀 Step 5: Open MusicTube Project

### Open Project
1. **Launch Android Studio**
2. **Open Project**: Choose "Open an existing project"
3. **Navigate to**: `C:\Users\kik0mch1ng\Documents\myMusicTube`
4. **Click OK**

### Initial Project Setup
Android Studio will automatically:
- ✅ Sync Gradle files (may take 5-10 minutes first time)
- ✅ Download dependencies (ExoPlayer, Room, Hilt, etc.)
- ✅ Index files
- ✅ Configure build variants

## ▶️ Step 6: Run the App

### Build and Run
1. **Wait for Gradle sync** to complete (bottom status bar)
2. **Select device/emulator** from dropdown next to run button
3. **Click Run button** (green play icon) or press `Shift+F10`
4. **Grant permissions** when app launches on device

### Build Variants
- **Debug**: For testing (default)
- **Release**: For production (needs signing)

## 🔧 Step 7: Environment Variables (Optional)

### Add to System PATH
For command-line access, add these to your PATH:
```
C:\Users\[USERNAME]\AppData\Local\Android\Sdk\platform-tools
C:\Users\[USERNAME]\AppData\Local\Android\Sdk\tools\bin
```

### Set JAVA_HOME
Android Studio includes JDK, but for command line:
```
JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
```

## ⚡ Quick Commands After Setup

### From Project Directory
```powershell
# Build project
.\gradlew.bat build

# Install debug APK
.\gradlew.bat installDebug  

# Generate release APK
.\gradlew.bat assembleRelease

# Clean project
.\gradlew.bat clean
```

## 🐛 Troubleshooting

### Common Issues

**Gradle Sync Failed**
- Check internet connection
- Try: File → Invalidate Caches and Restart
- Check Java version (should use bundled JDK)

**Emulator Won't Start**
- Enable Hyper-V in Windows features
- Or disable Hyper-V if using other virtualization
- Try creating new AVD with different API level

**ADB Not Found**
- Restart Android Studio
- Check PATH variables
- Manually add SDK platform-tools to PATH

**Build Errors**
- Clean project: Build → Clean Project
- Rebuild: Build → Rebuild Project
- Check Gradle version compatibility

### Performance Tips
- **Increase Android Studio Memory**: Help → Edit Custom VM Options
  ```
  -Xms2g
  -Xmx8g
  ```
- **Enable Offline Mode**: File → Settings → Build → Gradle → Offline work
- **Use SSD** for better performance
- **Close unnecessary projects** in Android Studio

## ✅ Verification Steps

### Test Installation
1. **Project opens** without errors
2. **Gradle sync** completes successfully  
3. **No red underlines** in code files
4. **Run button** is enabled
5. **Emulator/device** appears in dropdown

### Test MusicTube App
1. **App installs** on device/emulator
2. **Permissions granted** for media access
3. **Home screen** loads with music library
4. **Search screen** opens and shows demo results
5. **Player screen** displays with controls

## 🎉 Ready to Develop!

Once setup is complete, you can:
- **Modify code** and see changes instantly
- **Debug** with breakpoints and logcat
- **Test** on multiple devices/API levels
- **Profile** app performance
- **Generate APK** for distribution

Your MusicTube app is now ready for full development and testing!