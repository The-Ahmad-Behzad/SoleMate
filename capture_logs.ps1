# Logcat capture script for SoleMate AR debugging
# Run this script after connecting your device and launching the app

Write-Host "Checking for connected devices..." -ForegroundColor Yellow
$devices = adb devices
Write-Host $devices

if ($devices -match "device$") {
    Write-Host "`nDevice found! Starting logcat capture..." -ForegroundColor Green
    Write-Host "Launch the AR activity in the app now, then press Ctrl+C to stop logging`n" -ForegroundColor Cyan
    
    $logFile = "logs\ar_session_$(Get-Date -Format 'yyyyMMdd_HHmmss').txt"
    
    # Clear logcat buffer
    adb logcat -c
    
    # Capture logs with filters for AR-related components
    adb logcat -s ARActivity:V SimpleRenderer:V VioPoseProvider:V Camera2Manager:V VioEngine:V BackgroundRenderer:V ArcorePoseProvider:V PlaneRenderer:V ShoeRenderer:V *:E | Tee-Object -FilePath $logFile
    
} else {
    Write-Host "`nNo device connected!" -ForegroundColor Red
    Write-Host "`nTo connect your device:" -ForegroundColor Yellow
    Write-Host "1. Enable USB debugging on your Android device"
    Write-Host "2. Connect via USB: adb devices"
    Write-Host "3. OR connect via WiFi: adb connect <IP>:5555"
    Write-Host "`nThen run this script again." -ForegroundColor Cyan
}




