# diagnose_solemate.ps1
# Automates the diagnostic flow for SoleMate App

$ConfigPath = "automation_config.json"
if (-not (Test-Path $ConfigPath)) {
    Write-Error "Configuration file not found: $ConfigPath"
    exit 1
}

$Config = Get-Content $ConfigPath | ConvertFrom-Json
$DeviceId = $Config.device_id
$PackageName = $Config.package_name
$MainActivity = $Config.main_activity
$LogFile = $Config.log_file

Write-Host "--- SoleMate Diagnostic Automation ---" -ForegroundColor Cyan

# 1. Verify Device
Write-Host "[1/5] Verifying device $DeviceId..."
$DeviceCheck = adb -s $DeviceId get-state
if ($DeviceCheck -ne "device") {
    Write-Error "Device $DeviceId is not available (State: $DeviceCheck)"
    exit 1
}

# 2. Get Screen Size
Write-Host "[2/5] Fetching screen dimensions..."
$SizeOutput = adb -s $DeviceId shell wm size
if ($SizeOutput -match "(\d+)x(\d+)") {
    $Width = [int]$Matches[1]
    $Height = [int]$Matches[2]
    Write-Host "Detected resolution: $Width x $Height"
} else {
    Write-Warning "Failed to parse screen size. Defaulting to 1080x2460."
    $Width = 1080
    $Height = 2460
}

# 3. Install & Launch
Write-Host "[3/5] Installing and launching app..."

# Background Install Confirmation (Handled via parallel tapping during install)
$InstallTapper = Start-Process adb -ArgumentList "-s", $DeviceId, "shell", "while true; do input tap 288 2090; sleep 1; done" -NoNewWindow -PassThru

# Install
adb -s $DeviceId install -r "build/app/outputs/flutter-apk/app-debug.apk"

# Stop Install Tapper
Stop-Process -Id $InstallTapper.Id -ErrorAction SilentlyContinue

# Auto-Grant Permissions (Bypass dialogs)
Write-Host "Granting Permissions..." -ForegroundColor Gray
adb -s $DeviceId shell pm grant $PackageName android.permission.CAMERA
adb -s $DeviceId shell pm grant $PackageName android.permission.RECORD_AUDIO

# Launch
adb -s $DeviceId shell am start -S -n "$PackageName/$MainActivity"

# 4. Start Logging (Background)
Write-Host "[4/5] Starting log capture to $LogFile..."
# Clear previous log
Clear-Content -Path $LogFile -ErrorAction SilentlyContinue

# Find the PID of the newly launched app
Start-Sleep -Seconds 2
$AppPid = adb -s $DeviceId shell pidof $PackageName
if ($AppPid) {
    Write-Host "App PID: $AppPid" -ForegroundColor Gray
    # Using PID filter for super clean logs
    $LogProcess = Start-Process adb -ArgumentList "-s", $DeviceId, "logcat", "--pid", $AppPid, "-v", "time" -NoNewWindow -RedirectStandardOutput $LogFile -PassThru
} else {
    Write-Warning "Could not find PID for $PackageName. Falling back to full logcat (filtered by tag)."
    $LogProcess = Start-Process adb -ArgumentList "-s", $DeviceId, "logcat", "*:S", "flutter:I", "MainActivity:I", "-v", "time" -NoNewWindow -RedirectStandardOutput $LogFile -PassThru
}

# 5. Execute Automated Steps
Write-Host "[5/5] Executing automated tests..." -ForegroundColor Yellow
Write-Host "NOTE: If taps fail with SecurityException, ensure 'USB Debugging (Security settings)' is ON in Developer Options." -ForegroundColor Gray

foreach ($Step in $Config.steps) {
    Write-Host "  > Action: $($Step.name)"
    
    switch ($Step.action) {
        "wait" {
            Start-Sleep -Milliseconds $Step.duration_ms
        }
        "tap" {
            $TargetX = [int]($Step.rel_x * $Width)
            $TargetY = [int]($Step.rel_y * $Height)
            Write-Host "    Current Tap: ($TargetX, $TargetY)" -ForegroundColor Gray
            # Try to tap, catching any errors
            try {
                adb -s $DeviceId shell input tap $TargetX $TargetY 2>&1 | Out-Default
            } catch {
                Write-Warning "Tap failed: $_"
            }
            # Added wait for garment selection + second tap for Analyze
            if ($Step.name -eq "Action: PROMINENT - Tap Rec Outfit") {
                Write-Host "  > Action: WAITING FOR USER: Select an image for Recommendation..." -ForegroundColor Yellow
                Start-Sleep -Seconds 10
                Write-Host "  > Tapping 'Analyze' now..."
                adb -s $DeviceId shell input tap $TargetX $TargetY 2>&1 | Out-Default
            }
        }
        "key" {
            adb -s $DeviceId shell input keyevent $Step.code
        }
        "swipe" {
            $StartX = [int]($Step.start_x * $Width)
            $StartY = [int]($Step.start_y * $Height)
            $EndX = [int]($Step.end_x * $Width)
            $EndY = [int]($Step.end_y * $Height)
            $Duration = if ($Step.duration_ms) { $Step.duration_ms } else { 500 }

            $WaitAfter = if ($Step.wait_after) { $Step.wait_after / 1000 } else { 2 }
            Start-Sleep -Seconds $WaitAfter
            Write-Host "    Current Swipe: ($StartX, $StartY) -> ($EndX, $EndY) in $($Duration)ms" -ForegroundColor Gray
            adb -s $DeviceId shell input swipe $StartX $StartY $EndX $EndY $Duration
        }
    }
}

Write-Host "`nAutomation sequence complete!" -ForegroundColor Green
Write-Host "Logs are being captured in $LogFile. Monitoring will continue until you stop it."
Write-Host "Log process ID: $($LogProcess.Id)"

# Optional: Wait a bit more for finishing logs
# Wait for final logs to settle
Start-Sleep -Seconds 30
# Write-Host "Stopping log capture..."
# Stop-Process -Id $LogProcess.Id -ErrorAction SilentlyContinue

# Keep terminal open if needed, or exit
# For background logging to persist, we don't need to stay in loop here if Start-Process worked.
