param(
    [Parameter(Mandatory = $true)]
    [string]$SigningProperties,

    [string]$BundletoolJar,

    [string]$EmulatorSerial,

    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repositoryRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repositoryRoot

if (-not $env:JAVA_HOME) {
    $androidStudioJbr = "C:\Program Files\Android\Android Studio\jbr"
    if (-not (Test-Path $androidStudioJbr)) {
        throw "JAVA_HOME is not set and the Android Studio JBR was not found."
    }
    $env:JAVA_HOME = $androidStudioJbr
}
$java = Join-Path $env:JAVA_HOME "bin\java.exe"

function Assert-LastExitCode {
    param([string]$FailureMessage)

    if ($LASTEXITCODE -ne 0) {
        throw $FailureMessage
    }
}

function Get-LatestVersionDirectory {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Root,

        [Parameter(Mandatory = $true)]
        [string]$Description
    )

    $directory = Get-ChildItem $Root -Directory |
        Sort-Object { [version]($_.Name -replace '-.*$', '') } -Descending |
        Select-Object -First 1
    if (-not $directory) {
        throw "$Description was not found under $Root."
    }
    return $directory
}

function Resolve-BundletoolJar {
    param([string]$RequestedPath)

    if ($RequestedPath) {
        return (Resolve-Path $RequestedPath).Path
    }

    $candidates = @(
        "C:\Program Files\Android\Android Studio\plugins\android\lib\bundletool.jar",
        "C:\Program Files\Android\Android Studio\plugins\android\lib\bundletool-all.jar"
    )

    $candidate = $candidates |
        Where-Object { Test-Path $_ } |
        Select-Object -First 1
    if ($candidate) {
        return (Resolve-Path $candidate).Path
    }

    $gradleBundletoolRoot =
        Join-Path $env:USERPROFILE ".gradle\caches\modules-2\files-2.1\com.android.tools.build\bundletool"
    if (Test-Path $gradleBundletoolRoot) {
        $candidate = Get-ChildItem $gradleBundletoolRoot -Recurse -File -Filter "bundletool-all*.jar" |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1
        if ($candidate) {
            return $candidate.FullName
        }
    }

    throw @"
bundletool was not found. Download the current bundletool-all JAR from:
https://github.com/google/bundletool/releases
Then rerun this script with -BundletoolJar "C:\path\to\bundletool-all.jar".
"@
}

$resolvedProperties = (Resolve-Path $SigningProperties).Path
if (-not $SkipBuild) {
    & "$PSScriptRoot\Build-T747ProductionRelease.ps1" -SigningProperties $resolvedProperties
    Assert-LastExitCode "Production packaging failed before the T-750 audit."
}

$apk = Get-Item ".\app\build\outputs\apk\release\app-release.apk"
$aab = Get-Item ".\app\build\outputs\bundle\release\app-release.aab"

$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$buildTools = Get-LatestVersionDirectory -Root "$sdk\build-tools" -Description "Android SDK Build Tools"
if ([version]($buildTools.Name -replace '-.*$', '') -lt [version]"35.0.0") {
    throw "Android SDK Build Tools 35.0.0 or newer are required."
}
$zipalign = Join-Path $buildTools.FullName "zipalign.exe"

Write-Host ""
Write-Host "1/4 APK ZIP alignment"
& $zipalign -v -c -P 16 4 $apk.FullName
Assert-LastExitCode "Release APK failed 16 KB ZIP alignment verification."

Write-Host ""
Write-Host "2/4 AAB page-alignment request"
$resolvedBundletool = Resolve-BundletoolJar -RequestedPath $BundletoolJar
$bundleConfig = @(& $java -jar $resolvedBundletool dump config --bundle=$($aab.FullName))
Assert-LastExitCode "bundletool could not inspect the Production AAB."
$alignmentLine = $bundleConfig | Select-String "PAGE_ALIGNMENT_(4K|16K)"
if (-not $alignmentLine) {
    throw "The AAB alignment setting was not found in bundletool output."
}
$alignmentLine
if (-not ($alignmentLine | Select-String "PAGE_ALIGNMENT_16K")) {
    throw "Production AAB does not request PAGE_ALIGNMENT_16K."
}

Write-Host ""
Write-Host "3/4 Native ELF alignment and RELRO"
$ndk = Get-LatestVersionDirectory -Root "$sdk\ndk" -Description "Android NDK"
$toolchain = Join-Path $ndk.FullName "toolchains\llvm\prebuilt\windows-x86_64\bin"
$llvmObjdump = Join-Path $toolchain "llvm-objdump.exe"
$llvmReadelf = Join-Path $toolchain "llvm-readelf.exe"
if (-not (Test-Path $llvmObjdump) -or -not (Test-Path $llvmReadelf)) {
    throw "llvm-objdump.exe or llvm-readelf.exe was not found in Android NDK $($ndk.Name)."
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$extractRoot = Join-Path ([IO.Path]::GetTempPath()) ("ropeskill-t750-" + [guid]::NewGuid())
[IO.Directory]::CreateDirectory($extractRoot) | Out-Null

try {
    $apkArchive = [IO.Compression.ZipFile]::OpenRead($apk.FullName)
    try {
        foreach ($entry in $apkArchive.Entries) {
            $normalizedEntryName = $entry.FullName.Replace("\", "/")
            if ($normalizedEntryName -notmatch '^lib/[^/]+/[^/]+\.so$') {
                continue
            }

            $destination = Join-Path $extractRoot ($normalizedEntryName.Replace("/", "\"))
            [IO.Directory]::CreateDirectory((Split-Path -Parent $destination)) | Out-Null

            # Some APK tools can expose duplicate ZIP entries. Inspect the final
            # payload for each native-library path instead of failing extraction.
            [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $destination, $true)
        }
    }
    finally {
        if ($apkArchive) {
            $apkArchive.Dispose()
        }
    }

    $nativeLibraries = @(Get-ChildItem (Join-Path $extractRoot "lib") -Recurse -File -Filter "*.so")
    if ($nativeLibraries.Count -eq 0) {
        throw "No native libraries were found in the Production APK."
    }

    $results = foreach ($library in $nativeLibraries) {
        $abi = $library.Directory.Name
        $programHeaders = @(& $llvmObjdump -p $library.FullName)
        Assert-LastExitCode "Unable to inspect ELF headers for $($library.Name)."
        $loadAlignments = @(
            $programHeaders |
                Select-String "LOAD.*align 2\*\*(\d+)" |
                ForEach-Object { [int]$_.Matches[0].Groups[1].Value }
        )
        if ($loadAlignments.Count -eq 0) {
            throw "No ELF LOAD alignment values were found for $($library.Name)."
        }

        $relroHeaders = @(& $llvmReadelf -lW $library.FullName)
        Assert-LastExitCode "Unable to inspect RELRO for $($library.Name)."
        $hasRelro = [bool]($relroHeaders | Select-String "GNU_RELRO")
        $minimumAlignment = ($loadAlignments | Measure-Object -Minimum).Minimum

        [pscustomobject]@{
            ABI = $abi
            Library = $library.Name
            MinimumLoadAlignment = "2**$minimumAlignment"
            SixteenKbAligned = [bool]($minimumAlignment -ge 14)
            RELRO = $hasRelro
        }
    }

    $results | Sort-Object ABI, Library | Format-Table -AutoSize

    $requiredResults = @($results | Where-Object { $_.ABI -in @("arm64-v8a", "x86_64") })
    if ($requiredResults.Count -eq 0) {
        throw "No arm64-v8a or x86_64 native libraries were found."
    }
    $unaligned = @($requiredResults | Where-Object { -not $_.SixteenKbAligned })
    if ($unaligned.Count -gt 0) {
        throw "One or more 64-bit native libraries failed ELF alignment verification."
    }
    $missingRelro = @($results | Where-Object { -not $_.RELRO })
    if ($missingRelro.Count -gt 0) {
        throw "One or more native libraries failed RELRO verification."
    }
}
finally {
    if (Test-Path $extractRoot) {
        Remove-Item $extractRoot -Recurse -Force
    }
}

Write-Host ""
Write-Host "4/4 16 KB runtime environment"
if (-not $EmulatorSerial) {
    Write-Host "Static artifact audit passed."
    Write-Host "Runtime audit remains pending. Rerun with -SkipBuild -EmulatorSerial <16_KB_EMULATOR_ID>."
    exit 0
}

$adb = Join-Path $sdk "platform-tools\adb.exe"
$pageSize = (& $adb -s $EmulatorSerial shell getconf PAGE_SIZE).Trim()
Assert-LastExitCode "Unable to query page size from $EmulatorSerial."
if ($pageSize -ne "16384") {
    throw "$EmulatorSerial reports PAGE_SIZE=$pageSize instead of 16384."
}

& $adb -s $EmulatorSerial install --no-incremental -r $apk.FullName
Assert-LastExitCode "Production APK installation failed on $EmulatorSerial."

$packageDetails = @(& $adb -s $EmulatorSerial shell dumpsys package com.ropeskill.app)
Assert-LastExitCode "Unable to inspect the installed RopeSkill package."
if (-not ($packageDetails | Select-String "versionCode=1") -or
    -not ($packageDetails | Select-String "versionName=0.1.0") -or
    ($packageDetails | Select-String "DEBUGGABLE")) {
    throw "Installed package identity is not the expected non-debuggable Production build."
}

& $adb -s $EmulatorSerial shell monkey -p com.ropeskill.app 1 | Out-Null
Assert-LastExitCode "Unable to launch RopeSkill on $EmulatorSerial."

Write-Host ""
Write-Host "Automated T-750 checks passed on PAGE_SIZE=16384."
Write-Host "Complete the manual smoke: Home -> Start Training -> Camera permission -> Pause -> Resume -> Finish -> Result -> History."
Write-Host "Confirm that there is no 16 KB compatibility warning, crash, freeze, or native-linker error."
