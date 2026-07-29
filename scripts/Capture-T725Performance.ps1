[CmdletBinding()]
param(
    [ValidateRange(1, 30)]
    [int]$DurationMinutes = 5,

    [string]$PackageName = "com.ropeskill.app",

    [string]$AdbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",

    [string]$OutputRoot = "$env:USERPROFILE\Downloads"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Invoke-AdbText {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $output = & $AdbPath @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "ADB command failed: adb $($Arguments -join ' ')`n$($output -join [Environment]::NewLine)"
    }

    return ($output -join [Environment]::NewLine)
}

function Get-RegexValue {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Text,

        [Parameter(Mandatory = $true)]
        [string]$Pattern
    )

    $match = [regex]::Match($Text, $Pattern)
    if ($match.Success) {
        return $match.Groups[1].Value
    }

    return ""
}

if (-not (Test-Path $AdbPath)) {
    throw "ADB was not found at '$AdbPath'. Open Android Studio once or pass -AdbPath with the correct path."
}

$deviceOutput = & $AdbPath devices 2>&1
if ($LASTEXITCODE -ne 0) {
    throw "Unable to list Android devices.`n$($deviceOutput -join [Environment]::NewLine)"
}

$connectedDevices = @($deviceOutput | Where-Object { $_ -match "`tdevice$" })
if ($connectedDevices.Count -ne 1) {
    throw "Connect exactly one unlocked Android device with USB debugging enabled. Current device list:`n$($deviceOutput -join [Environment]::NewLine)"
}

$runStamp = Get-Date -Format "yyyyMMdd-HHmmss"
$outputDirectory = Join-Path $OutputRoot "RopeSkill-T725-$runStamp"
$zipPath = "$outputDirectory.zip"
$remoteScreenshot = "/sdcard/Download/RopeSkill_T725_capture.png"

New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null

$deviceInfo = @(
    "Captured: $(Get-Date -Format o)"
    "Model: $(Invoke-AdbText -Arguments @('shell', 'getprop', 'ro.product.model'))"
    "Android: $(Invoke-AdbText -Arguments @('shell', 'getprop', 'ro.build.version.release'))"
    "SDK: $(Invoke-AdbText -Arguments @('shell', 'getprop', 'ro.build.version.sdk'))"
    "Package: $PackageName"
    "DurationMinutes: $DurationMinutes"
)
$deviceInfo | Set-Content -Path (Join-Path $outputDirectory "device-and-test-info.txt") -Encoding UTF8

$summaryRows = @()

for ($minute = 0; $minute -le $DurationMinutes; $minute++) {
    $checkpoint = "minute-$minute"
    $checkpointDirectory = Join-Path $outputDirectory $checkpoint
    New-Item -ItemType Directory -Path $checkpointDirectory -Force | Out-Null

    Write-Host "Capturing T-725 checkpoint $checkpoint of minute-$DurationMinutes..."

    $batteryText = Invoke-AdbText -Arguments @("shell", "dumpsys", "battery")
    $memoryText = Invoke-AdbText -Arguments @("shell", "dumpsys", "meminfo", $PackageName)
    $cpuText = Invoke-AdbText -Arguments @("shell", "dumpsys", "cpuinfo")
    $thermalText = Invoke-AdbText -Arguments @("shell", "dumpsys", "thermalservice")
    $appProcessIdText = Invoke-AdbText -Arguments @("shell", "pidof", $PackageName)

    $batteryText | Set-Content -Path (Join-Path $checkpointDirectory "battery.txt") -Encoding UTF8
    $memoryText | Set-Content -Path (Join-Path $checkpointDirectory "memory.txt") -Encoding UTF8
    $cpuText | Set-Content -Path (Join-Path $checkpointDirectory "cpu.txt") -Encoding UTF8
    $thermalText | Set-Content -Path (Join-Path $checkpointDirectory "thermal.txt") -Encoding UTF8

    $appProcessId = (($appProcessIdText.Trim() -split "\s+") | Select-Object -First 1)
    if ($appProcessId) {
        $topText = Invoke-AdbText -Arguments @("shell", "top", "-b", "-n", "1", "-p", $appProcessId)
        $topText | Set-Content -Path (Join-Path $checkpointDirectory "top.txt") -Encoding UTF8
    }
    else {
        "App process not found at $checkpoint." |
            Set-Content -Path (Join-Path $checkpointDirectory "top.txt") -Encoding UTF8
    }

    Invoke-AdbText -Arguments @("shell", "screencap", "-p", $remoteScreenshot) | Out-Null
    $localScreenshot = Join-Path $checkpointDirectory "screen.png"
        $savedErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        $pullOutput = & $AdbPath pull $remoteScreenshot $localScreenshot 2>&1
        $pullExitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $savedErrorActionPreference
    }

    if ($pullExitCode -ne 0) {
        throw "Unable to pull the phone screenshot.`n$($pullOutput -join [Environment]::NewLine)"
    }

    $batteryLevel = Get-RegexValue -Text $batteryText -Pattern "(?m)^\s*level:\s*(\d+)"
    $temperatureTenths = Get-RegexValue -Text $batteryText -Pattern "(?m)^\s*temperature:\s*(\d+)"
    $totalPssKb = Get-RegexValue -Text $memoryText -Pattern "TOTAL PSS:\s*(\d+)"

    if (-not $totalPssKb) {
        $totalPssKb = Get-RegexValue -Text $memoryText -Pattern "(?m)^\s*TOTAL\s+(\d+)"
    }

    $temperatureCelsius = ""
    if ($temperatureTenths) {
        $temperatureCelsius = ([double]$temperatureTenths / 10).ToString("0.0")
    }

    $summaryRows += [pscustomobject]@{
        Checkpoint       = $checkpoint
        CapturedAt       = Get-Date -Format o
        BatteryPercent   = $batteryLevel
        BatteryTempC     = $temperatureCelsius
        TotalPssKb       = $totalPssKb
        AppProcessId     = $appProcessId
    }

    if ($minute -lt $DurationMinutes) {
        Start-Sleep -Seconds 60
    }
}

$summaryPath = Join-Path $outputDirectory "summary.csv"
$summaryRows | Export-Csv -Path $summaryPath -NoTypeInformation -Encoding UTF8

Compress-Archive -Path (Join-Path $outputDirectory "*") -DestinationPath $zipPath -Force

Write-Host ""
Write-Host "T-725 capture completed."
Write-Host "Folder: $outputDirectory"
Write-Host "ZIP to send for analysis: $zipPath"
Write-Host "The final phone screenshot remains at Download/RopeSkill_T725_capture.png."
