# Log Capture Instructions

## Quick Start

1. **Connect your device:**
   - Via USB: Enable USB debugging and connect via USB cable
   - Via WiFi: `adb connect <YOUR_DEVICE_IP>:5555` (requires WiFi debugging enabled)

2. **Run the log capture script:**
   ```powershell
   .\capture_logs.ps1
   ```

3. **Launch the AR activity** in your app

4. **Stop logging** with Ctrl+C when done

## Manual Log Capture

If you prefer to run commands manually:

```powershell
# Clear log buffer
adb logcat -c

# Start capturing (saves to logs/ directory)
adb logcat -s ARActivity:V SimpleRenderer:V VioPoseProvider:V Camera2Manager:V VioEngine:V BackgroundRenderer:V *:E | Tee-Object -FilePath "logs\ar_session_$(Get-Date -Format 'yyyyMMdd_HHmmss').txt"
```

## What to Look For

After running the app, check the logs for:

- **ARActivity**: Session initialization, mode selection (ARCore vs VIO)
- **SimpleRenderer**: Rendering issues, texture problems, camera ready state
- **Camera2Manager**: Camera opening, frame capture, IMU sensor data
- **VioPoseProvider**: VIO initialization, tracking status, fallback mode
- **BackgroundRenderer**: Texture rendering, shader compilation
- **Errors (*:E)**: Any critical errors

## Common Issues to Check

1. **Black screen**: Look for texture binding errors, SurfaceTexture issues
2. **Camera not working**: Check Camera2Manager logs for permission/initialization errors
3. **VIO not tracking**: Check VioEngine and VioPoseProvider for initialization failures
4. **ARCore issues**: Check ARActivity for session creation/configuration errors




