# Quick Setup Script for MusicTube Development

Write-Host "🎵 MusicTube Development Environment Setup" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Green

# Function to check if software is installed
function Test-Software($name, $command) {
    try {
        & $command --version 2>$null | Out-Null
        Write-Host "✅ $name is installed" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "❌ $name is not installed" -ForegroundColor Red
        return $false
    }
}

# Function to download file with progress
function Download-WithProgress($url, $output) {
    Write-Host "📥 Downloading $(Split-Path $output -Leaf)..." -ForegroundColor Yellow
    try {
        Invoke-WebRequest -Uri $url -OutFile $output -UseBasicParsing
        Write-Host "✅ Download completed" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "❌ Download failed: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

Write-Host "`n🔍 Checking current environment..."

# Check Java
$javaInstalled = Test-Software "Java" "java"

# Check Android Studio
$androidStudioPath = "C:\Program Files\Android\Android Studio\bin\studio64.exe"
$androidStudioInstalled = Test-Path $androidStudioPath

# Check ADB (Android Debug Bridge)
$adbInstalled = Test-Software "ADB" "adb"

Write-Host "`n📋 Installation Plan:" -ForegroundColor Cyan

if (!$androidStudioInstalled) {
    Write-Host "1. Download and install Android Studio" -ForegroundColor Yellow
    $androidStudioUrl = "https://redirector.gvt1.com/edgedl/android/studio/install/2023.1.1.28/android-studio-2023.1.1.28-windows.exe"
    $androidStudioInstaller = "$env:TEMP\android-studio-installer.exe"
    
    $downloadChoice = Read-Host "Download Android Studio now? (y/n)"
    if ($downloadChoice -eq 'y' -or $downloadChoice -eq 'Y') {
        if (Download-WithProgress $androidStudioUrl $androidStudioInstaller) {
            Write-Host "🚀 Starting Android Studio installer..." -ForegroundColor Green
            Start-Process -FilePath $androidStudioInstaller -Wait
            Write-Host "✅ Android Studio installation completed" -ForegroundColor Green
        }
    } else {
        Write-Host "⚠️  Manual installation required:" -ForegroundColor Yellow
        Write-Host "   Download from: https://developer.android.com/studio" -ForegroundColor Gray
    }
} else {
    Write-Host "✅ Android Studio is already installed" -ForegroundColor Green
}

Write-Host "`n🎯 Next Steps:" -ForegroundColor Cyan
Write-Host "1. Launch Android Studio from Start Menu" -ForegroundColor White
Write-Host "2. Complete initial setup wizard" -ForegroundColor White  
Write-Host "3. Open existing project: $PWD" -ForegroundColor White
Write-Host "4. Wait for Gradle sync to complete" -ForegroundColor White
Write-Host "5. Click Run button to test MusicTube app" -ForegroundColor White

Write-Host "`n📱 Device Setup:" -ForegroundColor Cyan
Write-Host "Option A - Use Emulator:" -ForegroundColor White
Write-Host "  • Tools → AVD Manager → Create Virtual Device" -ForegroundColor Gray
Write-Host "  • Choose Pixel 6, API 34, Standard configuration" -ForegroundColor Gray

Write-Host "`nOption B - Use Physical Device:" -ForegroundColor White
Write-Host "  • Enable Developer Options (tap Build Number 7 times)" -ForegroundColor Gray
Write-Host "  • Enable USB Debugging" -ForegroundColor Gray
Write-Host "  • Connect via USB and allow debugging" -ForegroundColor Gray

Write-Host "`n🔧 Environment Variables (Optional):" -ForegroundColor Cyan
Write-Host "Add to PATH for command line access:" -ForegroundColor White
Write-Host "  • C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools" -ForegroundColor Gray
Write-Host "  • C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\tools\bin" -ForegroundColor Gray

Write-Host "`n🧪 Test Commands After Setup:" -ForegroundColor Cyan
Write-Host ".\gradlew.bat build          # Build project" -ForegroundColor Gray
Write-Host ".\gradlew.bat installDebug   # Install on device" -ForegroundColor Gray
Write-Host "adb devices                  # List connected devices" -ForegroundColor Gray

Write-Host "`n📖 Need Help?" -ForegroundColor Cyan
Write-Host "• Read ANDROID_STUDIO_SETUP.md for detailed instructions" -ForegroundColor Gray
Write-Host "• Read TESTING.md for app testing guide" -ForegroundColor Gray
Write-Host "• Check README.md for project overview" -ForegroundColor Gray

if ($androidStudioInstalled) {
    Write-Host "`n🚀 Ready to launch Android Studio?" -ForegroundColor Green
    $launchChoice = Read-Host "Open Android Studio now? (y/n)"
    if ($launchChoice -eq 'y' -or $launchChoice -eq 'Y') {
        Write-Host "🎉 Launching Android Studio..." -ForegroundColor Green
        Start-Process -FilePath $androidStudioPath -ArgumentList $PWD
    }
}

Write-Host "🎵 MusicTube is ready for development!" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Green